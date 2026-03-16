#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import json
import os
import sys
from datetime import datetime

import imageio
import numpy as np

from pettingzoo.classic.tictactoe_v3 import env as ttt_env
from template import AgentFactory


def ensure_dir(path_str: str):
    os.makedirs(path_str, exist_ok=True)


def resolve_student_dir(base: str, path_str: str):
    if os.path.isabs(path_str):
        return path_str
    return os.path.join(base, path_str)


def load_agent_from_student_dir(student_dir_abs: str):
    config_path = os.path.join(student_dir_abs, "config.json")
    model_path = os.path.join(student_dir_abs, "model.pt")

    if not os.path.isfile(config_path):
        raise FileNotFoundError(f"config.json not found in {student_dir_abs}")
    if not os.path.isfile(model_path):
        raise FileNotFoundError(f"model.pt not found in {student_dir_abs}")

    agent = AgentFactory.create_from_config(config_path)
    agent.load(model_path)
    return agent


def normalize_frame(frame, max_size: int = 640):
    if frame is None:
        return None

    frame = np.asarray(frame)
    if frame.ndim != 3:
        return frame

    h, w = frame.shape[:2]
    longest = max(h, w)
    if longest <= max_size:
        return frame

    scale = longest / max_size
    step = max(1, int(np.ceil(scale)))
    return frame[::step, ::step, :]


def run_battle_tictactoe(agent1, agent2, games: int, video_path: str, fps: int = 5):
    env = ttt_env(render_mode="rgb_array")
    names = env.possible_agents
    if len(names) != 2:
        raise RuntimeError(f"unexpected possible_agents: {names}")

    win1 = 0
    win2 = 0
    draw = 0

    ensure_dir(os.path.dirname(video_path))
    writer = imageio.get_writer(video_path, fps=fps)

    try:
        for ep in range(games):
            env.reset()

            if ep % 2 == 0:
                a1_name, a2_name = names[0], names[1]
            else:
                a1_name, a2_name = names[1], names[0]

            controllers = {a1_name: agent1, a2_name: agent2}
            final_reward = {}

            for agent_name in env.agent_iter():
                frame = env.render()
                frame = normalize_frame(frame, max_size=640)
                if frame is not None:
                    writer.append_data(frame)

                obs, reward, termination, truncation, info = env.last()
                done = termination or truncation

                if done:
                    final_reward[agent_name] = reward
                    env.step(None)
                    continue

                action = controllers[agent_name].act(obs)
                env.step(action)

            r1 = final_reward.get(a1_name, 0)
            r2 = final_reward.get(a2_name, 0)

            if r1 > r2:
                win1 += 1
            elif r1 < r2:
                win2 += 1
            else:
                draw += 1
    finally:
        writer.close()
        env.close()

    return win1, win2, draw


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workspace", required=True)
    parser.add_argument("--student1_dir", required=True)
    parser.add_argument("--student2_dir", required=True)
    parser.add_argument("--env", default="tictactoe_v3")
    parser.add_argument("--games", type=int, default=30)
    parser.add_argument("--result_base", required=True)
    args = parser.parse_args()

    try:
        base = args.workspace
        s1_abs = resolve_student_dir(base, args.student1_dir)
        s2_abs = resolve_student_dir(base, args.student2_dir)

        agent1 = load_agent_from_student_dir(s1_abs)
        agent2 = load_agent_from_student_dir(s2_abs)

        if args.env not in ["tictactoe_v3", "tictactoe", "TicTacToe"]:
            raise ValueError(f"当前 battle_evaluator 仅支持 tictactoe_v3，收到 env={args.env}")

        result_base_abs = os.path.join(base, args.result_base)
        video_path = result_base_abs + ".mp4"
        summary_path = result_base_abs + "_summary.txt"

        win1, win2, draw = run_battle_tictactoe(agent1, agent2, args.games, video_path, fps=5)

        total = max(args.games, 1)
        win_rate1 = win1 / total
        win_rate2 = win2 / total

        if win1 > win2:
            winner = 1
        elif win2 > win1:
            winner = 2
        else:
            winner = 0

        ensure_dir(os.path.dirname(summary_path))
        with open(summary_path, "w", encoding="utf-8") as f:
            f.write(f"env: tictactoe_v3\n")
            f.write(f"games: {total}\n")
            f.write(f"win1: {win1}\n")
            f.write(f"win2: {win2}\n")
            f.write(f"draw: {draw}\n")
            f.write(f"winRate1: {win_rate1:.3f}\n")
            f.write(f"winRate2: {win_rate2:.3f}\n")
            f.write(f"time: {datetime.now()}\n")

        out = {
            "status": "FINISHED",
            "env": "tictactoe_v3",
            "games": total,
            "win1": win1,
            "win2": win2,
            "draw": draw,
            "winRate1": win_rate1,
            "winRate2": win_rate2,
            "winner": winner,
            "result_dir": args.result_base.replace("\\", "/"),
            "video": os.path.basename(video_path),
            "summary": os.path.basename(summary_path)
        }

        print(json.dumps(out, ensure_ascii=False))
        sys.exit(0)

    except Exception as e:
        out = {"status": "FAILED", "error": str(e)}
        print(json.dumps(out, ensure_ascii=False))
        sys.exit(2)


if __name__ == "__main__":
    main()
