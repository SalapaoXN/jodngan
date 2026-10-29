package com.jodngan.course.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCourseRepository extends JpaRepository<CourseEntity, UUID> {
    List<CourseEntity> findAllByOrderByNameAscIdAsc();
}
