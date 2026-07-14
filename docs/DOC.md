#  H2OS - Documentation

This is a brief overview of how this project works internally

## The ESP32

After connecting to a WiFi, it establishes a TCP connection to the server.

It continuously reads the socket via `select`.

## The Server

The server is split into 3 main components

### The ESP-Handler

Handles all communication with the ESP32

It sends one of the following messages to the ESP32:

```c++
enum msg_type : uint8_t {
  MSG_START         = 0,
  MSG_STARTED       = 1,
  MSG_STOP          = 2,
  MSG_STOPPED       = 3,
  MSG_LEAK_DETECTED = 4,
  MSG_LEAK_RESOLVED = 5,
};
```

```MSG_START``` and ```MSG_STOP``` are sent by the server and tell the ESP32 that it should perform the appropriate action.
As a response either ```MSG_STARTED``` or `MSG_STOPPED` is sent back.

### The WebSocket-Handler

Handles all communication with the browser clients via a WebSocket

Messages are sent as stringified `JSON-Objects`, a standard message is built like this

```json
{
    "type": <type>,
    "option": <option>,
    "data": <data>
}
```

#### Message Types

There are 3 possible message types:

```java
enum WebMsgType {
    REQUEST (0),
    COMMAND (1),
    EVENT   (2);
}
```

#### Option - Request 

When the client request information, like the next task

```java
enum MsgRequest {
    GET_TASKS           (0),
    GET_ACTIVE_TASK     (1),
    GET_TODAYS_TASKS    (2);
}
```

#### Option - Command 

When the client performs an action, like updating the tasks

```java
enum MsgCommand {
    SET_TASKS           (0),
    SET_TODAYS_TASKS    (1),
    START_NOW           (2),
    STOP_NOW            (3),
    PAUSE_TASK          (4),
    RESUME_TASK         (5),
    RELOAD_TODAYS_TASKS (6);
}
```

#### Option - Event

When an event occurs, like the ESP32 started watering 

```java
 enum MsgEvent {
    STARTED             (0),
    STOPPED             (1),
    ESP_CONNECTED       (2),
    ESP_DISCONNECTED    (3),
    LEAK_DETECTED       (4),
    LEAK_RESOLVED       (5);
 }
```

#### Message Data

The whole message is sent as a `Java Record` with Jackson's `ObjectMapper`

### The Task-Handler

Manages the task schedule and parses information between the ESP-Handler and the WebSocket-Handler  

It also checks every second if the next task should start now.