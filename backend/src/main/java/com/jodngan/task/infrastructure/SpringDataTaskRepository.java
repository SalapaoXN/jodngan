package com.jodngan.task.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jodngan.task.domain.TaskStatus;

public interface SpringDataTaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByStatusOrderByDueAtAscIdAsc(TaskStatus status);

    List<TaskEntity> findByStatusAndCourseIdOrderByDueAtAscIdAsc(TaskStatus status, UUID courseId);

    List<TaskEntity> findByStatusOrderByCompletedAtDescIdAsc(TaskStatus status);

    List<TaskEntity> findByStatusAndCourseIdOrderByCompletedAtDescIdAsc(TaskStatus status, UUID courseId);
}
