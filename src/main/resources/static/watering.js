import { MsgType, MsgCommand, MsgEvent, MsgRequest, TaskStatus, TaskType, Weekdays, TaskDialogType, TaskDialogOption, SERVER_URL, WEBSOCKET_PORT } from "./exports.js";

window.sendScheduleChanges = sendScheduleChanges;
window.startMin_Decrement = startMin_Decrement;
window.startMin_Increment = startMin_Increment; 
window.BTN_showAddTaskDialog = BTN_showAddTaskDialog; 
window.BTN_popupHideBG = BTN_popupHideBG;
window.BTN_popupFinish = BTN_popupFinish;
window.readFilter = readFilter;
window.startMin_Start = startMin_Start;
window.btnStop = btnStop;
window.btnPause = btnPause;
window.btnResume = btnResume;
window.reloadTodaysTasks = reloadTodaysTasks;
window.TaskDialogOption = TaskDialogOption;
window.TaskDialogType = TaskDialogType;

class Task {
    constructor(startHour, startMinute, endHour, endMinute, day, status, type) {
        this.startHour = parseInt(startHour);
        this.startMinute = parseInt(startMinute);
        this.endHour = parseInt(endHour);
        this.endMinute = parseInt(endMinute);
        this.day = parseInt(day);
        this.status = parseInt(status);
        this.type = parseInt(type);
    }

    getDay() {
       return this.day - 1;
    }

    getStartTimeStamp() {
        return ((this.startHour << 8) | this.startMinute);
    }
    getEndTimeStamp() {
        return ((this.endHour << 8) | this.endMinute);
    }

    compareTo(otherTask) {
        if (this.day > otherTask.day) return  1; 	// this Task is later than the other one
        if (this.day < otherTask.day) return -1; 	// this task is earlier than the other one

        const thisTime = this.getEndTimeStamp();
        const otherTime = otherTask.getEndTimeStamp();

        if (thisTime > otherTime) return  1; 	// this Task is later than the other one
        if (thisTime < otherTime) return -1; 	// this task is earlier than the other one

        return 0;
    }

    equals(otherTask) {
	if (this.day !== otherTask.day) return false;
	if (this.getStartTimeStamp() !== otherTask.getStartTimeStamp()) return false;
	if (this.getEndTimeStamp() !== otherTask.getEndTimeStamp()) return false;
	return true;
    }

    overlaps(otherTask) {
        if (this.day !== otherTask.day || this.type === TaskType.INSERTED || otherTask.type === TaskType.INSERTED) return false;
        return !(this.getEndTimeStamp() < otherTask.getStartTimeStamp() || this.getStartTimeStamp() > otherTask.getEndTimeStamp());
    }
}

class Message {
    constructor(type, option, data) {
        this.type = type;
        this.option = option;
        this.data = data;
    }
}

class Popup {
    constructor(type, option, taskIndex) {
        this.type = type;
        this.option = option;
        this.taskIndex = taskIndex;
    }
}

const ACTIVE_DAY_EMPTY = "Leer";
const TIME_EMPTY = "--:--";

