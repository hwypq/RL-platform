package org.example.rlplatform.controller;


import io.micrometer.common.util.StringUtils;
import jakarta.validation.constraints.Pattern;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.JwtUtil;
import org.example.rlplatform.utils.Md5Util;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/register")
    public Result register(
            @RequestParam String username,
            @RequestParam @Pattern(regexp = "^\\S{5,16}$") String password,
            @RequestParam String email,
            @RequestParam(defaultValue = "STUDENT") String role) {
        User u = userService.findByUserNameAndIsDeletedFalse(username);
        User ue = userService.findByEmail(email);
        if (u != null) {
            return Result.error("用户名已被占用");
        } else if (ue != null) {
            return Result.error("邮箱已被占用");
        } else {
            UserRole userRole;
            try {
                userRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.error("身份参数无效");
            }
            userService.register(username, password, email, userRole);
            return Result.success();
        }
    }

    @PostMapping("/login")
    public Result<String> login(String username, String password){

        User loginuser = userService.findByUserNameAndIsDeletedFalse(username);
        if(loginuser == null){
            return Result.error("用户名错误");
        }

        if(loginuser.getPassword().equals(Md5Util.getMD5String(password))){
            Map<String,Object> claims = new HashMap<>();
            claims.put("id", loginuser.getId());
            claims.put("username", loginuser.getUsername());
            claims.put("role", loginuser.getRole().name());

            String tokenVersion = UUID.randomUUID().toString();
            claims.put("tokenVersion", tokenVersion);

            String key = "login:version:" + loginuser.getId();
            stringRedisTemplate.opsForValue().set(key, tokenVersion, 1, TimeUnit.HOURS);

            String token = JwtUtil.genToken(claims);
            return Result.success(token);
        }

        return Result.error("密码错误");
    }

    @GetMapping("/userInfo")
    public Result<User> userinfo(/*@RequestHeader(name="Authorization") String token*/){

//        Map<String,Object> claims = JwtUtil.parseToken(token);
//        String username = claims.get("username").toString();
        Map<String, Object> claims = ThreadLocalUtil.get();
        String userId = claims.get("id").toString();
        User user = userService.findByIdAndIsDeletedFalse(Integer.parseInt(userId));
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result update(@RequestBody @Validated User user){
        userService.update(user);
        return Result.success();
    }

    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl){
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String,String> params){
        //校验参数
        String oldPwd = params.get("oldPwd");
        String newPwd = params.get("newPwd");
        String rePwd = params.get("rePwd");

        if (StringUtils.isEmpty(oldPwd) || StringUtils.isEmpty(newPwd) || StringUtils.isEmpty(rePwd)) {
            return Result.error("缺少必要的参数");
        }

        Map<String, Object> claims = ThreadLocalUtil.get();
        String userId = claims.get("id").toString();
        if (!userService.findByIdAndIsDeletedFalse(Integer.parseInt(userId)).getPassword().equals(Md5Util.getMD5String(oldPwd))){
            return Result.error("原密码不正确");
        }

        if (!rePwd.equals(newPwd)) {
            return Result.error("两次填写的密码不一致");
        }

        //密码更新
        userService.updatePwd(newPwd);
        return Result.success();
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted
    ){
        return Result.success(userService.listByCondition(pageNum, pageSize, role, keyword, classId, isDeleted));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Result changeRole(@PathVariable Integer id,
                             @RequestParam String role) {
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.error("目标身份参数无效，应为 STUDENT / TEACHER / ADMIN");
        }

        userService.changeUserRole(id, newRole);
        return Result.success();
    }

    @DeleteMapping("/students/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result deleteStudent(@PathVariable Integer id){
        userService.softDeleteStudent(id);
        return Result.success();
    }

    @PatchMapping("/{id}/password_reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result resetPwdByAdminAndTeacher(@PathVariable Integer id, @RequestBody Map<String,String> params){
        String newPwd = params.get("newPwd");
        if (StringUtils.isEmpty(newPwd)) {
            return Result.error("新密码不能为空");
        }

        userService.resetPwd(id, newPwd);
        return Result.success();
    }

    @PatchMapping("/me/class")
    public Result studentChooseClass(@RequestParam String code){
        userService.studentChooseClass(code);
        return Result.success();
    }

    @GetMapping("/me/class")
    public Result<String> getClassName(){
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        User user = userService.findByIdAndIsDeletedFalse(userId);
        StudentClass dbclass = user.getStudentClass();
        if (dbclass == null) {
            return Result.error("您还未选择班级");
        } else if (dbclass.getIsDeleted()){
            return Result.error("班级已删除");
        }
        return Result.success(dbclass.getName());
    }

    @PatchMapping("/me/class/quit")
    public Result studentQuitClass(){
        userService.studentQuitClass();
        return Result.success();
    }

    @PatchMapping("{id}/class")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result teacherAssignClass(@PathVariable Integer id, @RequestParam Integer classId){
        userService.teacherAssignClass(id, classId);
        return Result.success();
    }
}
