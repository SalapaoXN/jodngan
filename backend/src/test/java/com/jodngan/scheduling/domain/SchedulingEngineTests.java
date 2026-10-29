package com.jodngan.scheduling.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SchedulingEngineTests {

    private static final Instant NOW = Instant.parse("2026-10-29T00:00:00Z");
    private static final Instant CREATED_AT = NOW.minusSeconds(3_600);

    private final SchedulingEngine engine = new SchedulingEngine();

    @Test
    void ranksDeadlineBandsFromOverdueToLow() {
        SchedulableTask overdue = task("00000000-0000-0000-0000-000000000001", NOW.minusSeconds(60), CREATED_AT);
        SchedulableTask critical = task("00000000-0000-0000-0000-000000000002", NOW.plusSeconds(12 * 3_600L), CREATED_AT);
        SchedulableTask high = task("00000000-0000-0000-0000-000000000003", NOW.plusSeconds(48 * 3_600L), CREATED_AT);
        SchedulableTask medium = task("00000000-0000-0000-0000-000000000004", NOW.plusSeconds(4 * 86_400L), CREATED_AT);
        SchedulableTask low = task("00000000-0000-0000-0000-000000000005", NOW.plusSeconds(10 * 86_400L), CREATED_AT);

        List<RankedTask> ranked = engine.rank(List.of(low, medium, high, critical, overdue), NOW).rankedTasks();

        assertEquals(List.of(overdue.id(), critical.id(), high.id(), medium.id(), low.id()),
                ranked.stream().map(item -> item.task().id()).toList());
        assertEquals(List.of(
                        UrgencyBand.OVERDUE,
                        UrgencyBand.CRITICAL,
                        UrgencyBand.HIGH,
                        UrgencyBand.MEDIUM,
                        UrgencyBand.LOW),
                ranked.stream().map(RankedTask::urgency).toList());
    }

    @Test
    void deadlineExactlyNowIsCriticalButNotOverdue() {
        RankedTask ranked = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001", NOW, CREATED_AT)), NOW).rankedTasks().get(0);

        assertEquals(UrgencyBand.CRITICAL, ranked.urgency());
        assertEquals(0, ranked.remainingSeconds());
        assertFalse(ranked.overdue());
        assertEquals(RecommendationReason.DUE_WITHIN_24_HOURS, ranked.reason());
        assertEquals(StartGuidance.START_NOW, ranked.startGuidance());
    }

    @Test
    void exactDeadlineThresholdsStayInTheirInclusiveBands() {
        RankedTask at24Hours = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000001", NOW.plusSeconds(24 * 3_600L), CREATED_AT)), NOW)
                .rankedTasks().get(0);
        RankedTask at72Hours = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000002", NOW.plusSeconds(72 * 3_600L), CREATED_AT)), NOW)
                .rankedTasks().get(0);
        RankedTask atSevenDays = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000003", NOW.plusSeconds(7 * 86_400L), CREATED_AT)), NOW)
                .rankedTasks().get(0);
        RankedTask afterSevenDays = engine.rank(List.of(task(
                "00000000-0000-0000-0000-000000000004", NOW.plusSeconds(7 * 86_400L + 1), CREATED_AT)), NOW)
                .rankedTasks().get(0);

        assertEquals(UrgencyBand.CRITICAL, at24Hours.urgency());
        assertEquals(StartGuidance.START_NOW, at24Hours.startGuidance());
        assertEquals(UrgencyBand.HIGH, at72Hours.urgency());
        assertEquals(StartGuidance.START_TODAY, at72Hours.startGuidance());
        assertEquals(UrgencyBand.MEDIUM, atSevenDays.urgency());
        assertEquals(StartGuidance.START_SOON, atSevenDays.startGuidance());
        assertEquals(UrgencyBand.LOW, afterSevenDays.urgency());
        assertEquals(StartGuidance.CAN_WAIT, afterSevenDays.startGuidance());
    }

    @Test
    void earlierDeadlineWinsInsideTheSameUrgencyBand() {
        SchedulableTask later = task("00000000-0000-0000-0000-000000000001", NOW.plusSeconds(20 * 3_600L), CREATED_AT);
        SchedulableTask earlier = task("00000000-0000-0000-0000-000000000002", NOW.plusSeconds(18 * 3_600L), CREATED_AT);

        List<RankedTask> ranked = engine.rank(List.of(later, earlier), NOW).rankedTasks();

        assertEquals(earlier.id(), ranked.get(0).task().id());
    }

    @Test
    void earlierCreatedAtBreaksAnIdenticalDeadlineTie() {
        Instant dueAt = NOW.plusSeconds(2 * 86_400L);
        SchedulableTask laterCreated = task(
                "00000000-0000-0000-0000-000000000001", dueAt, CREATED_AT.plusSeconds(60));
        SchedulableTask earlierCreated = task(
                "00000000-0000-0000-0000-000000000002", dueAt, CREATED_AT);

        List<RankedTask> ranked = engine.rank(List.of(laterCreated, earlierCreated), NOW).rankedTasks();

        assertEquals(earlierCreated.id(), ranked.get(0).task().id());
    }

    @Test
    void uuidLexicalOrderIsTheFinalStableTieBreaker() {
        Instant dueAt = NOW.plusSeconds(2 * 86_400L);
        SchedulableTask second = task("00000000-0000-0000-0000-000000000002", dueAt, CREATED_AT);
        SchedulableTask first = task("00000000-0000-0000-0000-000000000001", dueAt, CREATED_AT);

        List<RankedTask> ranked = engine.rank(List.of(second, first), NOW).rankedTasks();

        assertEquals(first.id(), ranked.get(0).task().id());
        assertEquals(second.id(), ranked.get(1).task().id());
    }

    @Test
    void mapsDeadlineBandsToExplainableReasonAndGuidance() {
        List<RankedTask> ranked = engine.rank(List.of(
                task("00000000-0000-0000-0000-000000000001", NOW.minusSeconds(1), CREATED_AT),
                task("00000000-0000-0000-0000-000000000002", NOW.plusSeconds(1), CREATED_AT),
                task("00000000-0000-0000-0000-000000000003", NOW.plusSeconds(48 * 3_600L), CREATED_AT),
                task("00000000-0000-0000-0000-000000000004", NOW.plusSeconds(5 * 86_400L), CREATED_AT),
                task("00000000-0000-0000-0000-000000000005", NOW.plusSeconds(10 * 86_400L), CREATED_AT)), NOW).rankedTasks();

        assertEquals(RecommendationReason.OVERDUE, ranked.get(0).reason());
        assertEquals(StartGuidance.START_NOW, ranked.get(0).startGuidance());
        assertEquals(RecommendationReason.DUE_WITHIN_24_HOURS, ranked.get(1).reason());
        assertEquals(StartGuidance.START_NOW, ranked.get(1).startGuidance());
        assertEquals(RecommendationReason.DUE_SOON, ranked.get(2).reason());
        assertEquals(StartGuidance.START_TODAY, ranked.get(2).startGuidance());
        assertEquals(RecommendationReason.DUE_SOON, ranked.get(3).reason());
        assertEquals(StartGuidance.START_SOON, ranked.get(3).startGuidance());
        assertEquals(RecommendationReason.UPCOMING, ranked.get(4).reason());
        assertEquals(StartGuidance.CAN_WAIT, ranked.get(4).startGuidance());
        assertTrue(ranked.get(0).overdue());
    }

    @Test
    void producesTheSameRankingForTheSameInputsAndNow() {
        List<SchedulableTask> tasks = List.of(
                task("00000000-0000-0000-0000-000000000002", NOW.plusSeconds(86_400), CREATED_AT),
                task("00000000-0000-0000-0000-000000000001", NOW.plusSeconds(86_400), CREATED_AT));

        List<UUID> first = engine.rank(tasks, NOW).rankedTasks().stream().map(item -> item.task().id()).toList();
        List<UUID> second = engine.rank(tasks, NOW).rankedTasks().stream().map(item -> item.task().id()).toList();

        assertEquals(first, second);
    }

    private static SchedulableTask task(String id, Instant dueAt, Instant createdAt) {
        return new SchedulableTask(
                UUID.fromString(id),
                "Task " + id.substring(id.length() - 1),
                dueAt,
                null,
                null,
                createdAt,
                createdAt);
    }
}