// HTML-Elements
// HTML-Status
const HTML_statusSave               = document.getElementById("status-save");
const HTML_statusSvgServer          = document.getElementById("status-svg-server");
const HTML_statusSvgMicrochip       = document.getElementById("status-svg-microchip");
// HTML-Info
const HTML_infoActiveDay            = document.getElementById("info-active-day");
const HTML_infoStartTime            = document.getElementById("info-start-time");
const HTML_infoEndTime              = document.getElementById("info-end-time");
const HTML_infoTimeSpan             = document.getElementById("info-time-span");
const HTML_infoActiveEndTimeDiv     = document.getElementById("info-active-end-time-div");
const HTML_infoActiveEndTime        = document.getElementById("info-active-end-time");
const HTML_infoBtnStop              = document.getElementById("info-btn-stop");
const HTML_infoAnimateStop          = document.getElementById("info-animate-stop");
const HTML_infoBtnPause             = document.getElementById("info-btn-pause");
const HTML_infoAnimatePause         = document.getElementById("info-animate-pause");
const HTML_infoBtnResume            = document.getElementById("info-btn-resume");
const HTML_infoAnimateResume        = document.getElementById("info-animate-resume");
const HTML_infoInactive             = document.getElementById("info-inactive");
// HTML-Start-Now
const HTML_startMinValue            = document.getElementById("start-min-value");
const HTML_startBtn                 = document.getElementById("start-btn");
// HTML-Temp-Task
const HTML_tempTasksEmpty           = document.getElementById("temp-tasks-empty");
const HTML_tempTasksContainer       = document.getElementById("temp-tasks-container");
// HTML-Filter
const HTML_filterCheckboxes         = document.getElementsByClassName("filter-checkbox");
// HTML-Week
const HTML_weekScheduleContainer    = document.getElementById("week-schedule-container");
const HTML_weekDays                 = document.getElementsByClassName("week-day");
const HTML_weekDaysContainer        = document.getElementsByClassName("week-day-container");
// HTML-PopUp
const HTML_popupBg                  = document.getElementById("popup-bg");
const HTML_popupWeekdays            = document.getElementById("popup-weekdays");
const HTML_popupTimeStart           = document.getElementById("popup-time-start");
const HTML_popupTimeEnd             = document.getElementById("popup-time-end");
const HTML_popupWeekdayCheckbox     = document.getElementsByName("popup-day-select");
const HTML_popupHeader 		    = document.getElementById("popup-header");
let   HTML_popWeekdayChecked        = document.querySelectorAll('input[name="popup-day-select"]:checked');
// HTML-Templates
const HTML_templateTask             = document.getElementById("template-task");

// CSS-Classes
const CSS_StatusSuccess     = "status-success-svg";
const CSS_StatusFailure     = "status-failure-svg";
const CSS_taskToggle        = "task-toggle";
const CSS_taskTimeStart     = "task-time-start";
const CSS_taskTimeEnd       = "task-time-end";
const CSS_filterCheckbox    = "filter-checkbox";
const CSS_taskEdit          = "task-edit";
const CSS_taskDelete        = "task-delete";

// Animations
let taskProgressPercent = 1;
let taskProgressDuration = 1000;
let ANIMATED_infoStop;
let ANIMATED_infoPause;
let ANIMATED_infoResume;
const getANIMATION_taskProgress = () => ({
    keyframes: [
        {width: `${100}%`},
        {width: 0},
    ],
    options: {
        duration: taskProgressDuration,
        fill: "forwards",
        easing: "linear",
    },
    progress: taskProgressPercent,
})

function getAnimationProgress(animationElement) {
    const progress = animationElement.effect.getComputedTiming().progress;
    if (progress !== null) {
        return (progress * 100).toFixed(0);
    }
    return -1;
}

// Dynamic variables
let tasks = [];
let todaysTasks = [];
let activeTask;
let isNextWeek;
let activeTaskEndSecond = -1;
let filteredDays = new Map([
    [-1, false],
    [0, false],
    [1, false],
    [2, false],
    [3, false],
    [4, false],
    [5, false],
    [6, false],
])
let taskPopup = new Popup();
let weeklyScheduleChanged = false;
let todaysScheduleChanged = true

// Connection Status
function statusSVG_set(statusHTML, isConnected) {
    if (isConnected) {
        if (statusHTML.classList.contains(CSS_StatusFailure)) {
            statusHTML.classList.remove(CSS_StatusFailure);
        }
        statusHTML.classList.add(CSS_StatusSuccess);
    } else {
        if (statusHTML.classList.contains(CSS_StatusSuccess)) {
            statusHTML.classList.remove(CSS_StatusSuccess);
        }
        statusHTML.classList.add(CSS_StatusFailure);
    }
}
// Status
function controllSaveBtn() {
    if (weeklyScheduleChanged || todaysScheduleChanged) {
        HTML_statusSave.classList.remove("hidden");
    } else {
        HTML_statusSave.classList.add("hidden");
    }
}
function sendScheduleChanges() {
    if (weeklyScheduleChanged) {
        sendMessage(MsgType.COMMAND,  MsgCommand.SET_TASKS, tasks);
        weeklyScheduleChanged = false;
    }
    if (todaysScheduleChanged) {
        sendMessage(MsgType.COMMAND, MsgCommand.SET_TODAYS_TASKS, todaysTasks);
        todaysScheduleChanged = false;
    }
    HTML_statusSave.classList.add("hidden");
}

