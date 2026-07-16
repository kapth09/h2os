package com.kaptheo.watering.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Objects;

public record Task(int startHour, int startMinute, int endHour, int endMinute, int day, TaskStatus status, TaskType type) implements Comparable<Task>, Serializable {
	@JsonIgnore
	public int getStartTimeStamp() {
		return ((startHour() << 8) | startMinute());
	}
	@JsonIgnore
	public int getEndTimeStamp() {
		return ((endHour() << 8) | endMinute());
	}

	public Task withStatus(TaskStatus newStatus) {
		return new Task(startHour, startMinute, endHour, endMinute, day, newStatus, type);
	}

	@Override
	public String toString() {
		return day + ": [" + startHour() + ":" + startMinute() + "] [" + endHour() + ":" + endMinute() + "]" + " (" + status.name() + ")"; //0: [xx:yy] [zz:aa] (status)
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Task)) return false;
		Task other = (Task)o;
		if (this.day != other.day) return false;
		if (this.startHour != other.startHour) return false;
		if (this.startMinute != other.startMinute) return false;
		if (this.endHour != other.endHour) return false;
		if (this.endMinute != other.endMinute) return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(startHour, startMinute, endHour, endMinute, day);
	}

	@Override
	public int compareTo(Task otherTask) {
		if (this.day() > otherTask.day()) return  1; 	// this Task is later than the other one
		if (this.day() < otherTask.day()) return -1; 	// this task is earlier than the other one

		int thisTime = this.getEndTimeStamp();
		int otherTime = otherTask.getEndTimeStamp();

		if (thisTime > otherTime) return  1; 	// this Task is later than the other one
		if (thisTime < otherTime) return -1; 	// this task is earlier than the other one

		return 0;
	}
}
