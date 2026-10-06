package com.kb.wms.auth.adapter.out.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kb.wms.auth.adapter.out.persistence.entity.UserJpaEntity;
import com.kb.wms.auth.adapter.out.persistence.repository.UserQueryJpaRepository;
import com.kb.wms.auth.application.port.in.result.UserAffiliation;
import com.kb.wms.auth.application.port.out.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserQueryPersistenceAdapter implements UserQueryRepository {

    private final UserQueryJpaRepository userQueryJpaRepository;

    @Override
    public UserAffiliation findAffiliation(Long userId) {
        return new UserAffiliation(
                userQueryJpaRepository.findWarehouseIds(userId),
                userQueryJpaRepository.findStoreIds(userId));
    }

    @Override
    public Map<Long, String> findNamesByIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userQueryJpaRepository.findAllByIds(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getUserId, UserJpaEntity::getName,
                        (first, second) -> first));
    }
}
