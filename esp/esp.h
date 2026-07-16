#ifndef ESP_WATERING
#define ESP_WATERING

#define WIFI_SSID "wifi-ssid"
#define WIFI_PASSWD "wifi-password"
#define WIFI_HOSTNAME "esp32-watering"


// #define USE_SENSOR 			// Uncomment if you want to use the sensor

#define NTP_SERVER "ntp-server" 	// for example: europe.pool.ntp.org
#define TIMEZONE_INFO "timezone" 	// for example: CET-1CEST,M3.5.0,M10.5.0/3

#define PUMP_PIN 25
#define SENSOR_PIN 16

#define SERVER_IP "server-ip"
#define SERVER_PORT 8282

#define SELECT_TIMEOUT 50000 		// 50 ms in microseconds 
#define RECONNECT_INTERVAL_MS 5000 	// in seconds

#define SENSOR_TIMEOUT 500 		// in milliseconds

#endif
