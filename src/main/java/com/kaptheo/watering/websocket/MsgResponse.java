package com.kaptheo.watering.websocket;

import com.kaptheo.watering.tasks.Task;

public record MsgResponse<T>(WebMsgType type, int option, T data) {
    public record REQ_GET_TASKS(Task[] tasks)                   implements Sendable {
        @Override
        public int optionIndex() { return MsgRequest.GET_TASKS.ordinal(); }
    }

    public record REQ_GET_ACTIVE_TASK(Task activeTask, boolean isNextWeek) implements Sendable {
        @Override
        public int optionIndex() { return MsgRequest.GET_ACTIVE_TASK.ordinal(); }
    }

    public record REQ_GET_TODAYS_TASKS(Task[] tasks)             implements Sendable {
        @Override
        public int optionIndex() { return MsgRequest.GET_TODAYS_TASKS.ordinal(); }
    }

    public record CMD_SET_TASKS(Task[] tasks)                   implements Sendable {
        @Override
        public int optionIndex() { return MsgCommand.SET_TASKS.ordinal(); }
    }

    public record CMD_SET_TODAYS_TASKS(Task[] tasks)            implements Sendable {
        @Override
        public int optionIndex() { return MsgCommand.SET_TODAYS_TASKS.ordinal(); }
    }

    public record CMD_START_NOW(int duration)                   implements Sendable {
        @Override
        public int optionIndex() { return MsgCommand.START_NOW.ordinal(); }
    }

    public record CMD_STOP_NOW()                                implements Sendable {
        @Override
        public int optionIndex() { return MsgCommand.STOP_NOW.ordinal(); }
    }

    public record CMD_PAUSE_TASK()                              implements Sendable {
        @Override
        public int optionIndex() { return MsgCommand.PAUSE_TASK.ordinal(); }
    }

    public record CMD_RESUME_TASK()                             implements Sendable {
        @Override
        public int optionIndex() { return MsgCommand.RESUME_TASK.ordinal(); }
    }
    public record CMD_RELOAD_TODAYS_TASKS()                     implements Sendable {
        @Override
        public int optionIndex() { return  MsgCommand.RELOAD_TODAYS_TASKS.ordinal(); }
    }

    public record EVT_STARTED(Task activeTask, int endSecond)   implements Sendable {
        @Override
        public int optionIndex() { return MsgEvent.STARTED.ordinal(); }
    }

    public record EVT_STOPPED(Task activeTask, int endSecond)   implements Sendable {
        @Override
        public int optionIndex() { return MsgEvent.STOPPED.ordinal(); }
    }

    public record EVT_ESP_CONNECTED()                           implements Sendable {
        @Override
        public int optionIndex() { return MsgEvent.ESP_CONNECTED.ordinal(); }
    }

    public record EVT_ESP_DISCONNECTED()                        implements Sendable {
        @Override
        public int optionIndex() { return MsgEvent.ESP_DISCONNECTED.ordinal(); }
    }

    public record EVT_LEAK_DETECTED()                           implements Sendable {
        @Override
        public int optionIndex() { return MsgEvent.LEAK_DETECTED.ordinal(); }
    }

    public record EVT_LEAK_RESOLVED()                           implements Sendable {
        @Override
        public int optionIndex() { return MsgEvent.LEAK_RESOLVED.ordinal(); }
    }
}
