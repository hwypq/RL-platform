package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.ModelFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelFileRepository extends JpaRepository<ModelFile, Integer> {

    List<ModelFile> findByStudentId(Long studentId);
}
