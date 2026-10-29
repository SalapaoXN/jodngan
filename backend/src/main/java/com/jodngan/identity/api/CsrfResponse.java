package com.jodngan.identity.api;

public record CsrfResponse(String headerName, String parameterName, String token) {
}
