package com.popIt.pop_it.domain.user_agreement.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AgreementResDTO {

    public record SaveResult(
            List<AgreedItem> agreements
    ) {}

    public record AgreedItem(
            Long termId,
            boolean isAgreed,
            LocalDateTime agreedAt
    ) {}
}
