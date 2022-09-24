package com.ithema.reggie_take_out.dto;
import com.ithema.reggie_take_out.entity.Dish;
import com.ithema.reggie_take_out.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO，全称Data Transfer Object ,即数据传输对象,一般用于展示层与服务层之间的数据传输
 * 用于封装页面提交的数据。当前端页面提交的数据没有可用的实体类接收时，就可以用这个
 *
 */
@Data
public class DishDto extends Dish {

    private List<DishFlavor> flavors = new ArrayList<>();   //用来将新增菜品的口味信息添加到口味信息表中

    private String categoryName;  //用来将根据菜品分类id也就是category_id查询的catagory_name展示在分页页面中

    private Integer copies;
}
