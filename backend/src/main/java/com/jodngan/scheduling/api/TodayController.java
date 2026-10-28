package com.jodngan.scheduling.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jodngan.scheduling.application.TodayQueryService;

@RestController
@RequestMapping("/api/today")
public class TodayController {

    private final TodayQueryService todayQueryService;

    public TodayController(TodayQueryService todayQueryService) {
        this.todayQueryService = todayQueryService;
    }

    @GetMapping
    public TodayResponse getToday() {
        return TodayResponse.from(todayQueryService.getToday());
    }
}
