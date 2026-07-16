package com.kaptheo.watering;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum MsgEvent {
    STARTED             (0),
    STOPPED             (1),
    ESP_CONNECTED       (2),
    ESP_DISCONNECTED    (3),
    LEAK_DETECTED       (4),
    LEAK_RESOLVED       (5);

    private int value;

    MsgEvent(int v) {
        this.value = v;
    }

    static public MsgEvent fromInt(int i) {
        for (MsgEvent r: MsgEvent.values()) {
            if (r.value == i) return r;
        }
        return null;
    }
}
