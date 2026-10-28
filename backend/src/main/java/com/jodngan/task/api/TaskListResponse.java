package com.jodngan.task.api;

import java.util.List;

import com.jodngan.task.application.TaskView;

public record TaskListResponse(List<TaskResponse> items) {

    static TaskListResponse from(List<TaskView> tasks) {
        return new TaskListResponse(tasks.stream().map(TaskResponse::from).toList());
    }
}
