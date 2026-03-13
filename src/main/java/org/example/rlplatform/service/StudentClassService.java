package org.example.rlplatform.service;

import org.springframework.data.domain.Page;
import org.example.rlplatform.entity.StudentClass;

public interface StudentClassService {

    void create(StudentClass studentClass);

    void update(StudentClass studentClass, Integer id);

    void softDelete(Integer id);

    Page<StudentClass> listPage(Integer pageNum, Integer pageSize, Boolean  isDeleted);

    StudentClass findByName(String name);

    StudentClass findByIdAndIsDeletedFalse(Integer id);

    StudentClass findByCodeAndIsDeletedFalse(String code);
}