// Info
function controllActiveTaskHTML() {
    let dayTxt = ACTIVE_DAY_EMPTY;
    let nextWeekTxt = "";
    let timeStartTxt = TIME_EMPTY;
    let timeEndTxt = TIME_EMPTY;
    HTML_infoActiveDay.classList.add("text-zinc-500");
    if (activeTask) {
        if (activeTask.status === TaskStatus.RUNNING || activeTask.status === TaskStatus.PAUSED) {
            dayTxt = "";
            nextWeekTxt = "Jetzt";
            const endTime= `${padDigits(activeTask.endHour, 2)}:${padDigits(activeTask.endMinute, 2)}:${padDigits(activeTaskEndSecond, 2)}`;
            HTML_infoActiveEndTime.textContent = endTime;
            HTML_infoActiveDay.classList.remove("text-zinc-500");
            HTML_infoTimeSpan.classList.add("hidden");
            HTML_infoActiveEndTimeDiv.classList.remove("hidden");
        } else {
            const day = Object.assign(new Task, activeTask).getDay();
            dayTxt = Weekdays.get(day);
            timeStartTxt = `${padDigits(activeTask.startHour, 2)}:${padDigits(activeTask.startMinute, 2)}`;
            timeEndTxt = `${padDigits(activeTask.endHour, 2)}:${padDigits(activeTask.endMinute, 2)}`;
            HTML_infoActiveDay.classList.remove("text-zinc-500");
            HTML_infoTimeSpan.classList.remove("hidden");
            HTML_infoActiveEndTimeDiv.classList.add("hidden");
            nextWeekTxt = isNextWeek ? "Nächsten" : "Diesen";
        }
    } else {
        HTML_infoActiveEndTimeDiv.classList.add("hidden");
        HTML_infoTimeSpan.classList.add("hidden");
        HTML_infoActiveDay.classList.add("text-zinc-500");
    }
    HTML_infoActiveDay.textContent = `${nextWeekTxt} ${dayTxt}`;
    HTML_infoStartTime.textContent = timeStartTxt;
    HTML_infoEndTime.textContent = timeEndTxt;
}
function calcAnimationData() {
    if (activeTask === null || activeTaskEndSecond === -1) return;
    taskProgressDuration = calcTaskDuration(activeTask) * 1000;
    const remainingDuration = calcRemainingTaskDuration(activeTask) * 1000;
    taskProgressPercent = remainingDuration / taskProgressDuration;
}
function setActiveTaskAnimations(animation) {
    ANIMATED_infoStop = HTML_infoAnimateStop.animate(animation.keyframes, animation.options);
    ANIMATED_infoPause = HTML_infoAnimatePause.animate(animation.keyframes, animation.options);
    const elapsedTime = taskProgressDuration - (taskProgressDuration * taskProgressPercent);
    ANIMATED_infoStop.currentTime = elapsedTime;
    ANIMATED_infoPause.currentTime = elapsedTime;
}
function controllActiveTaskControlls() {
    const animate = getANIMATION_taskProgress();
    if (activeTask === null || activeTask.status === TaskStatus.READY || activeTask.status === TaskStatus.FINISHED) {
        HTML_startBtn.disabled = false;
        HTML_infoBtnStop.classList.add("hidden");
        HTML_infoBtnResume.classList.add("hidden");
        HTML_infoBtnPause.classList.add("hidden");
        HTML_infoInactive.classList.remove("hidden");
        taskProgressPercent = 100;
        taskProgressDuration = 1000;
    } else if (activeTask.status === TaskStatus.RUNNING) {
        HTML_startBtn.disabled = true;
        HTML_infoBtnStop.classList.remove("hidden");
        HTML_infoBtnResume.classList.add("hidden");
        HTML_infoBtnPause.classList.remove("hidden");
        HTML_infoInactive.classList.add("hidden");
        setActiveTaskAnimations(animate);
    } else if (activeTask.status === TaskStatus.PAUSED) {
        HTML_startBtn.disabled = true;
        if (!ANIMATED_infoStop || !ANIMATED_infoPause) {
            setActiveTaskAnimations(animate);
        }
        ANIMATED_infoStop.pause();
        ANIMATED_infoPause.pause();
        taskProgressPercent = getAnimationProgress(ANIMATED_infoStop);
        HTML_infoAnimateResume.style.width = getComputedStyle(HTML_infoAnimateStop).width;
        HTML_infoBtnStop.classList.remove("hidden");
        HTML_infoBtnResume.classList.remove("hidden");
        HTML_infoBtnPause.classList.add("hidden");
        HTML_infoInactive.classList.add("hidden");
    }
}
function btnStop() {
    sendMessage(MsgType.COMMAND, MsgCommand.STOP_NOW, null);
}
function btnPause() {
    sendMessage(MsgType.COMMAND, MsgCommand.PAUSE_TASK, null);
}
function btnResume() {
    sendMessage(MsgType.COMMAND, MsgCommand.RESUME_TASK, null);
}

