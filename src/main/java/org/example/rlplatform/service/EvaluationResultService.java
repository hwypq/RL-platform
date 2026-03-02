package org.example.rlplatform.service;

import org.example.rlplatform.entity.EvaluationResult;
import org.springframework.core.io.Resource;

import java.util.List;

public interface EvaluationResultService {

    void create(EvaluationResult evaluationResult);

    EvaluationResult getById(Long id);

    List<EvaluationResult> listByEvaluationId(Long evaluationId);

    List<EvaluationResult> list();

    /** 返回该评测结果目录下第一个视频文件（video_0.mp4），供前端直接播放；无 resultDir 或文件不存在时抛异常 */
    Resource getVideo(Long id);
}
