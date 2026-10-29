package com.jodngan.course.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseNameRequest(
        @NotBlank(message = "Course name is required")
        @Size(max = 120, message = "Course name must be 120 characters or fewer")
        String name) {
}
