package com.jodngan.course.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.jodngan.course.application.CourseRepository;
import com.jodngan.course.domain.Course;

@Repository
public class CoursePersistenceAdapter implements CourseRepository {

    private final SpringDataCourseRepository repository;

    public CoursePersistenceAdapter(SpringDataCourseRepository repository) {
        this.repository = repository;
    }

    @Override
    public Course save(Course course) {
        return repository.save(CourseEntity.from(course)).toDomain();
    }

    @Override
    public List<Course> findAll() {
        return repository.findAllByOrderByNameAscIdAsc().stream()
                .map(CourseEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Course> findById(UUID id) {
        return repository.findById(id).map(CourseEntity::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
