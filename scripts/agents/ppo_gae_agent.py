import torch
import torch.nn as nn
from torch.distributions import Categorical


# ActorCritic 网络定义保持不变，我们把它也放在这个新文件里
class ActorCritic(nn.Module):
    def __init__(self, state_dim, action_dim, hidden_dim=64):
        super(ActorCritic, self).__init__()
        self.actor = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, action_dim),
            nn.Softmax(dim=-1)
        )
        self.critic = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.Tanh(),
            nn.Linear(hidden_dim, 1)
        )

    def act(self, state):
        action_probs = self.actor(state)
        dist = Categorical(action_probs)
        action = dist.sample()
        action_logprob = dist.log_prob(action)
        return action.detach(), action_logprob.detach()

    def evaluate(self, state, action):
        action_probs = self.actor(state)
        dist = Categorical(action_probs)
        action_logprobs = dist.log_prob(action)
        dist_entropy = dist.entropy()
        state_values = self.critic(state)
        return action_logprobs, state_values, dist_entropy


class PPO_GAE_Agent:
    """
    一个全新的 Agent 类，实现了 PPO with GAE (广义优势估计)
    """

    def __init__(self, state_dim, action_dim, lr_actor=3e-4, lr_critic=1e-3,
                 gamma=0.99, K_epochs=40, eps_clip=0.2, gae_lambda=0.95):

        self.gamma = gamma
        self.eps_clip = eps_clip
        self.K_epochs = K_epochs
        self.gae_lambda = gae_lambda
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

        # 内部使用的临时列表
        self.rewards = []
        self.is_terminals = []

    def select_action(self, state):
        with torch.no_grad():
            state = torch.FloatTensor(state).to(self.device)
            action, action_logprob = self.policy_old.act(state)

        self.buffer.append((state, action, action_logprob))

        return action.item()

    def get_greedy_action(self, state):
        """
        在评估时，选择概率最高的“贪心”动作
        """
        with torch.no_grad():
            state = torch.FloatTensor(state).to(self.device)
            action_probs = self.policy.actor(state)
            action = torch.argmax(action_probs).item()
        return action

    def update(self):
        # 使用 GAE 计算优势函数
        old_states = torch.squeeze(torch.stack([s for s, a, lp in self.buffer], dim=0)).detach().to(self.device)
        old_actions = torch.squeeze(torch.stack([a for s, a, lp in self.buffer], dim=0)).detach().to(self.device)
        old_logprobs = torch.squeeze(torch.stack([lp for s, a, lp in self.buffer], dim=0)).detach().to(self.device)

        with torch.no_grad():
            old_values = self.policy_old.critic(old_states).detach()

        advantages = []
        last_advantage = 0
        for i in reversed(range(len(self.rewards))):
            is_terminal = self.is_terminals[i]
            reward = self.rewards[i]
            next_value = 0 if is_terminal else old_values[i + 1] if i < len(self.rewards) - 1 else 0
            td_error = reward + self.gamma * next_value - old_values[i]
            last_advantage = td_error + self.gamma * self.gae_lambda * (1 - is_terminal) * last_advantage
            advantages.insert(0, last_advantage)

        advantages = torch.tensor(advantages, dtype=torch.float32).to(self.device)
        v_targets = advantages + old_values.squeeze()
        advantages = (advantages - advantages.mean()) / (advantages.std() + 1e-7)

        # K 个 Epochs 的优化
        for _ in range(self.K_epochs):
            logprobs, state_values, dist_entropy = self.policy.evaluate(old_states, old_actions)
            ratios = torch.exp(logprobs - old_logprobs.detach())

            surr1 = ratios * advantages
            surr2 = torch.clamp(ratios, 1 - self.eps_clip, 1 + self.eps_clip) * advantages
            actor_loss = -torch.min(surr1, surr2)
            critic_loss = self.MseLoss(state_values.squeeze(), v_targets.detach())
            loss = actor_loss + 0.5 * critic_loss - 0.01 * dist_entropy

            self.optimizer.zero_grad()
            loss.mean().backward()
            self.optimizer.step()

        self.policy_old.load_state_dict(self.policy.state_dict())

        # 清空临时数据
        self.buffer = []
        self.rewards = []
        self.is_terminals = []