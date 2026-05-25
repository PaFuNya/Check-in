package org.example.repository;

import org.example.entity.BaseEntity;
import org.springframework.data.repository.CrudRepository;

public interface BaseRepository<T extends BaseEntity> extends CrudRepository<T, Long> {
}
