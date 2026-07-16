package com.kaptheo.watering;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum WebMsgType {
    REQUEST (0),
    COMMAND (1),
    EVENT   (2);

    WebMsgType(int v) { }
}
