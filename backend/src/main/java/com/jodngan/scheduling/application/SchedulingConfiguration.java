package com.jodngan.scheduling.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jodngan.scheduling.domain.SchedulingEngine;

@Configuration(proxyBeanMethods = false)
class SchedulingConfiguration {

    @Bean
    SchedulingEngine schedulingEngine() {
        return new SchedulingEngine();
    }
}
