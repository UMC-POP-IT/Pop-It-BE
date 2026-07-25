package com.popIt.pop_it.domain.wishlist.exception.code;

import com.popIt.pop_it.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WishlistSuccessCode implements BaseSuccessCode {

    WISH_ADDED(HttpStatus.OK, "WISH200_1", "찜 등록에 성공했습니다."),
    WISH_REMOVED(HttpStatus.OK, "WISH200_2", "찜 해제에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
