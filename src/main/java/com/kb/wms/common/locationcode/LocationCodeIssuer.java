package com.kb.wms.common.locationcode;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 창고·지점 코드 발급: {@code WH-SU-001}, {@code ST-BS-002} (접두어-시·도-일련번호 3자리).
 * 일련번호는 종류+시·도별 채번 행을 잠그고 올리므로 동시 등록에서도 겹치지 않는다. 호출자의 트랜잭션 안에서 실행한다.
 */
@Component
@RequiredArgsConstructor
public class LocationCodeIssuer {

    private static final Pattern ISSUED_CODE = Pattern.compile("^(WH|ST)-([A-Z]{2})-\\d{3,}$");

    private final LocationCodeSequenceRepository sequenceRepository;

    /**
     * @param exists 이미 같은 코드가 있는지 확인한다. 과거에 직접 입력된 코드와 겹치면 다음 번호로 넘어간다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String issue(LocationType type, RegionCode region, Predicate<String> exists) {
        String sequenceKey = type.getPrefix() + "-" + region.name();
        LocationCodeSequence sequence = sequenceRepository.findByKeyForUpdate(sequenceKey)
                .orElseGet(() -> sequenceRepository.saveAndFlush(new LocationCodeSequence(sequenceKey)));
        String code;
        do {
            code = String.format("%s-%03d", sequenceKey, sequence.next());
        } while (exists.test(code));
        return code;
    }

    /** 발급 형식의 코드면 그 시·도를 돌려준다. 과거에 직접 입력한 코드는 비어 있다. */
    public static Optional<RegionCode> regionOf(String code) {
        if (code == null) {
            return Optional.empty();
        }
        Matcher matcher = ISSUED_CODE.matcher(code);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(RegionCode.valueOf(matcher.group(2)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
