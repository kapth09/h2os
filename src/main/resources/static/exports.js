export { SERVER_URL, WEBSOCKET_PORT, MsgType, MsgCommand, MsgEvent, MsgRequest, TaskStatus, TaskType, Weekdays, TaskDialogType, TaskDialogOption };

const SERVER_URL = "192.168.0.199";
const WEBSOCKET_PORT = 8080;

const Weekdays = new Map([
    [0, "Montag"],
    [1, "Dienstag"],
    [2, "Mittwoch"],
    [3, "Donnerstag"],
    [4, "Freitag"],
    [5, "Samstag"],
    [6, "Sonntag"],
])

const MsgType = {
    REQUEST     : 0,
    COMMAND     : 1,
    EVENT       : 2,
};

const MsgCommand = {
    SET_TASKS           : 0,
    SET_TODAYS_TASKS    : 1,
    START_NOW           : 2,
    STOP_NOW            : 3,
    PAUSE_TASK          : 4,
    RESUME_TASK         : 5,
    RELOAD_TODAYS_TASKS : 6,
}

const MsgEvent = {
    STARTED             : 0,
    STOPPED             : 1,
    ESP_CONNECTED       : 2,
    ESP_DISCONNECTED    : 3, 
    LEAK_DETECTED       : 4,
    LEAK_RESOLVED       : 5,
}

const MsgRequest = {
    GET_TASKS           : 0,
    GET_ACTIVE_TASK     : 1,
    GET_TODAYS_TASKS    : 2,
}

const TaskStatus = {
    READY       : 0,
    DEACTIVATED : 1,
    PAUSED      : 2,
    RUNNING     : 3,
    FINISHED    : 4,
}

const TaskType = {
    STANDARD    : 0,
    TEMPORARY   : 1,
    INSERTED    : 2,
}

const TaskDialogType = {
    ADD     : 0,
    EDIT    : 1,
}

const TaskDialogOption = {
    STANDARD    : 0,
    TEMPORARY   : 1,
}
