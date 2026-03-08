package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.StudentClassRepository;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.service.StudentClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

import static java.time.LocalDateTime.now;

@Service
public class StudentClassImpl implements StudentClassService {

    @Autowired
    private StudentClassRepository studentClassRepository;

    @Override
    public StudentClass findByName(String name) {
        return studentClassRepository.findByName(name);
    }

    @Override
    public StudentClass findByIdAndIsDeletedFalse(Integer id) {
        return getByIdAndNotDeleted(id);
    }

    @Override
    public void create(StudentClass studentClass) {
        studentClass.setIsDeleted(false);
        studentClass.setCreateTime(now());
        studentClassRepository.save(studentClass);
    }

    @Override
    public void update(StudentClass studentClass, Integer id) {
        StudentClass sc = getByIdAndNotDeleted(id);
        sc.setName(studentClass.getName());
        studentClassRepository.save(sc);
    }

    @Override
    public void softDelete(Integer id) {
        StudentClass sc = getByIdAndNotDeleted(id);
        sc.setIsDeleted(true);
        studentClassRepository.save(sc);
    }

    @Override
    public Page<StudentClass> listPage(Integer pageNum, Integer pageSize, Boolean isDeleted) {
        Specification<StudentClass> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isDeleted != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDeleted"), isDeleted));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return studentClassRepository.findAll(spec, PageRequest.of(pageNum, pageSize));
    }

    private StudentClass getByIdAndNotDeleted(Integer id) {
        StudentClass sc = studentClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("班级不存在"));
        if (sc.getIsDeleted()) {
            throw new RuntimeException("班级已删除");
        }
        return sc;
    }

}
