package org.example.rlplatform.service;


import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface UserService {
    User findByUserName(String username);

    List<User> findByRole(UserRole role);

    void register(String username, String password, String email, UserRole role);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePwd(String newPwd);

    List<User> list();

    void changeUserRole(Integer id, UserRole newRole);

    void softDeleteStudent(Integer id);
}
