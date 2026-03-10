package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.StudentClass;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.service.StudentClassService;
import org.example.rlplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/class")
public class StudentClassController {

    @Autowired
    private UserService userService;
    @Autowired
    private StudentClassService studentClassService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> create(@RequestBody StudentClass studentClass) {
        StudentClass sc = studentClassService.findByName(studentClass.getName());
        if  (sc != null) {
            return Result.error("班级名字已被占用");
        } else {
            studentClassService.create(studentClass);
            return Result.success();
        }
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@RequestBody StudentClass studentClass, @PathVariable Integer id) {
        studentClassService.update(studentClass, id);
        return Result.success();
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> softDelete(@PathVariable Integer id) {
        studentClassService.softDelete(id);
        return Result.success();
    }

    @GetMapping
    public Result<Page<StudentClass>> list(
        @RequestParam(defaultValue = "0") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false, defaultValue = "false") Boolean isDeleted
    ){
        return Result.success(studentClassService.listPage(pageNum, pageSize, isDeleted));
    }

    @GetMapping("{id}")
    public Result<StudentClass> get(@PathVariable Integer id) {
        StudentClass sc = studentClassService.findByIdAndIsDeletedFalse(id);
        return Result.success(sc);
    }

    @GetMapping("{id}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<User>> listUser(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "false") Boolean isDeleted
    ){
        return Result.success(userService.listByCondition(pageNum, pageSize, null, null, id, isDeleted));
    }
}