// Start Now
function startMin_Increment() {
    let value = parseInt(HTML_startMinValue.value);
    if (isNaN(value)) {
        value = 0;
    }
    value += 1;
    HTML_startMinValue.value = value;
}
function startMin_Decrement() {
    let value = parseInt(HTML_startMinValue.value);
    if (isNaN(value)) {
        value = 0;
    } else if (value > 0) {
        value -= 1;
    }
    HTML_startMinValue.value = value;
}
function startMin_Start() {
    const value = parseInt(HTML_startMinValue.value);
    if (isNaN(value) || value === 0) {
        return;
    }
    sendMessage(MsgType.COMMAND, MsgCommand.START_NOW, {"duration": value});
}

// Filter
function readFilter() {
    for (let element of HTML_filterCheckboxes) {
        filteredDays.set(parseInt(element.value), element.checked);    
    }
    applyDayFilter();
}

// Tasks
function createTask(task, dialogOption, taskIndex) {
    const taskClone = HTML_templateTask.content.cloneNode(true);
    const taskElement = taskClone.firstElementChild;
    const startHour = padDigits(task.startHour, 2);
    const startMinute = padDigits(task.startMinute, 2);
    const endHour = padDigits(task.endHour, 2);
    const endMinute = padDigits(task.endMinute, 2);
    taskElement.querySelector(`.${CSS_taskTimeStart}`).textContent = `${startHour}:${startMinute}`;
    taskElement.querySelector(`.${CSS_taskTimeEnd}`).textContent = `${endHour}:${endMinute}`;
    const edit = taskElement.querySelector(`.${CSS_taskEdit}`)
        edit.addEventListener("click", () => {
        BTN_showAddTaskDialog(TaskDialogType.EDIT, dialogOption, taskIndex)
    });
    const toggle = taskElement.querySelector(`.${CSS_taskToggle}`);
    toggle.checked = !(task.status === TaskStatus.DEACTIVATED)
    toggle.addEventListener("click", () => {
        toggleTask(dialogOption, taskIndex);
    });
    const deleteBtn = taskElement.querySelector(`.${CSS_taskDelete}`);
    deleteBtn.addEventListener("click", () => {
	deleteTask(dialogOption, taskIndex);
    });
    return taskElement;
}
function generateTodaysTaskHTML() {
    HTML_tempTasksContainer.innerHTML = "";
    if (todaysTasks.length === 0) {
        HTML_tempTasksEmpty.classList.remove("hidden");
        HTML_tempTasksContainer.classList.add("hidden");
    } else {
        HTML_tempTasksEmpty.classList.add("hidden");
        HTML_tempTasksContainer.classList.remove("hidden");
        for (let i = 0; i < todaysTasks.length; i++) {
            const task = todaysTasks[i];
            if (task.type !== TaskType.TEMPORARY) continue;
            HTML_tempTasksContainer.appendChild(createTask(task, TaskDialogOption.TEMPORARY, i));
        }
    }
}
function generateTasksHTML() {
    for (let i = 0; i < HTML_weekDays.length; i++) {
        const html_day = HTML_weekDays[i];
        html_day.innerHTML = "";
    }
    for (let i = 0; i < tasks.length; i++) {
        const task = tasks[i];
        if (task.type !== TaskType.STANDARD) continue;
        const html_day = HTML_weekDays[task.getDay()];
        html_day.appendChild(createTask(task, TaskDialogOption.STANDARD, i));
        filteredDays.set(task.getDay(), true);
    }
    applyDayFilter();
}
function toggleTask(type, taskIndex) {
    let taskArray = type === TaskDialogOption.STANDARD ? tasks : todaysTasks;
    if (taskArray[taskIndex].status === TaskStatus.DEACTIVATED) {
        taskArray[taskIndex].status = TaskStatus.READY;
    } else {
        taskArray[taskIndex].status = TaskStatus.DEACTIVATED;
    }
    if (taskArray[taskIndex].type === TaskType.STANDARD) {
        weeklyScheduleChanged = true;
    } else if (taskArray[taskIndex].type === TaskType.TEMPORARY) {
        todaysScheduleChanged = true;
    }
    controllSaveBtn();
}
function deleteTask(option, taskIndex) {
    let taskArray = option === TaskDialogOption.STANDARD ? tasks : todaysTasks;
    if (taskIndex < 0 || taskIndex >= taskArray.length) return;
    taskArray.splice(taskIndex, 1);
    if (option === TaskDialogOption.STANDARD) {
	weeklyScheduleChanged = true;
	generateTasksHTML();
    } else if (option === TaskDialogOption.TEMPORARY) {
	todaysScheduleChanged = true;
	generateTodaysTaskHTML();
    }
    controllSaveBtn();
}
function applyDayFilter() {
    const showAll = filteredDays.get(-1);
    const showAvailable = document.querySelector(`.${CSS_filterCheckbox}:checked`) === null;
    if (showAvailable) {
        for (let i = 0; i < HTML_weekDays.length; i++) {
            HTML_weekDaysContainer[i].classList.add("hidden");
        }
        for (let i = 0; i < tasks.length; i++) {
            const task = Object.assign(new Task(), tasks[i]);
            const html_day = HTML_weekDaysContainer[task.getDay()];
            if (task.type !== TaskType.INSERTED) {
                html_day.classList.remove("hidden");
            }
        }
    } else {
        for (let i = 0; i < HTML_weekDays.length; i++) {
            const html_day = HTML_weekDaysContainer[i];
            const visible = filteredDays.get(i);
            if (visible || showAll) {
                html_day.classList.remove("hidden");
            } else {
                html_day.classList.add("hidden");
            }
        }
    }
}
function timeToSeconds(hours, minute, second) {
    return hours * 3600 + minute * 60 + second;
}
function calcTaskDuration(task) {
    const t1 = timeToSeconds(task.startHour, task.startMinute, activeTaskEndSecond);
    const t2 = timeToSeconds(task.endHour, task.endMinute, activeTaskEndSecond);
    return Math.abs(t2 - t1);
}
function calcRemainingTaskDuration(task) {
    const d = new Date();
    const t1 = timeToSeconds(d.getHours(), d.getMinutes(), d.getSeconds());
    const t2 = timeToSeconds(task.endHour, task.endMinute, activeTaskEndSecond);
    return Math.abs(t2 - t1);
}

