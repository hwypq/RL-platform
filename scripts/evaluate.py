#!/usr/bin/env python3
"""
统一评测脚本：
- 支持 Mujoco HalfCheetah (DDPG)
- 支持 LunarLander (DQN / PPO / PPO-GAE)
- 统一输出 JSON 到 stdout，便于后端调用。
"""

import os
import sys
import argparse
import json
from datetime import datetime

import numpy as np

# 优先使用 gymnasium，如不可用再退回 gym
try:
    import gymnasium as gym
except ImportError:  # pragma: no cover - 运行环境中若无 gymnasium 则退回 gym
    import gym

import torch

from agents.dqn_agent import DQNAgent
from agents.ppo_agent import PPOAgent
from agents.ppo_gae_agent import PPO_GAE_Agent
from agents.ddpg_agent import DDPGAgent
from policy_wrapper import _PolicyWrapper, DQNPolicy, PPOPolicy, PPOGAEPolicy, DDPGPolicy


STEPS_PER_EPISODE = 1000


def _maybe_add_mujoco_dll_directory() -> None:
    """
    仅在本地存在 mujoco200 路径时添加 DLL 目录。
    保留 evaluate_cheetah.py 中的行为以便 HalfCheetah 正常运行。
    """
    mujoco_bin = r"C:\Users\qtq\.mujoco\mujoco200\bin"
    if os.path.isdir(mujoco_bin):
        try:
            os.add_dll_directory(mujoco_bin)
        except Exception:
            # 在非 Windows 或无权限时静默忽略
            pass


def make_env(env_id: str,
             model_name: str,
             realtime_render: bool = False,
             render_video: bool = False):
    """
    统一的环境创建函数，支持三种模式：
    - 实时渲染（human）
    - 录制视频（RecordVideo，rgb_array）
    - 无渲染（默认）

    返回 (env, result_dir)：
    - env: gym 环境实例
    - result_dir: 若录制视频，则为视频基路径；否则为 None
    """
    result_dir = None

    if realtime_render:
        env = gym.make(env_id, render_mode="human")
    elif render_video:   
        env = gym.make(env_id, render_mode="rgb_array")
    else:
        env = gym.make(env_id, render_mode=None)

    if render_video:
        run_id = datetime.now().strftime("%Y%m%d_%H%M%S")
        base_name = os.path.splitext(model_name)[0]
        name_prefix = f"{base_name}_{run_id}"
        os.makedirs("videos", exist_ok=True)
        env = gym.wrappers.RecordVideo(
            env,
            video_folder="videos",
            episode_trigger=lambda eid: eid == 0,  # 先只录制第一个 episode
            name_prefix=name_prefix,
        )
        result_dir = os.path.join("videos", name_prefix + "-episode-0")

    return env, result_dir


# def _resolve_model_path(model_path: str) -> str:
#     """
#     兼容两种写法：
#     - 直接传完整路径
#     - 只传文件名，此时默认在当前工作目录下的 models/ 里查找
#     """
#     if os.path.isabs(model_path) or os.path.exists(model_path):
#         return model_path
#     candidate = os.path.join("models", model_path)
#     return candidate


def load_policy(env, agent_name: str, model_name: str) -> _PolicyWrapper:
    """
    按 agent_name 加载不同的策略，并统一成 act(state) 接口。
    """
    agent_name = agent_name.lower()
    obs_space = env.observation_space
    act_space = env.action_space

    model_path = os.path.join("models", model_name)

    if agent_name == "ddpg":
        if not hasattr(act_space, "shape") or act_space.shape is None:
            raise ValueError("DDPG 需要连续动作空间（Box），当前环境动作空间不兼容。")

        state_dim = obs_space.shape[0]
        action_dim = act_space.shape[0]
        agent = DDPGAgent(state_dim, action_dim)
        agent.load(model_path)
        return DDPGPolicy(agent)

    state_dim = obs_space.shape[0]
    if hasattr(act_space, "n"):
        action_dim = act_space.n
    else:
        raise ValueError("当前环境的动作空间不是离散型，不能用于 DQN/PPO/PPO-GAE。")

    if agent_name == "dqn":
        agent = DQNAgent(state_dim, action_dim, epsilon=0.0)
        agent.q_network.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.q_network.eval()
        return DQNPolicy(agent)

    if agent_name == "ppo":
        agent = PPOAgent(state_dim, action_dim)
        agent.policy.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.policy.eval()
        return PPOPolicy(agent)

    if agent_name in ("ppo_gae", "ppo-gae"):
        agent = PPO_GAE_Agent(state_dim, action_dim)
        agent.policy.load_state_dict(torch.load(model_path, map_location="cpu"))
        agent.policy.eval()
        return PPOGAEPolicy(agent)

    raise ValueError(f"Unsupported agent type: {agent_name}")


