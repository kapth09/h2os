#ifndef ESP_WATERING
#define ESP_WATERING

#define WIFI_SSID "A1-Mesh-dVC9D4"
#define WIFI_PASSWD "93MdvXtcqh4N"
#define WIFI_HOSTNAME "h2os-watering"


// #define USE_SENSOR 			// Uncomment if you want to use the sensor

#define NTP_SERVER "europe.pool.ntp.org" 	// for example: europe.pool.ntp.org
#define TIMEZONE_INFO "CET-1CEST,M3.5.0,M10.5.0/3" 	// for example: CET-1CEST,M3.5.0,M10.5.0/3

#define PUMP_PIN 25
#define SENSOR_PIN 16

#define SERVER_IP "192.168.0.199"
#define SERVER_PORT 8282

#define SELECT_TIMEOUT 50000 		// 50 ms in microseconds 
#define RECONNECT_INTERVAL_MS 5000 	// in seconds

#define SENSOR_TIMEOUT 500 		// in milliseconds

#endif
