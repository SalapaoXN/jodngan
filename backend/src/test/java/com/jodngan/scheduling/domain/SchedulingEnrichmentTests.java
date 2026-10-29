package com.jodngan.scheduling.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SchedulingEnrichmentTests {

    private static final Instant NOW = Instant.parse("2026-10-29T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3_600);

    private final SchedulingEngine engine = new SchedulingEngine();

    @Test
    void knownEffortCanChangeRankingInsideTheSameDeadlineBand() {
        SchedulableTask earlierSmall = task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(4 * 86_400L),
                60,
                null);
        SchedulableTask laterLarge = task(
                "00000000-0000-0000-0000-000000000002",
                NOW.plusSeconds(4 * 86_400L + 3_600),
                1_800,
                null);

        List<RankedTask> ranked = engine.rank(List.of(earlierSmall, laterLarge), NOW).rankedTasks();

        assertEquals(laterLarge.id(), ranked.get(0).task().id());
        assertEquals(60, ranked.get(0).recommendationScore());
        assertEquals(40, ranked.get(1).recommendationScore());
    }

    @Test
    void removingEffortFallsBackToDeadlineOrdering() {
        SchedulableTask earlier = task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(4 * 86_400L),
                null,
                null);
        SchedulableTask later = task(
                "00000000-0000-0000-0000-000000000002",
                NOW.plusSeconds(4 * 86_400L + 3_600),
                null,
                null);

        List<RankedTask> ranked = engine.rank(List.of(later, earlier), NOW).rankedTasks();

        assertEquals(earlier.id(), ranked.get(0).task().id());
        assertEquals(40, ranked.get(0).recommendationScore());
        assertNull(ranked.get(0).effortPressureRatio());
    }

    @Test
    void importanceAdjustsScoreAndBreaksOtherwiseIdenticalTasks() {
        Instant dueAt = NOW.plusSeconds(10 * 86_400L);
        SchedulableTask low = task(
                "00000000-0000-0000-0000-000000000001",
                dueAt,
                null,
                PlanningImportance.LOW);
        SchedulableTask normal = task(
                "00000000-0000-0000-0000-000000000002",
                dueAt,
                null,
                PlanningImportance.NORMAL);
        SchedulableTask high = task(
                "00000000-0000-0000-0000-000000000003",
                dueAt,
                null,
                PlanningImportance.HIGH);

        List<RankedTask> ranked = engine.rank(List.of(low, normal, high), NOW).rankedTasks();

        assertEquals(List.of(high.id(), normal.id(), low.id()),
                ranked.stream().map(item -> item.task().id()).toList());
        assertEquals(List.of(30, 20, 15),
                ranked.stream().map(RankedTask::recommendationScore).toList());
        assertEquals(RecommendationReason.HIGH_IMPORTANCE, ranked.get(0).reason());
        assertEquals(StartGuidance.START_SOON, ranked.get(0).startGuidance());
    }

    @Test
    void effortPressureUsesAcceptedBonusThresholds() {
        long fourDaysSeconds = 4 * 86_400L;
        assertEquals(40, rankedScore(fourDaysSeconds, 575));
        assertEquals(50, rankedScore(fourDaysSeconds, 576));
        assertEquals(60, rankedScore(fourDaysSeconds, 1_440));
        assertEquals(70, rankedScore(fourDaysSeconds, 2_880));
    }

    @Test
    void highEffortPressureExplainsRecommendationAndStartsToday() {
        RankedTask ranked = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(10 * 86_400L),
                4_000,
                null)), NOW).rankedTasks().get(0);

        assertEquals(RecommendationReason.HIGH_EFFORT_PRESSURE, ranked.reason());
        assertEquals(StartGuidance.START_TODAY, ranked.startGuidance());
    }

    @Test
    void overdueIsAlwaysAtRisk() {
        RankedTask ranked = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001",
                NOW.minusSeconds(60),
                null,
                null)), NOW).rankedTasks().get(0);

        assertEquals(RiskState.AT_RISK, ranked.risk().state());
        assertEquals(RiskReason.OVERDUE, ranked.risk().reasonCode());
        assertEquals(RecommendationReason.OVERDUE, ranked.reason());
        assertEquals(StartGuidance.START_NOW, ranked.startGuidance());
    }

    @Test
    void unknownEffortReportsInsufficientDataWithoutBlockingRecommendation() {
        RankedTask ranked = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(2 * 86_400L),
                null,
                null)), NOW).rankedTasks().get(0);

        assertEquals(RiskState.INSUFFICIENT_DATA, ranked.risk().state());
        assertEquals(RiskReason.EFFORT_UNKNOWN, ranked.risk().reasonCode());
        assertEquals(UrgencyBand.HIGH, ranked.urgency());
    }

    @Test
    void effortThatCannotFitInsideClockTimeIsAtRisk() {
        RankedTask ranked = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(120 * 60L),
                120,
                null)), NOW).rankedTasks().get(0);

        assertEquals(RiskState.AT_RISK, ranked.risk().state());
        assertEquals(RiskReason.INSUFFICIENT_CLOCK_TIME, ranked.risk().reasonCode());
        assertEquals(RecommendationReason.INSUFFICIENT_CLOCK_TIME, ranked.reason());
        assertEquals(StartGuidance.START_NOW, ranked.startGuidance());
    }

    @Test
    void knownEffortBelowClockTimeHasNoConfirmedRisk() {
        RankedTask ranked = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(10 * 86_400L),
                120,
                null)), NOW).rankedTasks().get(0);

        assertEquals(RiskState.NO_CONFIRMED_RISK, ranked.risk().state());
        assertNull(ranked.risk().reasonCode());
    }

    private int rankedScore(long remainingSeconds, int estimatedMinutes) {
        return engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001",
                NOW.plusSeconds(remainingSeconds),
                estimatedMinutes,
                null)), NOW).rankedTasks().get(0).recommendationScore();
    }

    private static SchedulableTask task(
            String id,
            Instant dueAt,
            Integer estimatedMinutes,
            PlanningImportance importance) {
        return new SchedulableTask(
                UUID.fromString(id),
                "Task " + id.substring(id.length() - 1),
                dueAt,
                estimatedMinutes,
                importance,
                CREATED_AT,
                CREATED_AT);
    }
}
