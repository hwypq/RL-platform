package org.example.rlplatform.service;

import org.example.rlplatform.entity.ModelFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ModelFileService {

    void create(ModelFile modelFile);

    ModelFile uploadModelFile(MultipartFile file, Integer studentId) throws IOException;

    ModelFile getById(Long id);

    List<ModelFile> listByStudentId(Integer studentId);

    List<ModelFile> list();
}
