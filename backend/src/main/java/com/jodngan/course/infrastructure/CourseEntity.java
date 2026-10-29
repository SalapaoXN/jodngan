package com.jodngan.course.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.jodngan.course.domain.Course;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
class CourseEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CourseEntity() {
    }

    private CourseEntity(Course course) {
        id = course.id();
        userId = course.userId();
        name = course.name();
        createdAt = course.createdAt();
        updatedAt = course.updatedAt();
    }

    static CourseEntity from(Course course) {
        return new CourseEntity(course);
    }

    Course toDomain() {
        return new Course(id, userId, name, createdAt, updatedAt);
    }
}
