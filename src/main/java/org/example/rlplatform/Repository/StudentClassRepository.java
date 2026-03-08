package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StudentClassRepository extends JpaRepository<StudentClass, Integer>, JpaSpecificationExecutor<StudentClass> {

    StudentClass findByName(String name);

    StudentClass findByIdAndIsDeletedFalse(Integer id);
}
