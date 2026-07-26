package com.popIt.pop_it.domain.space.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.popIt.pop_it.domain.facility.repository.FacilityRepository;
import com.popIt.pop_it.domain.space.dto.SpaceReqDTO;
import com.popIt.pop_it.domain.space.entity.Space;
import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceFacilityRepository;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import com.popIt.pop_it.domain.user.repository.HostProfileRepository;
import com.popIt.pop_it.domain.wishlist.repository.WishlistRepository;
import com.popIt.pop_it.global.embedding.event.SpaceCreatedEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private SpaceImageRepository spaceImageRepository;
    @Mock
    private SpaceFacilityRepository spaceFacilityRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private HostProfileRepository hostProfileRepository;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private KakaoLocalService kakaoLocalService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SpaceService spaceService;

    @Test
    void 공간_등록_성공_시_임베딩_생성은_직접_호출하지_않고_커밋_후_이벤트로만_위임한다() {
        Long userId = 1L;
        Long savedSpaceId = 100L;
        Space savedSpace = Space.builder().id(savedSpaceId).build();

        given(hostProfileRepository.existsByUserId(userId)).willReturn(true);
        given(kakaoLocalService.resolveDong(any(), any())).willReturn(Optional.of("합정동"));
        given(spaceRepository.save(any(Space.class))).willReturn(savedSpace);

        spaceService.createSpace(userId, createRequest());

        // 이 트랜잭션이 커밋되기 전에 별도 트랜잭션에서 embedding을 생성하면 SPACE_NOT_FOUND가 나므로
        // 여기서 SpaceEmbeddingService를 직접 호출하지 않고 이벤트만 발행해야 한다 (AFTER_COMMIT 리스너가 처리)
        ArgumentCaptor<SpaceCreatedEvent> captor = ArgumentCaptor.forClass(SpaceCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().spaceId()).isEqualTo(savedSpaceId);
    }

    private SpaceReqDTO.SpaceCreateReq createRequest() {
        return new SpaceReqDTO.SpaceCreateReq(
                "테스트 빌딩",
                RegistrantType.OWNER,
                BuildingType.LARGE_OFFICE,
                "서울특별시",
                "마포구",
                "서울특별시 마포구 합정동 130-3",
                "302동 302호",
                37.5,
                127.0,
                1_000_000L,
                90_000,
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                SpaceCategory.POPUP_STORE,
                SpaceType.OPEN_HALL,
                66.0,
                FloorType.GENERAL_FLOOR,
                2,
                true,
                "테스트용 공간 설명입니다.",
                null,
                List.of("https://example.com/1.jpg", "https://example.com/2.jpg", "https://example.com/3.jpg")
        );
    }
}
