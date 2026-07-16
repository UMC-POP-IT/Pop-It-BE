package com.popIt.pop_it.domain.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 호스트에게 임대료를 지급하는 외부 지급대행사 연동 클라이언트.
 */
@Slf4j
@Component
public class HostPayoutClient {

    public void payout(String idempotencyKey, Long hostUserId, Long amount) {
        // @TODO: 실제 지급대행사 API 연동
        log.info("[호스트 지급 - 미연동] idempotencyKey={}, hostUserId={}, amount={}", idempotencyKey, hostUserId, amount);
    }
}
