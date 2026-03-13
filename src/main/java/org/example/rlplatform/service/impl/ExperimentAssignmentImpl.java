package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.ExperimentConfig;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.StudentClassService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ExperimentAssignmentImpl implements ExperimentAssignmentService {

    @Autowired
    private StudentClassService studentClassService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExperimentAssignmentRepository experimentAssignmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void create(Integer classId, ExperimentAssignment experimentAssignment) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        experimentAssignment.setTeacherId(userId);
        experimentAssignment.setStudentClass(studentClassService.findByIdAndIsDeletedFalse(classId));
        experimentAssignment.setCreateTime(LocalDateTime.now());
        experimentAssignment.setUpdateTime(LocalDateTime.now());
        experimentAssignment.setIsDeleted(false);

        ExperimentConfig config = experimentAssignment.getConfig();
        if (config != null) {
            try {
                experimentAssignment.setConfigJson(objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("实验配置序列化失败", e);
            }
        }

        experimentAssignmentRepository.save(experimentAssignment);
    }

    @Override
    public void update(Integer assignmentId, ExperimentAssignment experimentAssignment) {
        ExperimentAssignment dbassignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        System.out.println(dbassignment);
        if (dbassignment == null) {
            throw new RuntimeException("实验不存在");
        }
        dbassignment.setTitle(experimentAssignment.getTitle());
        dbassignment.setConfig(experimentAssignment.getConfig());
        dbassignment.setEvaluationMode(experimentAssignment.getEvaluationMode());
        dbassignment.setAgentName(experimentAssignment.getAgentName());
        dbassignment.setEnvironment(experimentAssignment.getEnvironment());
        dbassignment.setDeadline(experimentAssignment.getDeadline());
        dbassignment.setUpdateTime(LocalDateTime.now());
        dbassignment.setIsDeleted(false);

        ExperimentConfig config = dbassignment.getConfig();
        if (config != null) {
            try {
                dbassignment.setConfigJson(objectMapper.writeValueAsString(config));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("实验配置序列化失败", e);
            }
        }

        experimentAssignmentRepository.save(dbassignment);
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
        return experimentAssignmentRepository.findByTeacherIdAndIsDeletedFalse(userId, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public Page<ExperimentAssignment> listAssignmentsByClass(Integer classId, Integer pageNum, Integer pageSize) {
        return experimentAssignmentRepository.findByStudentClass_IdAndIsDeletedFalse(classId, PageRequest.of(pageNum, pageSize));
    }

    @Override
    public ExperimentAssignment getById(Integer assignmentId) {
        ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
        if (assignment == null) {
            return null;
        }
        String configJson = assignment.getConfigJson();
        if (configJson != null && !configJson.isBlank()) {
            try {
                ExperimentConfig config = objectMapper.readValue(configJson, ExperimentConfig.class);
                assignment.setConfig(config);
            } catch (JsonProcessingException e) {
                // 配置解析失败时暂时忽略，保留原始 JSON
            }
        }
        return assignment;
    }
}
