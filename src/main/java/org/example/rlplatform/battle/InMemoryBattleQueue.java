package org.example.rlplatform.battle;

import org.example.rlplatform.entity.Submission;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class InMemoryBattleQueue {

    /**
     * 按 assignmentId 分队列，防止不同任务但相同 environment 的学生被错误配对
     */
    private final ConcurrentHashMap<Integer, Deque<Submission>> queueMap = new ConcurrentHashMap<>();

    public int enqueue(Submission submission) {
        Deque<Submission> queue = queueMap.computeIfAbsent(
                submission.getAssignmentId(),
                k -> new ConcurrentLinkedDeque<>()
        );
        synchronized (queue) {
            queue.addLast(submission);
            return queue.size();
        }
    }

    public Submission[] tryDequeuePair(Integer assignmentId) {
        Deque<Submission> queue = queueMap.computeIfAbsent(assignmentId, k -> new ConcurrentLinkedDeque<>());
        synchronized (queue) {
            if (queue.size() < 2) {
                return null;
            }
            Submission first = queue.pollFirst();
            Submission second = queue.pollFirst();
            if (first == null || second == null) {
                return null;
            }
            return new Submission[]{first, second};
        }
    }
}