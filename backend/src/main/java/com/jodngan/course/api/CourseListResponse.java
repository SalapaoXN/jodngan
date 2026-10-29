package com.jodngan.course.api;

import java.util.List;

import com.jodngan.course.application.CourseData;

public record CourseListResponse(List<CourseResponse> items) {
    static CourseListResponse from(List<CourseData> courses) {
        return new CourseListResponse(courses.stream().map(CourseResponse::from).toList());
    }
}
