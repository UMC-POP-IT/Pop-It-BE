package com.popIt.pop_it.domain.upload.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadType {

    // 공간 이미지: 일반 버킷 (비민감)
    SPACE_IMAGE("space", BucketType.GENERAL),

    // 호스트 서류(통장 사본, 사업자등록증): 민감서류 전용 프라이빗 버킷
    HOST_DOCUMENT("host-document", BucketType.HOST_DOCUMENT),

    // 계약 전자서명 이미지: 일반 버킷
    CONTRACT_SIGNATURE("signature", BucketType.GENERAL),

    // 3D 큐레이션 씬(방) 사진: 일반 버킷
    SCENE_IMAGE("scene", BucketType.GENERAL),

    // 퇴실 증빙 사진: 일반 버킷
    CHECKOUT_IMAGE("checkout", BucketType.GENERAL);

    private final String path;
    private final BucketType bucketType;

    public enum BucketType {
        GENERAL,
        HOST_DOCUMENT
    }
}
