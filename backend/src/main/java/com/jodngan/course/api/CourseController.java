package com.jodngan.course.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jodngan.course.application.CourseApplicationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseApplicationService courseApplicationService;

    public CourseController(CourseApplicationService courseApplicationService) {
        this.courseApplicationService = courseApplicationService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseNameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CourseResponse.from(courseApplicationService.createCourse(request.name())));
    }

    @GetMapping
    public CourseListResponse listCourses() {
        return CourseListResponse.from(courseApplicationService.findCourses());
    }

    @PutMapping("/{courseId}")
    public CourseResponse renameCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseNameRequest request) {
        return CourseResponse.from(courseApplicationService.renameCourse(courseId, request.name()));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> removeCourse(@PathVariable UUID courseId) {
        courseApplicationService.removeCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}
