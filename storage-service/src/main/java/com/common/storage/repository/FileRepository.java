package com.common.storage.repository;

import com.common.storage.model.entity.jpa.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface FileRepository {

    Optional<File> findById(String id);

    Page<File> findAll(Pageable pageable);

    void save(File file);

    void deleteById(String id);

    void deleteAllByIds(Iterable<String> ids);
}
