package com.jodngan.shared.api;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(String code, String message, Map<String, String> fieldErrors) {
}
