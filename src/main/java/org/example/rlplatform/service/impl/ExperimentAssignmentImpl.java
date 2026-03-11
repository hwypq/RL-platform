package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.StudentClassService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ExperimentAssignmentImpl implements ExperimentAssignmentService {

    @Autowired
    private StudentClassService studentClassService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExperimentAssignmentRepository experimentAssignmentRepository;

    @Override
    public void create(Integer classId, ExperimentAssignment experimentAssignment) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        experimentAssignment.setTeacherId(userId);
        experimentAssignment.setStudentClass(studentClassService.findByIdAndIsDeletedFalse(classId));
        experimentAssignment.setCreateTime(LocalDateTime.now());
        experimentAssignment.setUpdateTime(LocalDateTime.now());
        experimentAssignment.setIsDeleted(false);
        experimentAssignmentRepository.save(experimentAssignment);
    }

    @Override
    public Page<ExperimentAssignment> listStuAssignments(Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        User me = userService.findByIdAndIsDeletedFalse(userId);
        StudentClass sc = me.getStudentClass();
        if (sc == null) {
            throw new RuntimeException("您还未选择班级");
        }
        if (sc.getIsDeleted()) {
            throw new RuntimeException("班级已删除");
        }
        return experimentAssignmentRepository.findByStudentClass_IdAndIsDeletedFalse(sc.getId(), PageRequest.of(pageNum, pageSize));
    }

    @Override
    public Page<ExperimentAssignment> listTeaAssignments(Integer pageNum, Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        User me = userService.findByIdAndIsDeletedFalse(userId);
        if (me.getRole() != UserRole.TEACHER) {
            throw new RuntimeException("您不是教师");
        }
        return experimentAssignmentRepository.findByTeacherIdAndIsDeletedFalse(userId, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public Page<ExperimentAssignment> listAssignmentsByClass(Integer classId, Integer pageNum, Integer pageSize) {
        return experimentAssignmentRepository.findByStudentClass_IdAndIsDeletedFalse(classId, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public ExperimentAssignment getById(Integer assignmentId) {
        return experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
    }
}
