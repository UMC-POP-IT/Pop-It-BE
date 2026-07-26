package com.popIt.pop_it.domain.user.dto;

import java.time.LocalDateTime;

public record HostRegisterRes(
        Long id,
        LocalDateTime createdAt
) {}
