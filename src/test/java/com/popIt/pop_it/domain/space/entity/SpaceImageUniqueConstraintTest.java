package com.popIt.pop_it.domain.space.entity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import com.popIt.pop_it.domain.space.repository.SpaceImageRepository;
import com.popIt.pop_it.domain.space.repository.SpaceRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("space_image 노출 순서 유니크 제약")
class SpaceImageUniqueConstraintTest {

    @Autowired
    private SpaceRepository spaceRepository;
    @Autowired
    private SpaceImageRepository spaceImageRepository;

    private Space newSpace() {
        return spaceRepository.save(Space.builder()
                .buildingName("유니크 제약 테스트 빌딩")
                .registrantType(RegistrantType.OWNER)
                .buildingType(BuildingType.GENERAL_COMMERCIAL)
                .city("서울특별시")
                .district("마포구")
                .dong("합정동")
                .latitude(37.5)
                .longitude(127.0)
                .roadAddress("테스트로 1")
                .addressDetail("101동 101호")
                .deposit(1_000_000L)
                .pricePerDay(100_000)
                .availableStartDate(LocalDate.now())
                .availableEndDate(LocalDate.now().plusYears(1))
                .spaceCategory(SpaceCategory.POPUP_STORE)
                .spaceType(SpaceType.OPEN_HALL)
                .exclusiveArea(30.0)
                .floorType(FloorType.GENERAL_FLOOR)
                .parkingAvailable(true)
                .description("유니크 제약 테스트용 공간입니다.")
                .hostId(1L)
                .build());
    }

    @Test
    void 같은_공간에_같은_노출순서_사진이_두_장이면_저장에_실패한다() {
        Space space = newSpace();

        spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(space).imageUrl("https://example.com/a.jpg").sortOrder(0).build());

        assertThatThrownBy(() -> spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(space).imageUrl("https://example.com/b.jpg").sortOrder(0).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_공간이라도_노출순서가_다르면_저장된다() {
        Space space = newSpace();

        spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(space).imageUrl("https://example.com/a.jpg").sortOrder(0).build());

        assertThatCode(() -> spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(space).imageUrl("https://example.com/b.jpg").sortOrder(1).build()))
                .doesNotThrowAnyException();
    }

    @Test
    void 다른_공간이면_같은_노출순서를_각각_가질_수_있다() {
        Space spaceA = newSpace();
        Space spaceB = newSpace();

        spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(spaceA).imageUrl("https://example.com/a.jpg").sortOrder(0).build());

        assertThatCode(() -> spaceImageRepository.saveAndFlush(SpaceImage.builder()
                .space(spaceB).imageUrl("https://example.com/b.jpg").sortOrder(0).build()))
                .doesNotThrowAnyException();
    }
}