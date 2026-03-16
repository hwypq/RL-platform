package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.LeaderBoardRow;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.LeaderBoard;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.service.LeaderBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaderBoardServiceImpl implements LeaderBoardService {

    @Autowired
    private EvaluationRepository leaderBoardRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Page<LeaderBoard> list(Integer assignmentId, Integer pageNum, Integer pageSize) {

        Pageable pageable = PageRequest.of(pageNum, pageSize);

        int offset = pageNum * pageSize;

        List<LeaderBoardRow> rows =
                leaderBoardRepository.leaderboardByAssignmentId(assignmentId, pageSize, offset);

        System.out.println("rows: " + rows);

        // 批量查用户昵称
        List<Integer> studentIds = rows.stream()
                .map(LeaderBoardRow::getStudentId)
                .toList();
        Map<Integer, User> userMap = userRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u));

        List<LeaderBoard> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            LeaderBoardRow row = rows.get(i);
            int rank = offset + i + 1;
            User u = userMap.get(row.getStudentId());
            String nickname = (u != null) ? u.getNickname() : null;
            Double bs = (row.getBestScore() != null) ? row.getBestScore() : -Double.MAX_VALUE;
            entries.add(new LeaderBoard(
                    rank,
                    row.getStudentId(),
                    nickname,
                    bs
            ));
        }

        // 查总人数，用于分页 total
        long total = leaderBoardRepository.countLeaderboardByAssignment(assignmentId);
        return new PageImpl<>(entries, pageable, total);
    }
}
