package org.example.rlplatform.service;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.vo.SystemBotStatusVO;
import org.springframework.web.multipart.MultipartFile;

public interface SystemBotService {

    Result<?> uploadBotFiles(Integer assignmentId, String difficulty, MultipartFile config, MultipartFile model);

    SystemBotStatusVO getBotStatus(Integer assignmentId);

    String getBotAbsoluteDir(Integer assignmentId, String difficulty);
}