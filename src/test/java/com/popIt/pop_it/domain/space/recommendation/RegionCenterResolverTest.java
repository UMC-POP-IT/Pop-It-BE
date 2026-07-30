package com.popIt.pop_it.domain.space.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;

import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.recommendation.RegionCenterResolver.RegionCenter;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionCenterResolverTest {

    @Mock
    private SpaceRepository spaceRepository;

    @InjectMocks
    private RegionCenterResolver regionCenterResolver;

    @Test
    void 지역_내_공간들의_좌표_평균을_중심좌표로_반환한다() {
        given(spaceRepository.findAllByDongAndDeletedAtIsNull("강남구")).willReturn(List.of(
                Space.builder().latitude(37.50).longitude(127.02).build(),
                Space.builder().latitude(37.52).longitude(127.04).build()
        ));

        Optional<RegionCenter> center = regionCenterResolver.resolveCenter("강남구");

        assertThat(center).isPresent();
        assertThat(center.get().latitude()).isCloseTo(37.51, within(0.0001));
        assertThat(center.get().longitude()).isCloseTo(127.03, within(0.0001));
    }

    @Test
    void 해당_지역에_공간이_없으면_빈_값을_반환한다() {
        given(spaceRepository.findAllByDongAndDeletedAtIsNull("무명동")).willReturn(List.of());

        Optional<RegionCenter> center = regionCenterResolver.resolveCenter("무명동");

        assertThat(center).isEmpty();
    }

    @Test
    void 지역명이_없으면_빈_값을_반환한다() {
        assertThat(regionCenterResolver.resolveCenter(null)).isEmpty();
        assertThat(regionCenterResolver.resolveCenter(" ")).isEmpty();
    }
}
