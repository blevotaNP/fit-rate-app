package com.outfitrating.dto;

import jakarta.validation.constraints.NotBlank;

/** Accepts either the username or the e-mail address in {@code login}. */
public record LoginRequest(
        @NotBlank String login,
        @NotBlank String password) {
}
