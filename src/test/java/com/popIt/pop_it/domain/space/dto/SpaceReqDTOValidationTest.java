package com.popIt.pop_it.domain.space.dto;

import com.popIt.pop_it.domain.space.enums.BuildingType;
import com.popIt.pop_it.domain.space.enums.FloorType;
import com.popIt.pop_it.domain.space.enums.RegistrantType;
import com.popIt.pop_it.domain.space.enums.SpaceCategory;
import com.popIt.pop_it.domain.space.enums.SpaceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpaceReqDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("등록: 공백만 있는 공간명은 400")
    void create_blankBuildingName() {
        assertThat(violatedFields(createReq("   ", "충분히 긴 공간 설명입니다.", "302동 302호")))
                .contains("buildingName");
    }

    @Test
    @DisplayName("등록: 공백만 있는 공간 소개/상세 주소는 400")
    void create_blankDescriptionAndAddressDetail() {
        Set<String> fields = violatedFields(createReq("합정 메세나폴리스", "  ", "\t"));
        assertThat(fields).contains("description", "addressDetail");
    }

    @Test
    @DisplayName("등록: 앞뒤 공백은 잘려서 저장된다")
    void create_trimsSurroundingWhitespace() {
        SpaceReqDTO.SpaceCreateReq request = createReq("  합정 메세나폴리스  ", "  충분히 긴 공간 설명입니다.  ", "  302동  ");

        assertThat(request.buildingName()).isEqualTo("합정 메세나폴리스");
        assertThat(request.description()).isEqualTo("충분히 긴 공간 설명입니다.");
        assertThat(request.addressDetail()).isEqualTo("302동");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("등록: 앞뒤 공백을 제거해도 20자를 넘는 공간명은 400")
    void create_lengthIsCheckedAfterTrim() {
        String twentyOne = "가".repeat(21);
        assertThat(violatedFields(createReq(" " + twentyOne + " ", "충분히 긴 공간 설명입니다.", "302동")))
                .contains("buildingName");
    }

    @Test
    @DisplayName("수정: 공백만 있는 공간명은 400 (trim 후 길이 0)")
    void update_blankBuildingName() {
        assertThat(violatedFields(updateReq("   ", null)))
                .contains("buildingName");
    }

    @Test
    @DisplayName("수정: 공백만 있는 공간 소개는 400")
    void update_blankDescription() {
        assertThat(violatedFields(updateReq(null, "     ")))
                .contains("description");
    }

    @Test
    @DisplayName("수정: 값을 생략(null)하면 검증을 통과한다")
    void update_nullFieldsAreAllowed() {
        assertThat(validator.validate(updateReq(null, null))).isEmpty();
    }

    @Test
    @DisplayName("탐색: 공백만 있는 검색어/지역은 필터 미적용(null)으로 처리된다")
    void search_blankParamsBecomeNull() {
        SpaceReqDTO.SpaceSearchReq request =
                new SpaceReqDTO.SpaceSearchReq("   ", null, "  ", null, null);

        assertThat(request.keyword()).isNull();
        assertThat(request.district()).isNull();
    }

    @Test
    @DisplayName("등록: 공백 제외 4자 미만인 공간명은 400 (@Size로는 못 잡는 케이스)")
    void create_buildingNameShorterThanFourIgnoringWhitespace() {
        // "가 나 다" = 전체 5자라 @Size(min = 4)는 통과하지만, 공백 제외 3자라 걸려야 함
        assertThat(messagesOf(createReq("가 나 다", "충분히 긴 공간 설명입니다.", "302동"), "buildingName"))
                .containsExactly("공간명은 공백을 제외하고 4자 이상이어야 합니다.");
    }

    @Test
    @DisplayName("등록: 20자를 넘는 공간명은 400")
    void create_buildingNameTooLong() {
        assertThat(violatedFields(createReq("가".repeat(21), "충분히 긴 공간 설명입니다.", "302동")))
                .contains("buildingName");
    }

    @Test
    @DisplayName("등록: 10자 미만인 공간 설명은 400")
    void create_descriptionTooShort() {
        assertThat(violatedFields(createReq("합정 메세나폴리스", "좋아요", "302동")))
                .contains("description");
    }

    @Test
    @DisplayName("수정: 공백 제외 4자 미만인 공간명은 400")
    void update_buildingNameShorterThanFour() {
        assertThat(violatedFields(updateReq("가 나", null))).contains("buildingName");
    }

    @Test
    @DisplayName("수정: 10자 미만인 공간 설명은 400")
    void update_descriptionTooShort() {
        assertThat(violatedFields(updateReq(null, "좋아요"))).contains("description");
    }

    private List<String> messagesOf(Object request, String field) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .toList();
    }

    @Test
    @DisplayName("등록: 유니코드 공백(EM SPACE)만 있는 설명은 400 (trim()으로는 못 잡는 케이스)")
    void create_descriptionWithOnlyUnicodeWhitespace() {
        // EM SPACE(U+2003) 10개. trim()은 U+0020 이하만 자르므로 길이 10으로 남아
        // @Size(min = 10)을 통과했었다. strip()은 Character.isWhitespace() 기준이라 걸러진다.
        String emSpaces = "\u2003".repeat(10);

        assertThat(violatedFields(createReq("합정 메세나폴리스", emSpaces, "302동")))
                .contains("description");
    }

    @Test
    @DisplayName("등록: 유니코드 공백은 앞뒤에서 제거된다")
    void create_stripsUnicodeWhitespace() {
        SpaceReqDTO.SpaceCreateReq request =
                createReq("\u2003합정 메세나폴리스\u2003", "\u2003충분히 긴 공간 설명입니다.\u2003", "302동");

        assertThat(request.buildingName()).isEqualTo("합정 메세나폴리스");
        assertThat(request.description()).isEqualTo("충분히 긴 공간 설명입니다.");
    }

    private Set<String> violatedFields(Object request) {
        return validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    private SpaceReqDTO.SpaceCreateReq createReq(String buildingName, String description, String addressDetail) {
        return new SpaceReqDTO.SpaceCreateReq(
                buildingName,
                RegistrantType.OWNER,
                BuildingType.LARGE_OFFICE,
                "서울특별시",
                "마포구",
                "서울특별시 마포구 합정동 130-3",
                addressDetail,
                37.5012,
                127.0397,
                4_500_000L,
                90_000,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 12, 31),
                SpaceCategory.POPUP_STORE,
                SpaceType.OPEN_HALL,
                66.0,
                FloorType.GENERAL_FLOOR,
                2,
                true,
                description,
                List.of(),
                List.of("https://example.com/1.jpg", "https://example.com/2.jpg", "https://example.com/3.jpg")
        );
    }

    private SpaceReqDTO.SpaceUpdateReq updateReq(String buildingName, String description) {
        return new SpaceReqDTO.SpaceUpdateReq(
                buildingName,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                description,
                null, null
        );
    }
}