package com.jodngan.course.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jodngan.course.domain.Course;

public interface CourseRepository {
    Course save(Course course);
    List<Course> findAll(UUID userId);
    Optional<Course> findById(UUID userId, UUID id);
    void deleteById(UUID userId, UUID id);
}
