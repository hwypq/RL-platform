package org.example.rlplatform.service;


import jakarta.validation.constraints.Email;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    User findByUserNameAndIsDeletedFalse(String username);

    User findByIdAndIsDeletedFalse(Integer id);

    void register(String username, String password, String email, UserRole role);

    void update(User user);

    void updateAvatar(String avatarUrl);

    void updatePwd(String newPwd);

    List<User> list();

    void changeUserRole(Integer id, UserRole newRole);

    void softDeleteStudent(Integer id);

    void resetPwd(Integer id, String newPwd);

    User findByEmail(@Email String email);

    Page<User> listByCondition(Integer pageNum, Integer pageSize, String role, String keyword, Integer classId, Boolean isDeleted);

    void studentChooseClass(String code);

    void teacherAssignClass(Integer userId, Integer classId);

    void studentQuitClass();
}
