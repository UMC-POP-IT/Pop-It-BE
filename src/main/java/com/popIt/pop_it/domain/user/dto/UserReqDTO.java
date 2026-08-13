package com.popIt.pop_it.domain.user.dto;

import com.popIt.pop_it.domain.user.entity.enums.UserMode;
import jakarta.validation.constraints.NotNull;

public class UserReqDTO {

    public record ModeSwitchReq(
            @NotNull UserMode mode
    ) {}
}
