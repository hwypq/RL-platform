package org.example.rlplatform.service;

import org.example.rlplatform.entity.ModelFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ModelFileService {

    void create(ModelFile modelFile);

    ModelFile uploadModelFile(MultipartFile file, Long studentId) throws IOException;

    ModelFile getById(Integer id);

    List<ModelFile> listByStudentId(Long studentId);

    List<ModelFile> list();
}