function reloadTodaysTasks() {
    sendMessage(MsgType.COMMAND, MsgCommand.RELOAD_TODAYS_TASKS, null);
}

// PopUp
function popupShowBG() {
    if (HTML_popupBg.classList.contains("hidden")) {
        HTML_popupBg.classList.remove("hidden");
    }
}
function BTN_popupHideBG() {
    if (!HTML_popupBg.classList.contains("hidden")) {
        HTML_popupBg.classList.add("hidden");
    }
}

function BTN_showAddTaskDialog(dialogType, dialogOption, taskIndex) {
    for (let checkbox of HTML_popupWeekdayCheckbox) {
        checkbox.checked = false;
    }
    if (dialogType === TaskDialogType.EDIT) {
	HTML_popupHeader.textContent = "Plan bearbeiten";
    } else if (dialogType === TaskDialogType.ADD) {
	HTML_popupHeader.textContent = "Neuen Plan hinzufügen";
    }
    if (dialogOption === TaskDialogOption.STANDARD) {
        HTML_popupWeekdays.classList.remove("hidden");
    } else if (dialogOption === TaskDialogOption.TEMPORARY) {
        HTML_popupWeekdays.classList.add("hidden");
    }
    if (taskIndex > -1 && dialogType === TaskDialogType.EDIT) {
        let task = null;
	if (dialogOption === TaskDialogOption.STANDARD) {
	    task = tasks[taskIndex];
	} else if (dialogOption === TaskDialogOption.TEMPORARY) {
	    task = todaysTasks[taskIndex];
	}
        HTML_popupTimeStart.value = `${padDigits(task.startHour, 2)}:${padDigits(task.startMinute, 2)}`
        HTML_popupTimeEnd.value = `${padDigits(task.endHour, 2)}:${padDigits(task.endMinute, 2)}`
        HTML_popupWeekdays.classList.add("hidden");
    }
    taskPopup = new Popup(dialogType, dialogOption, taskIndex);
    popupShowBG();
}

