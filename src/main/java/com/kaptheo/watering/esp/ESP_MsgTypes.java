package com.kaptheo.watering;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum ESP_MsgTypes {
    MSG_START         ((byte) 0),
    MSG_STARTED       ((byte) 1),
    MSG_STOP          ((byte) 2),
    MSG_STOPPED       ((byte) 3),
    MSG_LEAK_DETECTED ((byte) 4),
    MSG_LEAK_RESOLVED ((byte) 5);

    private final byte value;

    ESP_MsgTypes(byte v) {
        value = v;
    }

    public static ESP_MsgTypes fromInt(int i) {
        for (ESP_MsgTypes e : ESP_MsgTypes.values()) {
            if (e.value == i) {
                return e;
            }
        }
        return MSG_START;
    }
}