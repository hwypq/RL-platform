package org.example.rlplatform.controller;

import org.example.rlplatform.entity.ModelFile;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.ModelFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/model-files")
public class ModelFileController {

    @Autowired
    private ModelFileService modelFileService;

    @PostMapping
    public Result<Void> create(@RequestBody ModelFile modelFile) {
        modelFileService.create(modelFile);
        return Result.success();
    }

    @PostMapping("/upload")
    public Result<ModelFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") Long studentId) {
                try {
                    ModelFile modelFile = modelFileService.uploadModelFile(file, studentId);
                    return Result.success(modelFile);
                } catch (IOException e) {
                    return Result.error(e.getMessage());
                }
    }

    @GetMapping("/{id}")
    public Result<ModelFile> getById(@PathVariable Integer id) {
        return Result.success(modelFileService.getById(id));
    }

    @GetMapping
    public Result<List<ModelFile>> list(@RequestParam(required = false) Long studentId) {
        if (studentId != null) {
            return Result.success(modelFileService.listByStudentId(studentId));
        }
        return Result.success(modelFileService.list());
    }
}
