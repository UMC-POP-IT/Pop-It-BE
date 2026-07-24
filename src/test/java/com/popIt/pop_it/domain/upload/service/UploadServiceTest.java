package com.popIt.pop_it.domain.upload.service;

import com.popIt.pop_it.domain.upload.dto.UploadReqDTO;
import com.popIt.pop_it.domain.upload.dto.UploadResDTO;
import com.popIt.pop_it.domain.upload.enums.UploadType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UploadServiceTest {

    @Autowired
    UploadService uploadService;

    private UploadReqDTO.PresignedUrlReq request(UploadType type) {
        return new UploadReqDTO.PresignedUrlReq(
                type,
                List.of(new UploadReqDTO.FileInfoReq("image/png"))
        );
    }

    private static final Long USER_ID = 42L;

    @Test
    @DisplayName("호스트 서류는 민감서류 전용 버킷의 유저별 경로로 presigned URL이 발급된다")
    void hostDocument_routesToHostBucket() {
        UploadResDTO.PresignedUrlListRes result = uploadService.issuePresignedUrls(USER_ID, request(UploadType.HOST_DOCUMENT));

        UploadResDTO.PresignedUrlInfoRes info = result.uploads().get(0);
        // test/resources/application.yml의 host-document-bucket = test-host-document-bucket
        assertThat(info.fileUrl()).contains("test-host-document-bucket");
        // 유저별 경로: host-document/{userId}/...
        assertThat(info.fileUrl()).contains("/host-document/" + USER_ID + "/");
        assertThat(info.presignedUrl()).contains("test-host-document-bucket");
    }

    @Test
    @DisplayName("공간 이미지는 일반 버킷의 유저별 경로로 presigned URL이 발급된다")
    void spaceImage_routesToGeneralBucket() {
        UploadResDTO.PresignedUrlListRes result = uploadService.issuePresignedUrls(USER_ID, request(UploadType.SPACE_IMAGE));

        UploadResDTO.PresignedUrlInfoRes info = result.uploads().get(0);
        // test/resources/application.yml의 bucket = test-bucket
        assertThat(info.fileUrl()).contains("test-bucket");
        assertThat(info.fileUrl()).contains("/space/" + USER_ID + "/");
    }
}
