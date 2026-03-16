package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws IOException {

        String originalfileName = file.getOriginalFilename();
        String suffix = originalfileName.substring(originalfileName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;
        file.transferTo(new File("E:\\2025下\\毕设\\test" + fileName));
        return Result.success(fileName);
    }
}
