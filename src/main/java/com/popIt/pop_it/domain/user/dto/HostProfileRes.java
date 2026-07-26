package com.popIt.pop_it.domain.user.dto;

import com.popIt.pop_it.domain.user.entity.enums.Bank;
import com.popIt.pop_it.domain.user.entity.enums.TaxationType;

import java.time.LocalDateTime;

public record HostProfileRes(
        Long id,
        TaxationType taxationType,
        String businessRegistrationNumber,
        String businessLicenseUrl,
        String businessName,
        String businessAddress,
        String bankbookCopyUrl,
        Bank bank,
        String settlementAccountNumber,
        String accountHolder,
        LocalDateTime createdAt,
        Long userId
) {}