def run_one_episode(env, policy: _PolicyWrapper, max_steps = None) -> float:
    """
    通用的单 episode 运行逻辑，兼容 gym 与 gymnasium 的返回格式。
    """
    reset_out = env.reset()
    if isinstance(reset_out, (list, tuple)):
        state = reset_out[0]
    else:
        state = reset_out

    total_reward = 0.0
    step = 0

    while True:
        action = policy.act(state)
        step_out = env.step(action)

        if len(step_out) == 5:
            state, reward, terminated, truncated, _ = step_out
            done = terminated or truncated
        else:
            state, reward, done, _ = step_out

        total_reward += float(reward)
        step += 1

        if done:
            break
        if max_steps is not None and step >= max_steps:
            break

    return total_reward


def run_episodes(env, policy: _PolicyWrapper, num_episodes: int, max_steps = None):
    rewards = []
    for _ in range(num_episodes):
        r = run_one_episode(env, policy, max_steps=max_steps)
        rewards.append(r)
    return rewards


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Unified RL evaluation: run episodes and output JSON.")
    parser.add_argument("--env", required=True, help="Gym/Gymnasium environment id, e.g. HalfCheetah-v2 or LunarLander-v3")
    parser.add_argument("--agent", required=True,
                        help="Agent type: ddpg / dqn / ppo / ppo_gae")
    parser.add_argument("--model_name", required=True,
                        help="Path or name of the saved model weights.")
    parser.add_argument("--episodes", type=int, default=10,
                        help="Number of evaluation episodes.")
    parser.add_argument("--workspace", default=None,
                        help="Project root where code and models live (default: cwd).")
    parser.add_argument("--realtime_render", action="store_true",
                        help="Render in realtime (human).")
    parser.add_argument("--render_video", action="store_true",
                        help="Record video to ./videos/")
    return parser.parse_args(argv)


def main(argv=None):
    _maybe_add_mujoco_dll_directory()

    args = parse_args(argv)

    # 处理 workspace：与 evaluate_cheetah.py 保持一致的行为
    workspace = os.path.abspath(args.workspace or os.getcwd())
    if args.workspace and workspace not in sys.path:
        sys.path.insert(0, workspace)
    os.chdir(workspace)

    result = {
        "status": "FINISHED",
        "avgReward": 0.0,
        "rewards": [],
        "result_dir": None,
    }

    try:
        env, result_dir = make_env(
            env_id=args.env,
            model_name=os.path.basename(args.model_name),
            realtime_render=bool(args.realtime_render),
            render_video=bool(args.render_video),
        )

        # HalfCheetah 等连续环境默认给一个步数上限
        max_steps = STEPS_PER_EPISODE if "cheetah" in args.env.lower() else None

        policy = load_policy(env, args.agent, args.model_name)
        rewards = run_episodes(env, policy, args.episodes, max_steps=max_steps)
        env.close()

        result["rewards"] = rewards
        result["avgReward"] = float(sum(rewards) / len(rewards)) if rewards else 0.0
        result["result_dir"] = result_dir

    except Exception as e:
        result["status"] = "FAILED"
        result["error"] = str(e)
        print(json.dumps(result, ensure_ascii=False))
        return 1

    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())

