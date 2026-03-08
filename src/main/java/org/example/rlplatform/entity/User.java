package org.example.rlplatform.entity;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.example.rlplatform.anno.ValidEmailSuffix;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Pattern(regexp = "^\\S{1,10}$")
    @Column(name = "nickname")
    private String nickname = null;

    @NotEmpty
    @Email
    @ValidEmailSuffix(allowedSuffixes = {"bjtu.edu.cn"}, message = "系统只支持学校邮箱")
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "user_pic")
    private String userPic;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.STUDENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private StudentClass studentClass;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // 软删除字段 false未删除, true已删除
}
