package com.jodngan.task.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jodngan.task.application.TaskApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = TaskResponse.from(
                taskApplicationService.createTask(request.title(), request.dueAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public TaskListResponse listActiveTasks() {
        return TaskListResponse.from(taskApplicationService.findActiveTasks());
    }

    @GetMapping("/{taskId}")
    public TaskResponse getTask(@PathVariable UUID taskId) {
        return TaskResponse.from(taskApplicationService.findTask(taskId));
    }
}
