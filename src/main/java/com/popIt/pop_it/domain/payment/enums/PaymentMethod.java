package com.popIt.pop_it.domain.payment.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {

    CARD("카드"),
    EASY_PAY("간편결제"),
    MOBILE_PHONE("휴대폰"),
    BANK_TRANSFER("계좌이체"),
    CULTURE_GIFT_CERTIFICATE("문화상품권"),
    BOOK_CULTURE_GIFT_CERTIFICATE("도서문화상품권"),
    GAME_CULTURE_GIFT_CERTIFICATE("게임문화상품권")
    ;

    private final String description;

    // 토스페이먼츠 응답의 method 문자열을 결제 수단으로 매핑
    // 매칭 실패 시 null, 토스 페이먼츠 문서의 결제 수단만 취급, 가상결제 취급X
    public static PaymentMethod fromDescription(String description) {
        PaymentMethod matched = Arrays.stream(values())
                .filter(method -> method.description.equals(description))
                .findFirst()
                .orElse(null);
        if (matched == null) {
            log.warn("매핑되지 않는 토스 결제수단: {}", description);
        }
        return matched;
    }
}