function taskIsOverlaping(taskArray, task, taskIndex) {
    for (let i = 0; i < taskArray.length; i++) {
        if (taskIndex === i) continue;
        if (!task.equals(taskArray[i]) && task.overlaps(taskArray[i])) {
            return true;
        }
    }
    return false;
}

function popupAddStandardTask(timeStart, timeEnd) {
    HTML_popWeekdayChecked = document.querySelectorAll('input[name="popup-day-select"]:checked');
    if (HTML_popWeekdayChecked.length === 0) {
        alert("Wähle mindestens einen Tag aus");
        return;
    }
    const type = TaskType.STANDARD;
    const status = TaskStatus.READY;
    let tempArray = [];
    for (let i = 0; i < HTML_popWeekdayChecked.length; i++) {
        const day = HTML_popWeekdayChecked[i].value;
        const task = new Task(timeStart[0], timeStart[1], timeEnd[0], timeEnd[1], day, status, type);
        if (taskIsOverlaping(tasks, task, taskPopup.taskIndex)) {
            alert("Ungültiger Plan");
            return;
        }
        tempArray.push(task);
    }
    tasks = tasks.concat(tempArray);
    tasks.sort((a,b) => a.compareTo(b));
    weeklyScheduleChanged = true;
}

function popupEditStandardTask(timeStart, timeEnd) {
    const type = TaskType.STANDARD;
    const status = TaskStatus.READY;
    const day = tasks[taskPopup.taskIndex].day;
    const task = new Task(timeStart[0], timeStart[1], timeEnd[0], timeEnd[1], day, status, type);
    if (taskIsOverlaping(tasks, task, taskPopup.taskIndex)) {
        alert("Ungültiger Plan");
        return;
    }
    tasks[taskPopup.taskIndex] = task;
    tasks.sort((a,b) => a.compareTo(b));
    weeklyScheduleChanged = true;
}

function popupAddTemporaryTask(timeStart, timeEnd) {
    const jsDay = new Date().getDay();
    const day = jsDay === 0 ? 7 : jsDay;
    const status = TaskStatus.READY;
    const type = TaskType.TEMPORARY;
    const task = new Task(timeStart[0], timeStart[1], timeEnd[0], timeEnd[1], day, status, type);
    if (taskIsOverlaping(todaysTasks, task, taskPopup.taskIndex)) {
        alert("Ungültiger Plan");
        return;
    }
    todaysTasks.push(task);
    todaysTasks.sort((a,b) => a.compareTo(b));
    todaysScheduleChanged = true;
}

function popupEditTemporaryTask(timeStart, timeEnd) {
    const type = TaskType.TEMPORARY;
    const status = TaskStatus.READY;
    const day = todaysTasks[taskPopup.taskIndex].day;
    const task = new Task(timeStart[0], timeStart[1], timeEnd[0], timeEnd[1], day, status, type);
    if (taskIsOverlaping(todaysTasks, task, taskPopup.taskIndex)) {
        alert("Ungültiger Plan");
        return;
    }
    todaysTasks[taskPopup.taskIndex] = task;
    todaysTasks.sort((a,b) => a.compareTo(b));
    todaysScheduleChanged = true;
}

function BTN_popupFinish() {
    const timeStart = HTML_popupTimeStart.value.split(":");
    const timeEnd = HTML_popupTimeEnd.value.split(":");
    if (timeEnd <= timeStart) {
        alert("Endzeit muss nach Beginnzeit sein");
        return;
    }
    if  (taskPopup.option === TaskDialogOption.STANDARD) {
	if (taskPopup.type === TaskDialogType.ADD) {
	    popupAddStandardTask(timeStart, timeEnd);
	} else if (taskPopup.type === TaskDialogType.EDIT) {
	    popupEditStandardTask(timeStart, timeEnd);
	}
	controllSaveBtn();
	generateTasksHTML();
    } else if (taskPopup.option === TaskDialogOption.TEMPORARY) {
	if (taskPopup.type === TaskDialogType.ADD) {
	    popupAddTemporaryTask(timeStart, timeEnd);
	} else if (taskPopup.type === TaskDialogType.EDIT) {
	    popupEditTemporaryTask(timeStart, timeEnd);
	}
        controllSaveBtn();
        generateTodaysTaskHTML();
    }
    BTN_popupHideBG();
}

