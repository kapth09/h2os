package com.kaptheo.watering.esp;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum EspState {
    WATERING_STATUS (0),
    ESP_STATUS      (1),
    LEAK_STATUS     (2);

    EspState(int v) { }
}
