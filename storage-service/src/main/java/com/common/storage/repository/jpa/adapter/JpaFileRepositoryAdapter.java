package com.common.storage.repository.jpa.adapter;

import com.common.storage.model.entity.jpa.File;
import com.common.storage.repository.FileRepository;
import com.common.storage.repository.jpa.JpaFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class JpaFileRepositoryAdapter implements FileRepository {

    private final JpaFileRepository jpaFileRepository;

    @Override
    public Optional<File> findById(String id) {
        return this.jpaFileRepository.findById(id);
    }

    @Override
    public Page<File> findAll(Pageable pageable) {
        return this.jpaFileRepository.findAll(pageable);
    }

    @Override
    public void save(File file) {
        this.jpaFileRepository.save(file);
    }

    @Override
    public void deleteById(String id) {
        this.jpaFileRepository.deleteById(id);
    }

    @Override
    public void deleteAllByIds(Iterable<String> ids) {
        this.jpaFileRepository.deleteAllById((Iterable<String>) ids);
    }
}
