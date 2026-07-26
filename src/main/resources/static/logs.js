const LOG_ENDPOINT_BASE = "/api/logs";
const LOG_ENDPOINT_CURRENT = `${LOG_ENDPOINT_BASE}/current`;
const LOG_ENDPOINT_LIST = `${LOG_ENDPOINT_BASE}/list`;
const LOG_ENDPOINT_FILE = `${LOG_ENDPOINT_BASE}/file/`

const ANSI_COLOR_RED      = "\x1b[31m";
const ANSI_COLOR_YELLOW   = "\x1b[33m";
const ANSI_COLOR_BLUE     = "\x1b[34m";
const ANSI_COLOR_RESET    = "\x1b[0m";

const LOG_TYPE = {
    INFO: 0,
    WARNING: 1,
    ERROR: 2,
};

class Log {
    constructor(timeStamp, message, type) {
        this.timeStamp = timeStamp;
        this.message = message;
        this.type = type;
    }

    toString() {
        return this.timeStamp + this.type + this.message;
    }
}

let logs = [];

const HTML_TEMPLATE_LOG_INFO = document.getElementById("template-info");
const HTML_TEMPLATE_LOG_WARNING = document.getElementById("template-warning");
const HTML_TEMPLATE_LOG_ERROR = document.getElementById("template-error");
const HTML_TABLE_BODY = document.getElementById("log-table-body");

const CSS_LOG_TIME = "log-time";
const CSS_LOG_TEXT = "log-text";

async function hitAPI(logUrl) {
  try {
    const response = await fetch(logUrl);
    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }
    const data = await response.text();
    return data;
  } catch (error) {
    console.error('Fetch error:', error);
  }
}

function processLog(logText, logsArray) {
    const logLines = logText.split("\n");
    for (let i = 0; i < logLines.length; i++) {
        let line = logLines[i];
        if (line === "") continue;
        line = line.split(ANSI_COLOR_RESET);

        let lineSplit = line[0].split(ANSI_COLOR_BLUE);
        if (lineSplit.length > 1) {
            addNewLog(logs, lineSplit[0], line[1], LOG_TYPE.INFO);
            continue;
        }
        lineSplit = line[0].split(ANSI_COLOR_RED);
        if (lineSplit.length > 1) {
            addNewLog(logs, lineSplit[0], line[1], LOG_TYPE.ERROR);
            continue;
        }
        lineSplit = line[0].split(ANSI_COLOR_YELLOW);
        if (lineSplit.length > 1) {
            addNewLog(logs, lineSplit[0], line[1], LOG_TYPE.WARNING);
            continue;
        }
        console.error(`Unknown Log type: ${line[0]}`)
    }
    console.log(logs);
}

function addNewLog(logArray, timeStamp, message, type) {
    timeStamp = timeStamp.slice(0, -2);
    message = message.slice(3);
    const log = new Log(timeStamp, message, type);
    logArray.push(log);
}

function createLogHtml(log) {
    const template = (function (type) {
        switch (type) {
            case LOG_TYPE.INFO: return HTML_TEMPLATE_LOG_INFO;
            case LOG_TYPE.WARNING: return HTML_TEMPLATE_LOG_WARNING;
            case LOG_TYPE.ERROR: return HTML_TEMPLATE_LOG_ERROR;
            default: return HTML_TEMPLATE_LOG_ERROR;
        }
    }) (log.type);
    const logClone = template.content.cloneNode(true);
    const logElement = logClone.firstElementChild;
    logElement.querySelector(`.${CSS_LOG_TIME}`).textContent = log.timeStamp;
    logElement.querySelector(`.${CSS_LOG_TEXT}`).textContent = log.message;
    return logElement;
}

function displayLogs(logs) {
    HTML_TABLE_BODY.innerHTML = "";
    for (let i = 0; i < logs.length; i++) {
        const log = createLogHtml(logs[i]);
        HTML_TABLE_BODY.appendChild(log);
    }
}

hitAPI(LOG_ENDPOINT_CURRENT).then(logText => {
    processLog(logText, logs);
    displayLogs(logs);
})