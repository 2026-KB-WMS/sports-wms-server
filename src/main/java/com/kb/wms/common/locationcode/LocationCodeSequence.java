package com.kb.wms.common.locationcode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 종류+시·도별 마지막 발급 번호. 코드가 다른 시·도로 재발급되어도 번호가 되돌아가지 않도록
 * 기존 코드 개수가 아니라 이 값으로 채번한다.
 */
@Entity
@Table(name = "location_code_sequence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationCodeSequence {

    @Id
    @Column(name = "sequence_key", length = 20)
    private String sequenceKey;

    @Column(name = "last_no", nullable = false)
    private long lastNo;

    LocationCodeSequence(String sequenceKey) {
        this.sequenceKey = sequenceKey;
        this.lastNo = 0;
    }

    long next() {
        this.lastNo++;
        return this.lastNo;
    }
}
