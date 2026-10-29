package com.jodngan.course.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jodngan.course.domain.Course;
import com.jodngan.identity.application.CurrentUserProvider;

@Service
public class CourseApplicationService implements CourseLookup {

    private final CourseRepository courseRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CourseApplicationService(
            CourseRepository courseRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.courseRepository = courseRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public CourseData createCourse(String name) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return CourseData.from(courseRepository.save(Course.create(userId, name, now)));
    }

    public List<CourseData> findCourses() {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return courseRepository.findAll(userId).stream().map(CourseData::from).toList();
    }

    public CourseData renameCourse(UUID id, String name) {
        Instant now = clock.instant();
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        Course course = requireCourse(userId, id).rename(name, now);
        return CourseData.from(courseRepository.save(course));
    }

    @Transactional
    public void removeCourse(UUID id) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        requireCourse(userId, id);
        courseRepository.deleteById(userId, id);
    }

    @Override
    public Optional<CourseData> findCourse(UUID userId, UUID id) {
        return courseRepository.findById(userId, id).map(CourseData::from);
    }

    private Course requireCourse(UUID userId, UUID id) {
        return courseRepository.findById(userId, id).orElseThrow(CourseNotFoundException::new);
    }
}
