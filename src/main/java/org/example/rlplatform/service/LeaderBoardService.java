package org.example.rlplatform.service;

import org.example.rlplatform.entity.LeaderBoard;
import org.springframework.data.domain.Page;

public interface LeaderBoardService {

    Page<LeaderBoard> list(Integer assignmentId, Integer pageNum, Integer pageSize);
}
