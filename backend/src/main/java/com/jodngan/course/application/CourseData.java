package com.jodngan.course.application;

import java.util.UUID;

import com.jodngan.course.domain.Course;

public record CourseData(UUID id, String name) {
    public static CourseData from(Course course) {
        return new CourseData(course.id(), course.name());
    }
}
