package com.kb.wms.common.locationcode;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface LocationCodeSequenceRepository extends JpaRepository<LocationCodeSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from LocationCodeSequence s where s.sequenceKey = :sequenceKey")
    Optional<LocationCodeSequence> findByKeyForUpdate(@Param("sequenceKey") String sequenceKey);
}
