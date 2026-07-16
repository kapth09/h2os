package com.kaptheo.watering.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum MsgRequest {
    GET_TASKS           (0),
    GET_ACTIVE_TASK     (1),
    GET_TODAYS_TASKS    (2);

    private int value;

    MsgRequest(int v) {this.value = v;}

    static public MsgRequest fromInt(int i) {
        for (MsgRequest r: MsgRequest.values()) {
            if (r.value == i) return r;
        }
        return null;
    }
}
