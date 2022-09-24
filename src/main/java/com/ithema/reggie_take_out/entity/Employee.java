package com.ithema.reggie_take_out.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;

    private String idNumber;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)  //公共字段填充，仅仅在插入时进行字段填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  //公共字段填充，在插入和更新时进行字段填充
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)  //公共字段填充，仅仅在插入时进行字段填充
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)  //公共字段填充，在插入和更新时进行字段填充
    private Long updateUser;

}
