package org.example.rlplatform.service;

import org.example.rlplatform.vo.SubmissionHistoryVO;

import java.util.List;

public interface SubmissionService {

    List<SubmissionHistoryVO> listMySubmissions();

    List<SubmissionHistoryVO> listStudentSubmissions(Integer studentId);

    List<SubmissionHistoryVO> listAssignmentSubmissions(Integer assignmentId);
}