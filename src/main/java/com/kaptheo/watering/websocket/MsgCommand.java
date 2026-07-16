package com.kaptheo.watering.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum MsgCommand {
    SET_TASKS           (0),
    SET_TODAYS_TASKS    (1),
    START_NOW           (2),
    STOP_NOW            (3),
    PAUSE_TASK          (4),
    RESUME_TASK         (5),
    RELOAD_TODAYS_TASKS (6);

    private int value;

    MsgCommand(int v) {this.value = v;}

    static public MsgCommand fromInt(int i) {
        for (MsgCommand r: MsgCommand.values()) {
            if (r.value == i) return r;
        }
        return null;
    }
}
