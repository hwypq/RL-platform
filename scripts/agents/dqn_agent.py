import torch
import torch.nn as nn
import numpy as np
import random
from collections import deque


class ReplayBuffer:
    """ 经验回放池 """

    def __init__(self, capacity):
        """
        初始化经验回放池
        :param capacity: 回放池的最大容量
        """
        # deque 是一个双端队列，当容量满时，再添加新元素会自动从另一端移除旧元素
        self.buffer = deque(maxlen=int(capacity))

    def add(self, state, action, reward, next_state, done):
        """
        向回放池中添加一条经验
        """
        # 将经验以元组形式存储
        experience = (state, action, reward, next_state, done)
        self.buffer.append(experience)

    def sample(self, batch_size):
        """
        从回放池中随机采样一个批次的经验
        :param batch_size: 批次大小
        :return: 一个包含 state, action, reward, next_state, done 张量的元组
        """
        # 从 buffer 中随机采样 batch_size 条经验
        batch = random.sample(self.buffer, batch_size)

        # 将经验解包并转换为 NumPy 数组
        # zip(*batch) 会将 [(s1,a1,r1,...), (s2,a2,r2,...)] 转换为 ([s1,s2,...], [a1,a2,...], ...)
        states, actions, rewards, next_states, dones = map(np.array, zip(*batch))

        # 将 NumPy 数组转换为 PyTorch 张量，并移动到合适的设备（例如 GPU）
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        states = torch.FloatTensor(states).to(device)
        actions = torch.LongTensor(actions).unsqueeze(1).to(device)  # unsqueeze(1) 增加一个维度以匹配网络输出
        rewards = torch.FloatTensor(rewards).unsqueeze(1).to(device)
        next_states = torch.FloatTensor(next_states).to(device)
        dones = torch.FloatTensor(dones).unsqueeze(1).to(device)

        return states, actions, rewards, next_states, dones

    def __len__(self):
        """
        返回当前回放池中的经验数量
        """
        return len(self.buffer)


class QNetwork(nn.Module):
    """ Q值网络 """
    def __init__(self, state_dim, action_dim, hidden_dim=128):
        """
        初始化Q网络
        :param state_dim: 状态空间的维度
        :param action_dim: 动作空间的维度
        :param hidden_dim: 隐藏层的维度
        """
        super(QNetwork, self).__init__()
        self.network = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, action_dim)
        )

    def forward(self, state):
        """
        前向传播
        :param state: 状态张量
        :return: 每个动作的Q值
        """
        return self.network(state)



class DQNAgent:
    """ DQN Agent """

    def __init__(self, state_dim, action_dim, lr=1e-3, gamma=0.99, epsilon=0.9,
                 target_update_freq=100, buffer_capacity=5000):
        """
        初始化 DQN Agent
        :param state_dim: 状态维度
        :param action_dim: 动作维度
        :param lr: 学习率 (learning rate)
        :param gamma: 折扣因子 (discount factor)
        :param epsilon: 探索率 (exploration rate)
        :param target_update_freq: 目标网络更新频率
        :param buffer_capacity: 经验回放池容量
        """
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

        self.action_dim = action_dim
        self.gamma = gamma
        self.epsilon = epsilon
        self.target_update_freq = target_update_freq

        # 创建主网络和目标网络
        self.q_network = QNetwork(state_dim, action_dim).to(self.device)
        self.target_network = QNetwork(state_dim, action_dim).to(self.device)

        # 将主网络的权重复制到目标网络
        self.target_network.load_state_dict(self.q_network.state_dict())
        self.target_network.eval()  # 目标网络不进行梯度下降，只用于计算目标Q值

        # 创建优化器
        self.optimizer = torch.optim.Adam(self.q_network.parameters(), lr=lr)

        # 创建经验回放池
        self.buffer = ReplayBuffer(buffer_capacity)

        # 记录更新次数，用于控制目标网络的更新
        self.update_count = 0

    def choose_action(self, state):
        """
        使用 epsilon-greedy 策略选择动作
        :param state: 当前状态 (numpy array)
        :return: 选择的动作 (int)
        """
        # epsilon 概率进行随机探索
        if random.random() < self.epsilon:
            return random.randrange(self.action_dim)

        # 1-epsilon 概率进行利用
        else:
            state = torch.FloatTensor(state).unsqueeze(0).to(self.device)  # 增加 batch 维度
            # 不计算梯度，以加速计算
            with torch.no_grad():
                q_values = self.q_network(state)
            # 选择 Q 值最大的动作
            return q_values.argmax().item()

    def update(self, batch_size):
        """
        采样经验并更新网络参数
        :param batch_size: 批次大小
        """
        # 如果经验池中的样本数量不足一个批次，则不进行更新
        if len(self.buffer) < batch_size:
            return

        # 从经验池采样
        states, actions, rewards, next_states, dones = self.buffer.sample(batch_size)

        # --- 计算当前Q值 ---
        # 根据 q_network 计算出在 states 状态下，采取 actions 动作的 Q 值
        # q_network(states) -> [batch_size, action_dim]
        # actions -> [batch_size, 1]
        # .gather(1, actions) -> 得到每个样本对应动作的Q值
        current_q_values = self.q_network(states).gather(1, actions)

        # --- 计算目标Q值 ---
        # 使用 target_network 计算下一个状态的最大Q值
        # .max(1) -> 返回 (values, indices) 元组，[0] 取出 values
        # .unsqueeze(1) -> 增加一个维度以匹配 current_q_values 的形状
        # .detach() -> 不计算梯度
        next_q_values = self.target_network(next_states).max(1)[0].unsqueeze(1)

        # 计算目标Q值 (Bellman方程)
        # 如果一个状态是终止状态 (done=1)，那么它的未来奖励为0
        target_q_values = rewards + (1 - dones) * self.gamma * next_q_values

        # --- 计算损失并更新 ---
        loss = nn.MSELoss()(current_q_values, target_q_values)

        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        # --- 更新目标网络 ---
        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())




class DoubleDQNAgent(DQNAgent):
    """ Double DQN Agent """

    def __init__(self, state_dim, action_dim, lr=1e-3, gamma=0.99, epsilon=0.9,
                 target_update_freq=100, buffer_capacity=5000):
        # 完全继承父类的初始化方法
        super().__init__(state_dim, action_dim, lr, gamma, epsilon,
                         target_update_freq, buffer_capacity)
        print("Initialized Double DQN Agent")

    def update(self, batch_size):
        """
        重写 update 方法以实现 Double DQN 的逻辑
        """
        if len(self.buffer) < batch_size:
            return

        states, actions, rewards, next_states, dones = self.buffer.sample(batch_size)

        # --- Double DQN 的核心改动在这里 ---

        # 1. 使用主网络(q_network)选择下一个状态的最佳动作
        # torch.argmax a_max' Q_main(s', a')
        next_actions = self.q_network(next_states).argmax(1).unsqueeze(1)

        # 2. 使用目标网络(target_network)评估这些动作的Q值
        # .gather() 根据 next_actions 中提供的索引，在 dim=1 上选取Q值
        next_q_values = self.target_network(next_states).gather(1, next_actions)

        # --- 后续计算与标准DQN相同 ---

        target_q_values = rewards + (1 - dones) * self.gamma * next_q_values

        current_q_values = self.q_network(states).gather(1, actions)

        loss = nn.MSELoss()(current_q_values, target_q_values)

        self.optimizer.zero_grad()
        loss.backward()
        self.optimizer.step()

        self.update_count += 1
        if self.update_count % self.target_update_freq == 0:
            self.target_network.load_state_dict(self.q_network.state_dict())