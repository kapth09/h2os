package com.kaptheo.watering;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum TaskType {
    STANDARD  (0),
    TEMPORARY (1),
    INSERTED  (2);

    TaskType(int v) { }
}
