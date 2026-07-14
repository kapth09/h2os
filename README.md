# H2OS - Home Watering System

H2OS is a DIY smart watering system for your home, because buying premade solutions is lame.

It also supports water detection with push notifications via `ntfy`

> [!IMPORTANT]
> User interface is currently only in German!

## Getting started

Minimum requirements for the project:

- An ESP32 (or any other micro controller with network capabilities)
- A Server
- Hardware to control the water flow, for example an electric valve

### 1. Clone the repo

```bash
git clone ...
```

### 2. Configure

1. Update the header file for the ESP32 (`./esp/esp.h`)

```c
#define WIFI_SSID "wifi-ssid"
#define WIFI_PASSWD "wifi-password"
#define WIFI_HOSTNAME "esp32-watering"

// #define USE_SENSOR 			 	// Uncomment if you want to use the sensor

#define NTP_SERVER "ntp-server" 	// for example: europe.pool.ntp.org
#define TIMEZONE_INFO "timezone" 	// for example: CET-1CEST,M3.5.0,M10.5.0/3

#define PUMP_PIN 25					// change if necessary
#define SENSOR_PIN 16				// change if necessary

#define SERVER_IP "server-ip"
```

2. Update the `application.properties` for the Spring server (`src/main/resources/application.properties`
   If you don't use `ntfy`, you can leave it as is

```properties
ntfy.url="ntfy-pve"		
ntfy.topic="watering"
```

3. Finally update the server URL for the website (`src/main/resources/static/exports.js`)

```js
const SERVER_URL = "server-ip";
```

### 3. Flash & Run

#### Flash the ESP32

Flash the ESP via the Arduino-IDE

> You may have to install the ESP-Board library, and the `ESP32Time.h` library

#### Server, via Docker (recommended)

1. Build the Image, run in the project root directory

   ```bash
   docker build . --tag watering-image
   ```

2. Run the image, with your timezone, for example: `-e TZ=America/New_York`

   ```bash
   docker run -e TZ=<your timezone> -p 8080:8080 -p 8282:8282 --volume watering-logs:/app/logs --name watering-container watering-image
   ```

#### Server, otherwise

Make sure you have the `openjdk-25-jdk` installed

1. Create the logging directory

   ```bash
   mkdir logs
   ```

2. Run the Server program

   ```bash
   java -jar ./watering-x.x.x.jar
   ```

#### Done!

In your Browser go to your server at port `8080` (or whatever you have set), you should see the website.

<html>

<div style="display: flex; flex-direction: row; justify-content: space-around;">
    <img src="./docs/img/website-001.png" style="height: 36rem">
    <img src="./docs/img/website-002.png" style="height: 36rem">
</div>

<hr>

<div style="display: flex; flex-direction: row; justify-content: space-around;">
    <img src="./docs/img/esp-housing.jpg" style="height: 28rem">
    <img src="./docs/img/valve.jpg" style="height: 28rem">
</div>



</html>
