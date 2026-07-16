package com.kaptheo.watering.tasks;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.NUMBER)
public enum TaskStatus {
    READY 		       (0),
    DEACTIVATED 	   (1),
    PAUSED   	       (2),
    RUNNING  	       (3),
    FINISHED 	       (4);

    TaskStatus(int v) { }
}