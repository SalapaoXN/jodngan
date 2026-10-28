package com.jodngan.task.application;

import java.util.List;

public interface TaskPlanningReader {

    List<TaskPlanningData> findActivePlanningTasks();
}