function padDigits(num, digits) {
    return String(num).padStart(digits, "0");
}

// --- WebSocket -- \\
const WEBSOCKET_URL = `ws://${SERVER_URL}:${WEBSOCKET_PORT}/ws`;

let websocket;

function connectWebSocket() {
    websocket = new WebSocket(WEBSOCKET_URL);

    websocket.onopen = () => {
        statusSVG_set(HTML_statusSvgServer, true);
        sendMessage(MsgType.REQUEST, MsgRequest.GET_TASKS, "");
        sendMessage(MsgType.REQUEST, MsgRequest.GET_ACTIVE_TASK, "");
        sendMessage(MsgType.REQUEST, MsgRequest.GET_TODAYS_TASKS, "");
    };

    websocket.onerror = () => {
        statusSVG_set(HTML_statusSvgServer, false);
        statusSVG_set(HTML_statusSvgMicrochip, false);
        console.error("WebSocket connection failure");
    };

    websocket.onclose = () => {
        statusSVG_set(HTML_statusSvgServer, false);
        statusSVG_set(HTML_statusSvgMicrochip, false);
    };

    websocket.onmessage = (msg) => {
	const res = JSON.parse(msg.data);
	const message = new Message(res.type, res.option, res.data);
	console.log(res);
	switch (message.type) {
	    case MsgType.REQUEST: {
		handleRequest(message);
		break;
	    }
	    case MsgType.EVENT: {
		handleEvent(message);
		break;
	    }
	}
    };
}

function handleRequest(msg) {
    switch (msg.option) {
        case MsgRequest.GET_TASKS: {
            tasks = [];
            for (let i = 0; i < msg.data.tasks.length; i++) {
                const t = msg.data.tasks[i];
                tasks[i] = new Task(t.startHour, t.startMinute, t.endHour, t.endMinute, t.day, t.status, t.type);
            }
            generateTasksHTML();
            break;
        }
        case MsgRequest.GET_TODAYS_TASKS: {
            todaysTasks = [];
            for (let i = 0; i < msg.data.tasks.length; i++) {
                const t = msg.data.tasks[i];
                todaysTasks[i] = new Task(t.startHour, t.startMinute, t.endHour, t.endMinute, t.day, t.status, t.type);
            }
            generateTodaysTaskHTML();
            break;
        }
        case MsgRequest.GET_ACTIVE_TASK: {
            const t = msg.data.activeTask;
            console.log("got active task")
            if (t === null) {
                activeTask = null;
                isNextWeek = false;
            } else {
                activeTask = new Task(t.startHour, t.startMinute, t.endHour, t.endMinute, t.day, t.status, t.type);
                isNextWeek = msg.data.isNextWeek;
            }
            controllActiveTaskHTML();
            controllActiveTaskControlls();
            break;
        }
    }
}

function handleEvent(msg) {
    switch (msg.option) {
        case MsgEvent.ESP_CONNECTED: {
            statusSVG_set(HTML_statusSvgMicrochip, true);
            break;
        }
        case MsgEvent.ESP_DISCONNECTED: {
            statusSVG_set(HTML_statusSvgMicrochip, false);
            break;
        }
        case MsgEvent.STARTED:
        case MsgEvent.STOPPED: {
            if (msg.data !== null && msg.data.activeTask !== null && msg.data.endSecond !== null) {
                const t = msg.data.activeTask;
                activeTask = new Task(t.startHour, t.startMinute, t.endHour, t.endMinute, t.day, t.status, t.type);
                activeTaskEndSecond = msg.data.endSecond;
            } else {
                activeTask = null;
                activeTaskEndSecond = -1;
            }
            calcAnimationData();
            controllActiveTaskControlls();
            controllActiveTaskHTML();
            break;
        }
        case MsgEvent.LEAK_DETECTED: {

            break;
        }
        case MsgEvent.LEAK_RESOLVED: {

            break;
        }
    }
}

function sendMessage(type, option, data) {
    const msg = new Message(type, option, JSON.stringify(data));
    websocket.send(JSON.stringify(msg));
}

// Event listener for tab switch
document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
        connectWebSocket();
    }
});
// Initial WebSocket connection
document.addEventListener("DOMContentLoaded", () => {
    connectWebSocket();
});
