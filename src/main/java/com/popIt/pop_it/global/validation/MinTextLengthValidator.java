package com.popIt.pop_it.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MinTextLengthValidator implements ConstraintValidator<MinTextLength, String> {
    private int min;

    @Override
    public void initialize(MinTextLength annotation) {
        this.min = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null은 값을 안 보낸 것이므로 필수 여부는 @NotNull이 판단
        if (value == null) {
            return true;
        }

        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                count++;
            }
        }

        return count >= min;
    }
}
