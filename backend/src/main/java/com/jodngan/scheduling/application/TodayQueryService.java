package com.jodngan.scheduling.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jodngan.scheduling.domain.PlanningImportance;
import com.jodngan.scheduling.domain.RankedTask;
import com.jodngan.scheduling.domain.SchedulableTask;
import com.jodngan.scheduling.domain.SchedulingEngine;
import com.jodngan.task.application.TaskPlanningData;
import com.jodngan.task.application.TaskPlanningReader;
import com.jodngan.task.domain.TaskImportance;

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
        List<TaskPlanningData> planningTasks = taskPlanningReader.findActivePlanningTasks();
        Map<UUID, TaskPlanningData> tasksById = planningTasks.stream()
                .collect(Collectors.toMap(TaskPlanningData::id, task -> task));
        List<SchedulableTask> inputs = planningTasks.stream()
                .map(TodayQueryService::toSchedulableTask)
                .toList();

        List<RankedTask> ranked = schedulingEngine.rank(inputs, now).rankedTasks();
        TodayView.Recommendation primary = ranked.isEmpty()
                ? null
                : toRecommendation(ranked.get(0), tasksById.get(ranked.get(0).task().id()));
        List<TodayView.Recommendation> upcoming = ranked.stream()
                .skip(1)
                .limit(UPCOMING_LIMIT)
                .map(item -> toRecommendation(item, tasksById.get(item.task().id())))
                .toList();

        return new TodayView(now, primary, upcoming);
    }

    private static SchedulableTask toSchedulableTask(TaskPlanningData task) {
        return new SchedulableTask(
                task.id(),
                task.title(),
                task.dueAt(),
                task.estimatedMinutes(),
                toPlanningImportance(task.importance()),
                task.createdAt(),
                task.updatedAt());
    }

    private static TodayView.Recommendation toRecommendation(RankedTask ranked, TaskPlanningData taskData) {
        return new TodayView.Recommendation(
                taskData,
                ranked.urgency(),
                ranked.reason(),
                ranked.startGuidance(),
                ranked.risk(),
                ranked.remainingSeconds(),
                ranked.overdue());
    }

    private static PlanningImportance toPlanningImportance(TaskImportance importance) {
        if (importance == null) {
            return null;
        }
        return switch (importance) {
            case LOW -> PlanningImportance.LOW;
            case NORMAL -> PlanningImportance.NORMAL;
            case HIGH -> PlanningImportance.HIGH;
        };
    }

}
