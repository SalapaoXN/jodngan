package com.jodngan.task.application;

import com.jodngan.task.domain.Task;
import com.jodngan.task.domain.TaskDeadlineProgress;

public record TaskView(Task task, TaskDeadlineProgress planning) {
}
