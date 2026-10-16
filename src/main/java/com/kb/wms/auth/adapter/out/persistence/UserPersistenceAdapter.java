package com.kb.wms.auth.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.auth.adapter.out.persistence.entity.UserJpaEntity;
import com.kb.wms.auth.adapter.out.persistence.repository.UserJpaRepository;
import com.kb.wms.auth.application.port.in.query.UserSearchCondition;
import com.kb.wms.auth.application.port.out.UserRepository;
import com.kb.wms.auth.domain.entity.User;
import com.kb.wms.common.paging.PageCondition;
import com.kb.wms.common.paging.PageResult;
import com.kb.wms.common.persistence.PageAdapter;
import com.kb.wms.common.persistence.SearchKeyword;
import com.kb.wms.common.security.UserRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        // flush해야 @LastModifiedDate가 채워져, 응답에 담는 updatedAt이 이번 수정 시각이 된다.
        UserJpaEntity saved = userJpaRepository.saveAndFlush(UserJpaEntity.fromDomain(user));
        return saved.toDomain();
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long userId) {
        return userJpaRepository.findByIdForUpdate(userId).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByLoginId(String loginId) {
        return userJpaRepository.findByLoginId(loginId).map(UserJpaEntity::toDomain);
    }

    @Override
    public PageResult<User> search(UserSearchCondition condition, PageCondition page) {
        return PageAdapter.toPageResult(userJpaRepository.search(
                        condition.role(),
                        condition.status(),
                        SearchKeyword.normalize(condition.keyword()),
                        PageAdapter.toPageable(page)),
                page, UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return userJpaRepository.existsByLoginId(loginId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndUserIdNot(String email, Long userId) {
        return userJpaRepository.existsByEmailAndUserIdNot(email, userId);
    }

    @Override
    public boolean existsByRole(UserRole role) {
        return userJpaRepository.existsByRole(role);
    }
}
