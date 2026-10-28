package com.jodngan.scheduling.domain;

import java.util.List;

public record SchedulingResult(List<RankedTask> rankedTasks) {

    public SchedulingResult {
        rankedTasks = List.copyOf(rankedTasks);
    }
}
