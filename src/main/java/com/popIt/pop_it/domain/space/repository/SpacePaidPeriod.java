package com.popIt.pop_it.domain.space.repository;

import java.time.LocalDate;

public interface SpacePaidPeriod {
    Long getSpaceId();
    LocalDate getStartDate();
    LocalDate getEndDate();
}
