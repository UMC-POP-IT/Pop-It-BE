package com.popIt.pop_it.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

// 공백을 제회한 글자 수의 최소값 검증
// @Size(min = 4)는 공백까지 포함해 세므로 "가 나 다"가 통과한다.
@Documented
@Constraint(validatedBy = MinTextLengthValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MinTextLength {

    int value();
    String message() default "공백을 제외하고 {value}자 이상이어야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
