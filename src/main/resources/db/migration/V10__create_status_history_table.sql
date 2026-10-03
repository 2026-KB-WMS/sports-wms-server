-- 공통 상태 이력 테이블: StatusHistory
-- 기준: ERD 데이터 사전 https://app.notion.com/p/3dfd0c80e04a81eaa6b9c3a78fad349a
-- entity_id는 entity_type에 따라 다른 테이블을 가리키는 다형 참조이므로 FK를 걸 수 없다.
-- changed_by(User, 회원 도메인)는 아직 테이블이 없어 FK 없이 인덱스만 둔다.
-- User 테이블 생성 마이그레이션에서 ALTER TABLE로 FK를 추가한다.

CREATE TABLE status_history (
    status_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type       VARCHAR(30)  NOT NULL,
    entity_id         BIGINT       NOT NULL,
    from_status       VARCHAR(30)  NULL,
    to_status         VARCHAR(30)  NOT NULL,
    reason            VARCHAR(500) NULL,
    changed_by        BIGINT       NULL,
    changed_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 엔티티별 이력 시간순 조회, 현재 상태로 바뀔 때의 사유 조회
CREATE INDEX idx_status_history_entity ON status_history (entity_type, entity_id, changed_at);
CREATE INDEX idx_status_history_changed_by ON status_history (changed_by);
