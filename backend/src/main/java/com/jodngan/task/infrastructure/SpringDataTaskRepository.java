package com.jodngan.task.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jodngan.task.domain.TaskStatus;

public interface SpringDataTaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByUserIdAndStatusOrderByDueAtAscIdAsc(UUID userId, TaskStatus status);

    List<TaskEntity> findByUserIdAndStatusAndCourseIdOrderByDueAtAscIdAsc(UUID userId, TaskStatus status, UUID courseId);

    List<TaskEntity> findByUserIdAndStatusOrderByCompletedAtDescIdAsc(UUID userId, TaskStatus status);

    List<TaskEntity> findByUserIdAndStatusAndCourseIdOrderByCompletedAtDescIdAsc(UUID userId, TaskStatus status, UUID courseId);

    java.util.Optional<TaskEntity> findByIdAndUserId(UUID id, UUID userId);
}
