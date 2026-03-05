package org.example.rlplatform.controller;


import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.example.rlplatform.anno.ValidEmailSuffix;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.entity.UserRole;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.JwtUtil;
import org.example.rlplatform.utils.Md5Util;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
            @RequestParam @Email @ValidEmailSuffix(allowedSuffixes = {"bjtu.edu.cn"}, message = "系统只支持学校邮箱") String email,
            @RequestParam(defaultValue = "STUDENT") String role) {
        User u = userService.findByUserName(username);
        if (u == null) {
            UserRole userRole;
            try {
                userRole = UserRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return Result.error("身份参数无效");
            }
            userService.register(username, password, email, userRole);
            return Result.success();
        } else {
            return Result.error("用户名已被占用");
        }
    }

    @PostMapping("/login")
    public Result<String> login(String username, String password){

        User loginuser = userService.findByUserName(username);
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
        String username = claims.get("username").toString();
        User user = userService.findByUserName(username);
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
        String username = claims.get("username").toString();
        if (!userService.findByUserName(username).getPassword().equals(Md5Util.getMD5String(oldPwd))){
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
    public Result<List<User>> list(){
        return Result.success(userService.list());
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

}
