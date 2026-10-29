package com.jodngan.course.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCourseRepository extends JpaRepository<CourseEntity, UUID> {
    List<CourseEntity> findAllByUserIdOrderByNameAscIdAsc(UUID userId);
    java.util.Optional<CourseEntity> findByIdAndUserId(UUID id, UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
