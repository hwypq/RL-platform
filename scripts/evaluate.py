#!/usr/bin/env python3
"""
评测脚本：被 Java 后端调用，对指定环境+算法模型跑 N 个 episode，输出 JSON 结果到 stdout。
用法示例：
  python evaluate.py --env HalfCheetah-v2 --agent DDPG --model_name DDPG_cheetah --episodes 10 [--workspace /path/to/project]
需在 workspace 目录下存在 DDPG.py 和 models/{model_name} 模型文件。
"""
import os
os.add_dll_directory("C:\\Users\\qtq\\.mujoco\\mujoco200\\bin")
import argparse
import json
import sys
import time
import numpy as np

import gym

STEPS_PER_EPISODE = 1000


def run_one_episode(env, model):
    """跑一个 episode，返回该 episode 的累计 reward。"""
    reset_out = env.reset()
    if isinstance(reset_out, (list, tuple)):
        state = reset_out[0]
    else:
        state = reset_out
    total_reward = 0.0
    step = 0
    while True:
        action = model.predict(np.expand_dims(state, axis=0))
        step_out = env.step(action)
        if len(step_out) == 5:
            state, reward, terminated, truncated, _ = step_out
            done = terminated or truncated
        else:
            state, reward, done, _ = step_out
        total_reward += float(reward)
        step += 1
        if done or step >= STEPS_PER_EPISODE:
            break
    return total_reward


def main():
    parser = argparse.ArgumentParser(description="RL evaluation: run episodes and output JSON.")
    parser.add_argument("--env", required=True, help="Gym environment id, e.g. HalfCheetah-v2")
    parser.add_argument("--agent", required=True, help="Agent type, e.g. DDPG")
    parser.add_argument("--model_name", required=True, help="Model name (no path), e.g. DDPG_cheetah")
    parser.add_argument("--episodes", type=int, default=10, help="Number of evaluation episodes")
    parser.add_argument("--workspace", default=None, help="Project root where DDPG.py and models/ live (default: cwd)")
    args = parser.parse_args()

    workspace = os.path.abspath(args.workspace or os.getcwd())
    if args.workspace and workspace not in sys.path:
        sys.path.insert(0, workspace)
    os.chdir(workspace)

    result = {"status": "FINISHED", "avgReward": 0.0, "episodes": args.episodes, "rewards": []}
    try:
        if args.agent.upper() == "DDPG":
            from DDPG import DDPG
        else:
            result["status"] = "FAILED"
            result["error"] = f"Unsupported agent: {args.agent}"
            print(json.dumps(result, ensure_ascii=False))
            return 1

        env = gym.make(args.env, render_mode=None)
        action_space = env.action_space.shape[0]
        state_space = env.observation_space.shape[0]
        model = DDPG(state_space, action_space)
        model_path = os.path.join("models", args.model_name)
        model.load(model_path)
        env.reset()

        rewards = []
        for i in range(args.episodes):
            r = run_one_episode(env, model)
            rewards.append(r)
        env.close()

        result["rewards"] = rewards
        result["avgReward"] = sum(rewards) / len(rewards) if rewards else 0.0
    except Exception as e:
        result["status"] = "FAILED"
        result["error"] = str(e)
        # 只输出到 stdout，便于 Java 统一解析（已 redirectErrorStream(true)）
        print(json.dumps(result, ensure_ascii=False))
        return 1

    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
