package org.example.rlplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderBoard {

    private Integer rank;
    private Integer studentId;
    private String nickname;
    private double bestScore;
}
