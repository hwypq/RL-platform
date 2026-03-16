package org.example.rlplatform.service.impl;


import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.StudentClassService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.Md5Util;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentClassService studentClassService;

    @Override
    public User findByUserNameAndIsDeletedFalse(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username);
    }

    @Override
    public User findByIdAndIsDeletedFalse(Integer id) {
        return getByIdAndNotDeleted(id);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Page<User> listByCondition(Integer pageNum, Integer pageSize, String role, String keyword, Integer classId, Boolean isDeleted) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("username"), like)/*,
                        criteriaBuilder.like(root.get("nickname"), like)*/
                ));
            }
            if (isDeleted != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDeleted"), isDeleted));
            }
            if (classId != null) {
                predicates.add(criteriaBuilder.equal(root.get("studentClass").get("id"), classId));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, PageRequest.of(pageNum, pageSize));
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
        User dbUser = getByIdAndNotDeleted(userId);

        dbUser.setUsername(user.getUsername());
        User dbUserByEmail = userRepository.findByEmail(user.getEmail());
        if (dbUserByEmail != null && dbUserByEmail.getId() != userId) {
            throw new RuntimeException("邮箱已被占用");
        }
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
        return userRepository.findAllByIsDeletedFalse();
    }

    @Override
    @Transactional
    public void changeUserRole(Integer id, UserRole newRole) {
        if (newRole == null) {
            throw new RuntimeException("目标身份不能为空");
        }

        User dbUser = getByIdAndNotDeleted(id);

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer currentId = (Integer) claims.get("id");

        if (dbUser.getId().equals(currentId) && newRole != UserRole.ADMIN) {
            throw new RuntimeException("不能移除自己的管理员权限");
        }

        dbUser.setRole(newRole);
        dbUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbUser);
    }


    @Override
    @Transactional
    public void softDeleteStudent(Integer id) {
        User dbUser = getByIdAndNotDeleted(id);

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

    @Override
    @Transactional
    public void resetPwd(Integer id, String newPwd) {
        User dbUser = getByIdAndNotDeleted(id);
        dbUser.setPassword(Md5Util.getMD5String(newPwd));
        dbUser.setIsDeleted(false);
        dbUser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbUser);
    }

    @Override
    @Transactional
    public void studentChooseClass(String code) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        User dbuser = getByIdAndNotDeleted(userId);
        StudentClass sc = studentClassService.findByCodeAndIsDeletedFalse(code);
        if (sc == null) {
            throw new RuntimeException("班级不存在");
        }
        dbuser.setStudentClass(sc);
        dbuser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbuser);
    }

    @Override
    @Transactional
    public void teacherAssignClass(Integer userId, Integer classId) {
        User dbuser = getByIdAndNotDeleted(userId);
        if (dbuser.getRole() == UserRole.ADMIN) {
            throw new RuntimeException("不能为管理员分配班级");
        }
        StudentClass sc = studentClassService.findByIdAndIsDeletedFalse(classId);
        dbuser.setStudentClass(sc);
        dbuser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbuser);
    }

    @Override
    @Transactional
    public void studentQuitClass() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        User dbuser = getByIdAndNotDeleted(userId);
        dbuser.setStudentClass(null);
        dbuser.setUpdateTime(LocalDateTime.now());
        userRepository.save(dbuser);
    }

    private User getByIdAndNotDeleted(Integer id) {
        User dbUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (dbUser.getIsDeleted()) {
            throw new RuntimeException("用户已删除");
        }
        return dbUser;
    }

}
