package com.common.storage.repository.jpa;

import com.common.storage.model.entity.jpa.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaFileRepository extends JpaRepository<File, String> {
}
