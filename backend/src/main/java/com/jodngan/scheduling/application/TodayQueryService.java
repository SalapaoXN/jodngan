package com.jodngan.scheduling.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jodngan.scheduling.domain.RankedTask;
import com.jodngan.scheduling.domain.SchedulableTask;
import com.jodngan.scheduling.domain.SchedulingEngine;
import com.jodngan.task.application.TaskPlanningData;
import com.jodngan.task.application.TaskPlanningReader;

@Service
public class TodayQueryService {

    private static final int UPCOMING_LIMIT = 5;

    private final TaskPlanningReader taskPlanningReader;
    private final SchedulingEngine schedulingEngine;
    private final Clock clock;

    public TodayQueryService(
            TaskPlanningReader taskPlanningReader,
            SchedulingEngine schedulingEngine,
            Clock clock) {
        this.taskPlanningReader = taskPlanningReader;
        this.schedulingEngine = schedulingEngine;
        this.clock = clock;
    }

    public TodayView getToday() {
        Instant now = clock.instant();
        List<SchedulableTask> inputs = taskPlanningReader.findActivePlanningTasks().stream()
                .map(TodayQueryService::toSchedulableTask)
                .toList();

        List<RankedTask> ranked = schedulingEngine.rank(inputs, now).rankedTasks();
        TodayView.Recommendation primary = ranked.isEmpty() ? null : toRecommendation(ranked.get(0));
        List<TodayView.Recommendation> upcoming = ranked.stream()
                .skip(1)
                .limit(UPCOMING_LIMIT)
                .map(TodayQueryService::toRecommendation)
                .toList();

        return new TodayView(now, primary, upcoming);
    }

    private static SchedulableTask toSchedulableTask(TaskPlanningData task) {
        return new SchedulableTask(
                task.id(),
                task.title(),
                task.dueAt(),
                task.createdAt(),
                task.updatedAt());
    }

    private static TodayView.Recommendation toRecommendation(RankedTask ranked) {
        SchedulableTask task = ranked.task();
        TaskPlanningData taskData = new TaskPlanningData(
                task.id(),
                task.title(),
                task.dueAt(),
                task.createdAt(),
                task.updatedAt());
        return new TodayView.Recommendation(
                taskData,
                ranked.urgency(),
                ranked.reason(),
                ranked.startGuidance(),
                ranked.remainingSeconds(),
                ranked.overdue());
    }
}
