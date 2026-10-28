package com.jodngan.task.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jodngan.task.domain.TaskStatus;

public interface SpringDataTaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByStatusOrderByDueAtAscIdAsc(TaskStatus status);
}
