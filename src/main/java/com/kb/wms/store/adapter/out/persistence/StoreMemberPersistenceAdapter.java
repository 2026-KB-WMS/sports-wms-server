package com.kb.wms.store.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.kb.wms.store.adapter.out.persistence.entity.StoreMemberJpaEntity;
import com.kb.wms.store.adapter.out.persistence.repository.StoreMemberJpaRepository;
import com.kb.wms.store.application.port.out.StoreMemberRepository;
import com.kb.wms.store.domain.entity.StoreMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StoreMemberPersistenceAdapter implements StoreMemberRepository {

    private final StoreMemberJpaRepository storeMemberJpaRepository;

    @Override
    public StoreMember save(StoreMember member) {
        StoreMemberJpaEntity saved = storeMemberJpaRepository.save(StoreMemberJpaEntity.fromDomain(member));
        return saved.toDomain();
    }

    @Override
    public Optional<StoreMember> findById(Long storeMemberId) {
        return storeMemberJpaRepository.findById(storeMemberId).map(StoreMemberJpaEntity::toDomain);
    }

    @Override
    public List<StoreMember> findAll(Long storeId, Long userId) {
        return storeMemberJpaRepository.findAllByFilter(storeId, userId).stream()
                .map(StoreMemberJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<StoreMember> findByUserId(Long userId) {
        return storeMemberJpaRepository.findByUserId(userId).stream()
                .map(StoreMemberJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByStoreIdAndUserId(Long storeId, Long userId) {
        return storeMemberJpaRepository.existsByStoreIdAndUserId(storeId, userId);
    }

    @Override
    public void deleteById(Long storeMemberId) {
        storeMemberJpaRepository.deleteById(storeMemberId);
    }
}
