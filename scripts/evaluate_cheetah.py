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
from datetime import datetime
import numpy as np

import gym

STEPS_PER_EPISODE = 1000


def run_one_episode(env, model):
    """跑一个 episode，返回该 episode 的累计 reward"""

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
    parser.add_argument("--realtime_render", action="store_true", help="Render in realtime")
    parser.add_argument("--render_video", action="store_true", help="Render video")

    args = parser.parse_args()

    RENDER_VIDEO = args.render_video
    REALTIME_RENDER = args.realtime_render
    AGENT = args.agent
    MODEL_NAME = args.model_name
    ENV = args.env
    EPISODES = args.episodes
    WORKSPACE = args.workspace

    workspace = os.path.abspath(WORKSPACE or os.getcwd())
    if WORKSPACE and workspace not in sys.path:
        sys.path.insert(0, workspace)
    os.chdir(workspace)

    result = {"status": "FINISHED", "avgReward": 0.0, "episodes": EPISODES, "rewards": []}
    try:
        if AGENT.upper() == "DDPG":
            from DDPG import DDPG
        else:
            result["status"] = "FAILED"
            result["error"] = f"Unsupported agent: {AGENT}"
            print(json.dumps(result, ensure_ascii=False))
            return 1

        if REALTIME_RENDER:
            env = gym.make(ENV, render_mode="human")
        elif RENDER_VIDEO:
            env = gym.make(ENV, render_mode="rgb_array")
            # 使用唯一子目录，避免覆盖
            run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
            base_name = os.path.splitext(MODEL_NAME)[0]
            name_prefix = f"{base_name}_{run_id}"
            os.makedirs("videos", exist_ok=True)
            env = gym.wrappers.RecordVideo(
                env,
                video_folder="videos",
                episode_trigger=lambda eid: eid == 0,  # 先只录制第一个 episode
                name_prefix=name_prefix,
            )
        else:
            env = gym.make(ENV, render_mode=None)

        action_space = env.action_space.shape[0]
        state_space = env.observation_space.shape[0]
        model = DDPG(state_space, action_space)
        model_path = os.path.join("models", MODEL_NAME)
        model.load(model_path)
        env.reset()

        rewards = []
        for i in range(args.episodes):
            r = run_one_episode(env, model)
            rewards.append(r)
        env.close()

        result["rewards"] = rewards
        result["result_dir"] = "videos/" + name_prefix + "-episode-0" if RENDER_VIDEO else None
        result["avgReward"] = sum(rewards) / len(rewards) if rewards else 0.0
    except Exception as e:
        result["status"] = "FAILED"
        result["error"] = str(e)
        # 只输出到 stdout，便于 Java 统一解析
        print(json.dumps(result, ensure_ascii=False))
        return 1

    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
