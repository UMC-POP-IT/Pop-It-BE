-- 같은 scene_id 안에서 sort_order가 중복되지 못하게 DB 레벨에서 막음
-- (SceneCommandService의 findByIdForUpdate 비관적 락이 애플리케이션 레벨 직렬화를 담당하지만,
--  락 우회 경로나 실수로 인한 중복까지 막는 최종 안전망)

ALTER TABLE scene_image
    ADD CONSTRAINT uq_scene_image_scene_id_sort_order UNIQUE (scene_id, sort_order);

-- 롤백:
-- ALTER TABLE scene_image DROP INDEX uq_scene_image_scene_id_sort_order;
