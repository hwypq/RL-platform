package org.example.rlplatform.service.impl;


import jakarta.transaction.Transactional;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.Md5Util;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findByUserName(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Override
    public void register(String username, String password, String email, UserRole role) {
        String md5String = Md5Util.getMD5String(password);
        User u = new User();
        u.setUsername(username);
        u.setPassword(md5String);
        u.setEmail(email);
        u.setRole(role);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userRepository.save(u);
    }

    @Override
    @Transactional
    public void update(User user) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer)map.get("id");
        User dbUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        dbUser.setNickname(user.getNickname());
        dbUser.setEmail(user.getEmail());
//        dbUser.setUserPic(user.getUserPic());

        dbUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbUser);
    }

    @Override
    @Transactional
    public void updateAvatar(String avatarUrl) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer)map.get("id");
        userRepository.updateAvatar(avatarUrl, userId);
    }

    @Override
    @Transactional
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer)map.get("id");
        userRepository.updatePwd(Md5Util.getMD5String(newPwd), userId);
    }

    @Override
    public List<User> list() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void softDeleteStudent(Integer id) {
        User dbUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (dbUser.getIsDeleted()) {
            throw new RuntimeException("用户已删除或不存在");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentId = (Integer) claims.get("id");
        if (dbUser.getId().equals(currentId)) {
            throw new RuntimeException("不能删除当前登录用户");
        }

        if (claims.get("role").equals(UserRole.TEACHER.name())) {
            if (dbUser.getRole() != UserRole.STUDENT) {
                throw new RuntimeException("您无权删除该用户！需要管理员权限");
            }
        }

        dbUser.setIsDeleted(true);
        dbUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbUser);
    }
}
