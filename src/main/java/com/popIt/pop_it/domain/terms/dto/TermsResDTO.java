package com.popIt.pop_it.domain.terms.dto;

import com.popIt.pop_it.domain.terms.entity.enums.TermCode;

import java.util.List;

public class TermsResDTO {

    public record ListResult(
            List<Item> terms
    ) {}

    public record Item(
            Long id,
            String title,
            String content,
            TermCode code,
            boolean required
    ) {}
}
