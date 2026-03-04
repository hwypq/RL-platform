import numpy as np
from agents.dqn_agent import DQNAgent
from agents.ppo_agent import PPOAgent
from agents.ppo_gae_agent import PPO_GAE_Agent
from agents.ddpg_agent import DDPGAgent

class _PolicyWrapper:
    """统一行为接口：所有策略都实现 act(state) -> action。"""

    def act(self, state):
        raise NotImplementedError


class DQNPolicy(_PolicyWrapper):
    def __init__(self, agent: DQNAgent):
        self.agent = agent

    def act(self, state):
        return self.agent.choose_action(state)


class PPOPolicy(_PolicyWrapper):
    def __init__(self, agent: PPOAgent):
        self.agent = agent

    def act(self, state):
        return self.agent.get_greedy_action(state)


class PPOGAEPolicy(_PolicyWrapper):
    def __init__(self, agent: PPO_GAE_Agent):
        self.agent = agent

    def act(self, state):
        return self.agent.get_greedy_action(state)


class DDPGPolicy(_PolicyWrapper):
    def __init__(self, agent: DDPGAgent):
        self.agent = agent

    def act(self, state):
        batch_state = np.expand_dims(state, axis=0)
        action = self.agent.predict(batch_state)
        return action