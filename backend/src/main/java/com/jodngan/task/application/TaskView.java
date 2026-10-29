package com.jodngan.task.application;

import com.jodngan.course.application.CourseData;
import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskDeadlineProgress;

public record TaskView(Task task, CourseData course, TaskDeadlineProgress planning) {
}
