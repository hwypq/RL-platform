package org.example.rlplatform.service;


import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;

import java.util.List;

public interface UserService {
    User findByUserName(String username);

    List<User> findByRole(UserRole role);

    void register(String username, String password, String email, UserRole role);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePwd(String newPwd);

    List<User> list();

    void softDeleteStudent(Integer id);
}
