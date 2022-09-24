package com.ithema.reggie_take_out.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ithema.reggie_take_out.dto.DishDto;
import com.ithema.reggie_take_out.entity.Dish;

public interface DishService extends IService<Dish> {
    //新增菜品，同时插入菜品对应的口味信息，需要同时操作两张表：dish,dish_flavor
    void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    DishDto getByIdWithFlavor(Long id);

    //更新菜品信息，同时更新对应的口味信息
    void updateWithFlavor(DishDto dishDto);
}
