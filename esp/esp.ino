#include <sys/select.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <WiFi.h>
#include <ESP32Time.h>

#include "./esp.h"

enum msg_type : uint8_t {
  MSG_START         = 0,
  MSG_STARTED       = 1,
  MSG_STOP          = 2,
  MSG_STOPPED       = 3,
  MSG_LEAK_DETECTED = 4,
  MSG_LEAK_RESOLVED = 5,
};

struct task {
  uint8_t start_h;
  uint8_t start_m;
  uint8_t end_h;
  uint8_t end_m;
};

struct sensor {
  uint32_t time_since_leak;
  uint8_t state;
  uint8_t leaked;
};

fd_set read_fds;
unsigned long last_reconnect = 0;
int sock_fd = -1;
bool tcp_connected = false;
bool tcp_reconnect_tried = false;
enum msg_type tcp_message;
struct tm current_time = {0};
struct timeval tv_select;
struct sensor leak_sensor = {
  .state = 1,
  .leaked = false,
};

void setup() {
  Serial.begin(9600);
  pinMode(PUMP_PIN, OUTPUT);
  pinMode(SENSOR_PIN, INPUT);
  connect_wifi();
  sync_time();
  create_socket();
  connect_server();
}

void connect_wifi() {
  Serial.print("\nConnecting");
  WiFi.setHostname(WIFI_HOSTNAME);
  WiFi.begin(WIFI_SSID, WIFI_PASSWD);
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(2000);
  }
  IPAddress ip = WiFi.localIP();
  Serial.print("\nIP-Address: ");
  Serial.println(ip);
}

void sync_time() {
  configTzTime(TIMEZONE_INFO, NTP_SERVER);
  struct tm timeinfo;
  while (!getLocalTime(&timeinfo)) {
    Serial.print(".");
    delay(100);
  }
}

void create_socket() {
  sock_fd = socket(AF_INET, SOCK_STREAM, 0);
  int keep_alive = 1;
  int keep_idle = 5;
  int keep_intvl = 2;
  int keep_count = 3;
  setsockopt(sock_fd, SOL_SOCKET, SO_KEEPALIVE, &keep_alive, sizeof(keep_alive));
  setsockopt(sock_fd, IPPROTO_TCP, TCP_KEEPIDLE, &keep_idle, sizeof(keep_idle));
  setsockopt(sock_fd, IPPROTO_TCP, TCP_KEEPINTVL, &keep_intvl, sizeof(keep_intvl));
  setsockopt(sock_fd, IPPROTO_TCP, TCP_KEEPCNT, &keep_count, sizeof(keep_count));
}

void connect_server() {
  struct sockaddr_in serv_addr = {
    .sin_family = AF_INET,
    .sin_port = htons(SERVER_PORT)
  };
  inet_pton(AF_INET, SERVER_IP, &serv_addr.sin_addr);
  if (connect(sock_fd, (struct sockaddr*) &serv_addr, sizeof(serv_addr)) == -1) {
    Serial.println("Server connection failed");
    tcp_connected = false;
  } else {
    Serial.println("Server connection successfull");
    tcp_connected = true;
    if (leak_sensor.leaked) {
      write_server(MSG_LEAK_DETECTED);
    }
  }
}

void reconnect_server() {
  if (millis() - last_reconnect >= RECONNECT_INTERVAL_MS) {
    close(sock_fd);
    create_socket();
    Serial.println("Attempting reconnect");
    connect_server();
    last_reconnect = millis();
  }
}

int8_t write_server(msg_type type) {
  tcp_message = type;
  ssize_t bytes_written = write(sock_fd, &tcp_message, sizeof(tcp_message));
  if (bytes_written == -1) {
    Serial.println("Failed to write to server (-1)");
    return -1;
  }
  // Serial.printf("Bytes written: %zd\n", bytes_written);
  return (uint8_t) bytes_written;
}

void read_socket() {
  if (tcp_connected == false) return;
  FD_ZERO(&read_fds);
  FD_SET(sock_fd, &read_fds);
  tv_select.tv_sec  = 0;
  tv_select.tv_usec = SELECT_TIMEOUT;  
  int select_status = select(sock_fd+1, &read_fds, NULL, NULL, &tv_select);

  if (select_status > 0 && FD_ISSET(sock_fd, &read_fds)) {
    ssize_t read_bytes = read(sock_fd, &tcp_message, sizeof(tcp_message));
    Serial.printf("Read %zd bytes\n", read_bytes);
    if (read_bytes > 0) {
      Serial.printf("message: %d\n", tcp_message);
      handle_message(tcp_message);
    } else if (read_bytes == -1) {
      Serial.printf("Error %d: Connection lost\n", errno);
      tcp_connected = false;
    } else if (read_bytes == 0) {
      Serial.println("Connection closed");
      tcp_connected = false;
    } 
  }
}

void handle_message(msg_type msg) {
  switch (msg) {
    case MSG_START: {
      digitalWrite(PUMP_PIN, HIGH);
      write_server(MSG_STARTED);
      break;
    }
    case MSG_STOP: {
      digitalWrite(PUMP_PIN, LOW);
      write_server(MSG_STOPPED);
      break;
    }
    default: {
      Serial.printf("Unknonw message: %d\n", msg);
    }
  }
}

void handle_sensor() {
  leak_sensor.state = digitalRead(SENSOR_PIN);
  Serial.printf("Sensor: %d\n", leak_sensor.state);
  if (leak_sensor.state == 0) {
    leak_sensor.time_since_leak = millis();
  }
  if (millis() - leak_sensor.time_since_leak > SENSOR_TIMEOUT && leak_sensor.leaked == 1) {
    // leak resolved
    leak_sensor.leaked = 0;
    write_server(MSG_LEAK_RESOLVED);
  } else if (leak_sensor.state == 0 && leak_sensor.leaked == 0) {
    // leak detected 
    leak_sensor.leaked = 1;
    write_server(MSG_LEAK_DETECTED);
  }
}

void loop() {
  getLocalTime(&current_time);
#ifdef USE_SENSOR
  handle_sensor();
#endif
  read_socket();
  if (tcp_connected == false) {
    reconnect_server();
    delay(50);
  }
}
