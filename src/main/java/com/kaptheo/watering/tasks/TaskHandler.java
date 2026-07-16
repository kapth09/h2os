package com.kaptheo.watering;

import com.kaptheo.watering.esp.ESP_MsgTypes;
import com.kaptheo.watering.esp.EspHandler;
import com.kaptheo.watering.esp.EspState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TaskHandler {
    private int taskIndex;
    private int endSecond;
    private boolean isNextWeek;
    private String FILEPATH = "./volume/schedule.data";
    private List<Task> schedule;
    private List<Task> todaysSchedule;
    private Map<EspState, Sendable> espEvents;
    private Task activeTask;
    private EspHandler espHandler;
    private WebSocketHandler webSocketHandler;

    public TaskHandler(EspHandler espHandler, WebSocketHandler webSocketHandler) {
        this.espHandler = espHandler;
        this.espHandler.setTaskHandler(this);
        this.webSocketHandler = webSocketHandler;
        this.webSocketHandler.setTaskManager(this);
        this.schedule = new CopyOnWriteArrayList<>();
        this.todaysSchedule = new CopyOnWriteArrayList<>();
        this.espEvents = new HashMap<>();
        this.espEvents.put(EspState.WATERING_STATUS, new MsgResponse.EVT_STOPPED(null, 0));
        this.espEvents.put(EspState.ESP_STATUS, new MsgResponse.EVT_ESP_DISCONNECTED());
        this.espEvents.put(EspState.LEAK_STATUS, new MsgResponse.EVT_LEAK_RESOLVED());
        LocalDateTime now = LocalDateTime.now();
        taskIndex = now.getDayOfWeek().getValue();
        readTasksFromFile();
    }

    public void setSchedule(Task[] newTasks) {
        synchronized (this) {
            espHandler.writeEsp(ESP_MsgTypes.MSG_STOP);
            schedule.clear();
            Collections.addAll(schedule, newTasks);
            Collections.sort(schedule);
            writeTasksToFile();
            refresh();
            sendTasks();
        }
    }

    public void setTodaysSchedule(Task[] newTasks) {
        synchronized (this) {
            espHandler.writeEsp(ESP_MsgTypes.MSG_STOP);
            todaysSchedule.clear();
            Collections.addAll(todaysSchedule, newTasks);
            Collections.sort(todaysSchedule);
            refresh();
        }
    }

    public List<Task> getSchedule() { return schedule; }
    public List<Task> getTodaysSchedule() { return todaysSchedule; }
    public boolean getIsNextWeek() { return isNextWeek; }

    private void copyIntoTodaysSchedule() {
        synchronized (this) {
            todaysSchedule.clear();
            LocalDateTime now = LocalDateTime.now();
            for (Task t : schedule) {
                if (t.day() == now.getDayOfWeek().getValue()) {
                    todaysSchedule.add(new Task(
                            t.startHour(),
                            t.startMinute(),
                            t.endHour(),
                            t.endMinute(),
                            t.day(),
                            t.status(),
                            TaskType.TEMPORARY
                    ));
                }
            }
        }
    }

    private void cleanSchedule(boolean restart) {
        synchronized (this) {
            int day = LocalDateTime.now().getDayOfWeek().getValue();
	    schedule.removeIf(t -> t.type() == TaskType.INSERTED || (t.type() == TaskType.TEMPORARY && t.day() != day));
	    if (restart) {
		schedule.replaceAll(t -> t.status() == TaskStatus.FINISHED ? t.withStatus(TaskStatus.READY) : t);
	    }
        }
    }

    private void writeTasksToFile() {
        synchronized (this) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILEPATH))) {
                oos.writeObject(schedule);
            } catch (IOException e) {
                System.out.println(Logger.error(e.getMessage()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void readTasksFromFile() {
        synchronized (this) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILEPATH))) {
                schedule = (CopyOnWriteArrayList<Task>) ois.readObject();
                cleanSchedule(true);
                copyIntoTodaysSchedule();
                refresh();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println(Logger.error(e.getStackTrace(), 10, "Schedule not found"));
            }
        }
    }

    public Task getActiveTask() {return activeTask;}

    public Sendable getEspState(EspState state) {
        return espEvents.get(state);
    }

    private void sendTasks() {
        MsgResponse.REQ_GET_TASKS req = new MsgResponse.REQ_GET_TASKS(this.getSchedule().toArray(new Task[0]));
        webSocketHandler.broadcastMessage(WebMsgType.REQUEST, req, req);
    }

    private void sendActiveTask() {
        MsgResponse.REQ_GET_ACTIVE_TASK req = new MsgResponse.REQ_GET_ACTIVE_TASK(this.getActiveTask(), this.getIsNextWeek());
        webSocketHandler.broadcastMessage(WebMsgType.REQUEST, req, req);
    }

    public void espConnected() {
        espEvents.put(EspState.ESP_STATUS, new MsgResponse.EVT_ESP_CONNECTED());
        MsgResponse.EVT_ESP_CONNECTED option = new MsgResponse.EVT_ESP_CONNECTED();
        webSocketHandler.broadcastMessage(WebMsgType.EVENT, option, null);
    }

    public void espDisconnected() {
        espEvents.put(EspState.ESP_STATUS, new MsgResponse.EVT_ESP_DISCONNECTED());
        MsgResponse.EVT_ESP_DISCONNECTED option = new MsgResponse.EVT_ESP_DISCONNECTED();
        webSocketHandler.broadcastMessage(WebMsgType.EVENT, option, null);
    }

    public void espStarted() {
        espEvents.put(EspState.WATERING_STATUS, new MsgResponse.EVT_STARTED(activeTask, endSecond));
        MsgResponse.EVT_STARTED option = new MsgResponse.EVT_STARTED(activeTask, endSecond);
        webSocketHandler.broadcastMessage(WebMsgType.EVENT, option, option);
    }

    public void espStopped() {
        espEvents.put(EspState.WATERING_STATUS, new MsgResponse.EVT_STOPPED(activeTask, endSecond));
        MsgResponse.EVT_STOPPED option = new MsgResponse.EVT_STOPPED(activeTask, endSecond);
        webSocketHandler.broadcastMessage(WebMsgType.EVENT, option, option);
    }

    public void startNow(int duration) {
        synchronized (this) {
            LocalDateTime now = LocalDateTime.now();
            Task nowTask = new Task(
                    now.getHour(),
                    now.getMinute(),
                    now.plusMinutes(duration).getHour(),
                    now.plusMinutes(duration).getMinute(),
                    now.getDayOfWeek().getValue(),
                    TaskStatus.READY,
                    TaskType.INSERTED
            );
            todaysSchedule.add(nowTask);
            Collections.sort(todaysSchedule);
            refresh();
        }
    }

    private void refresh() {
        synchronized (this) {
            LocalDateTime now = LocalDateTime.now();
            taskIndex = now.getDayOfWeek().getValue();
            activeTask = null;
            int nowDay = now.getDayOfWeek().getValue();
            int nowTimeStamp = (now.getHour() << 8) | now.getMinute();
            boolean isToday = false;
            for (int i = 0; i < todaysSchedule.size(); i++) {
                Task t = todaysSchedule.get(i);
                if (t.status() == TaskStatus.FINISHED || t.status() == TaskStatus.DEACTIVATED) continue;
                if (nowTimeStamp < t.getEndTimeStamp()) {
                    activeTask = t;
                    taskIndex = i;
                    isToday = true;
                    isNextWeek = false;
                    break;
                }
            }
            if (isToday == false) {
                int taskDiff = -1;
                for (int i = 0; i < schedule.size(); i++) {
                    Task t = schedule.get(i);
                    if (t.status() == TaskStatus.DEACTIVATED || t.status() == TaskStatus.FINISHED) {
                        continue;
                    }
                    int tempTaskDiff = t.day() - nowDay;
                    if (tempTaskDiff <= 0) {
                        tempTaskDiff = 7 + tempTaskDiff;
                    }
                    if (activeTask == null) {
                        activeTask = t;
                        taskIndex = i;
                        taskDiff = tempTaskDiff;
                    } else if (tempTaskDiff < taskDiff) {
                        activeTask = t;
                        taskIndex = i;
                        taskDiff = tempTaskDiff;
                    } else if (tempTaskDiff == taskDiff && t.getStartTimeStamp() < activeTask.getStartTimeStamp()) {
                        activeTask = t;
                        taskIndex = i;
                    }
                }
                if (activeTask != null) {
                    isNextWeek = activeTask.day() <= nowDay;
                }
            }
            sendActiveTask();
        }
    }


    public void startWatering() {
        synchronized (this) {
            if (activeTask == null) return;
            activeTask = activeTask.withStatus(TaskStatus.RUNNING);
            espHandler.writeEsp(ESP_MsgTypes.MSG_START);
        }
    }
    public void stopWatering() {
        synchronized (this) {
            if (activeTask == null) return;
            if (activeTask.type() == TaskType.INSERTED) {
                todaysSchedule.remove(activeTask);
            } else if (activeTask.type() == TaskType.TEMPORARY) {
                todaysSchedule.set(taskIndex, activeTask.withStatus(TaskStatus.FINISHED));
            } else {
                schedule.set(taskIndex, activeTask.withStatus(TaskStatus.FINISHED));
            }
            endSecond = 0;
            refresh();
            espHandler.writeEsp(ESP_MsgTypes.MSG_STOP);
        }
    }
    public void resumeWatering() {
        synchronized (this) {
            if (activeTask == null) return;
            if (activeTask.status() != TaskStatus.PAUSED) return;
            activeTask = activeTask.withStatus(TaskStatus.RUNNING);
            espHandler.writeEsp(ESP_MsgTypes.MSG_START);
        }
    }
    public void pauseWatering() {
        synchronized (this) {
            if (activeTask == null) return;
            if (activeTask.status() != TaskStatus.RUNNING) return;
            activeTask = activeTask.withStatus(TaskStatus.PAUSED);
            espHandler.writeEsp(ESP_MsgTypes.MSG_STOP);
        }
    }

    public void reloadTodaysTasks() {
        synchronized (this) {
            copyIntoTodaysSchedule();
            refresh();
            MsgResponse.REQ_GET_TODAYS_TASKS req = new MsgResponse.REQ_GET_TODAYS_TASKS(getTodaysSchedule().toArray(new Task[0]));
            webSocketHandler.broadcastMessage(WebMsgType.REQUEST, req, req);
        }
    }

    @Scheduled(fixedRate = 1000)
    public void scheduleTasks() {
        synchronized (this) {
            if (activeTask == null) return;
            LocalDateTime now = LocalDateTime.now();
            if (activeTask.day() != now.getDayOfWeek().getValue()) return;
            int nowTimeStamp = (now.getHour() << 8) | now.getMinute();
            TaskStatus status = activeTask.status();
            if (status == TaskStatus.RUNNING || status == TaskStatus.PAUSED) {
		int taskEndSeconds = (activeTask.endHour() * 3600) + (activeTask.endMinute() * 60) + endSecond;
		int daySeconds = now.toLocalTime().toSecondOfDay();
                if (daySeconds >= taskEndSeconds) {
                    stopWatering();
                }
            } else if (status == TaskStatus.READY && isNextWeek == false){
                if (nowTimeStamp >= activeTask.getStartTimeStamp() && nowTimeStamp < activeTask.getEndTimeStamp()) {
                    endSecond = now.getSecond();
                    startWatering();
                }
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * SUN")
    protected void restartSchedule() {
        isNextWeek = false;
        cleanSchedule(true);
        refresh();
    }

    @Scheduled(cron = "0 0 0 * * *")
    protected void cleanTodaysTasks() {
        copyIntoTodaysSchedule();
    }
}
