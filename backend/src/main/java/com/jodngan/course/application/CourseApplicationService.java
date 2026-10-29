package com.jodngan.course.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jodngan.course.domain.Course;

@Service
public class CourseApplicationService implements CourseLookup {

    private final CourseRepository courseRepository;
    private final Clock clock;

    public CourseApplicationService(CourseRepository courseRepository, Clock clock) {
        this.courseRepository = courseRepository;
        this.clock = clock;
    }

    public CourseData createCourse(String name) {
        Instant now = clock.instant();
        return CourseData.from(courseRepository.save(Course.create(name, now)));
    }

    public List<CourseData> findCourses() {
        return courseRepository.findAll().stream().map(CourseData::from).toList();
    }

    public CourseData renameCourse(UUID id, String name) {
        Instant now = clock.instant();
        Course course = requireCourse(id).rename(name, now);
        return CourseData.from(courseRepository.save(course));
    }

    public void removeCourse(UUID id) {
        requireCourse(id);
        courseRepository.deleteById(id);
    }

    @Override
    public Optional<CourseData> findCourse(UUID id) {
        return courseRepository.findById(id).map(CourseData::from);
    }

    private Course requireCourse(UUID id) {
        return courseRepository.findById(id).orElseThrow(CourseNotFoundException::new);
    }
}
