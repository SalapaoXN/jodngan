package com.jodngan.course.application;

import java.util.Optional;
import java.util.UUID;

public interface CourseLookup {
    Optional<CourseData> findCourse(UUID userId, UUID id);
}
