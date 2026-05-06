package com.nemonichorse.repository;

import com.nemonichorse.model.HorseData;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HorseRepository {

    void initialize() throws Exception;

    void close();

    Optional<HorseData> findById(UUID horseId);

    List<HorseData> findByOwner(UUID ownerId);

    void save(HorseData data);

    void saveBatch(Collection<HorseData> dataList);

    void delete(UUID horseId);
}
