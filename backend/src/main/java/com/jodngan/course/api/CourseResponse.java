package com.jodngan.course.api;

import java.util.UUID;

import com.jodngan.course.application.CourseData;

public record CourseResponse(UUID id, String name) {
    static CourseResponse from(CourseData course) {
        return new CourseResponse(course.id(), course.name());
    }
}
