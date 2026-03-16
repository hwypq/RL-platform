package org.example.rlplatform.entity;

import lombok.Data;

import java.util.List;

@Data
public class ExperimentConfig {
    private String overview;
    private String rules;
    private String observationSpace;
    private String actionSpace;
    private String rewardFunction;
    private String evaluationFunction;
}
