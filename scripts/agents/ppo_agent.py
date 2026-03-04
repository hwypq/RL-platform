import torch
import torch.nn as nn
from torch.distributions import Categorical


class ActorCritic(nn.Module):
    """
    PPO 的 Actor-Critic 网络
    这个网络有两个输出头：一个用于Actor(策略)，一个用于Critic(价值)
    """

    def __init__(self, state_dim, action_dim, hidden_dim=64):
        super(ActorCritic, self).__init__()

        # Actor 网络
        self.actor = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, action_dim),
            nn.Softmax(dim=-1)  # 输出动作的概率分布
        )

        # Critic 网络
        self.critic = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, 1)  # 输出状态的价值
        )

    def forward(self):
        # 这个方法不会被直接调用，因为我们需要分别从 actor 和 critic 获取输出
        raise NotImplementedError

    def act(self, state):
        """
        根据状态选择动作
        """
        action_probs = self.actor(state)
        dist = Categorical(action_probs)
        action = dist.sample()
        action_logprob = dist.log_prob(action)
        return action.detach(), action_logprob.detach()

    def evaluate(self, state, action):
        """
        评估状态和动作
        """
        action_probs = self.actor(state)
        dist = Categorical(action_probs)
        action_logprobs = dist.log_prob(action)
        dist_entropy = dist.entropy()
        state_values = self.critic(state)
        return action_logprobs, state_values, dist_entropy


class PPOAgent:
    def __init__(self, state_dim, action_dim, lr_actor=3e-4, lr_critic=1e-3,
                 gamma=0.99, K_epochs=40, eps_clip=0.2):

        self.gamma = gamma
        self.eps_clip = eps_clip
        self.K_epochs = K_epochs
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        self.buffer = []

        self.policy = ActorCritic(state_dim, action_dim).to(self.device)
        self.optimizer = torch.optim.Adam([
            {'params': self.policy.actor.parameters(), 'lr': lr_actor},
            {'params': self.policy.critic.parameters(), 'lr': lr_critic}
        ])

        self.policy_old = ActorCritic(state_dim, action_dim).to(self.device)
        self.policy_old.load_state_dict(self.policy.state_dict())

        self.MseLoss = nn.MSELoss()

        # 【关键修正】: 在这里初始化 rewards 和 is_terminals 列表
        self.rewards = []
        self.is_terminals = []

    def select_action(self, state):
        """
        从旧策略中采样动作（用于数据收集）
        """
        with torch.no_grad():
            state = torch.FloatTensor(state).to(self.device)
            action, action_logprob = self.policy_old.act(state)

        self.buffer.append((state, action, action_logprob))

        return action.item()

    def update(self):
        # --- 计算奖励和优势 ---
        rewards = []
        discounted_reward = 0
        for reward, is_terminal in zip(reversed(self.rewards), reversed(self.is_terminals)):
            if is_terminal:
                discounted_reward = 0
            discounted_reward = reward + (self.gamma * discounted_reward)
            rewards.insert(0, discounted_reward)

        rewards = torch.tensor(rewards, dtype=torch.float32).to(self.device)
        rewards = (rewards - rewards.mean()) / (rewards.std() + 1e-7)

        old_states = torch.squeeze(torch.stack([s for s, a, lp in self.buffer], dim=0)).detach().to(self.device)
        old_actions = torch.squeeze(torch.stack([a for s, a, lp in self.buffer], dim=0)).detach().to(self.device)
        old_logprobs = torch.squeeze(torch.stack([lp for s, a, lp in self.buffer], dim=0)).detach().to(self.device)

        # --- 使用 PPO 算法进行 K 个 Epochs 的优化 ---
        for _ in range(self.K_epochs):
            logprobs, state_values, dist_entropy = self.policy.evaluate(old_states, old_actions)
            ratios = torch.exp(logprobs - old_logprobs.detach())
            advantages = rewards - state_values.detach()
            surr1 = ratios * advantages
            surr2 = torch.clamp(ratios, 1 - self.eps_clip, 1 + self.eps_clip) * advantages
            actor_loss = -torch.min(surr1, surr2)
            critic_loss = self.MseLoss(state_values, rewards)
            loss = actor_loss + 0.5 * critic_loss - 0.01 * dist_entropy
            self.optimizer.zero_grad()
            loss.mean().backward()
            self.optimizer.step()

        self.policy_old.load_state_dict(self.policy.state_dict())

        self.buffer = []
        self.rewards = []
        self.is_terminals = []

    def get_greedy_action(self, state):
        """
        在评估时，选择概率最高的“贪心”动作
        """
        with torch.no_grad():
            state = torch.FloatTensor(state).to(self.device)
            action_probs = self.policy.actor(state)
            # 选择概率最高的动作 (argmax) 而不是采样
            action = torch.argmax(action_probs).item()
        return action