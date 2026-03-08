package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    User findByUsernameAndIsDeletedFalse(String username);

    List<User> findAllByIsDeletedFalse();

    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :id")
    void updatePwd(@Param("password") String password, @Param("id") Integer id);

    @Modifying
    @Query("UPDATE User u SET u.userPic = :avatar WHERE u.id = :id")
    void updateAvatar(@Param("avatar") String avatar, @Param("id") Integer id);

    User findByEmail(String email);
}
