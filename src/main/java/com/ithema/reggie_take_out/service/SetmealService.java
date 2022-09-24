package com.ithema.reggie_take_out.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ithema.reggie_take_out.dto.SetmealDto;
import com.ithema.reggie_take_out.entity.Setmeal;

public interface SetmealService extends IService<Setmeal> {
    //新增套餐，同时将套餐中的菜品信息添加到setmeal_dish表中。需要操作两张表，setmeal和setmeal_dish
    void saveWithsetmeal_dish(SetmealDto setmealDto);
    //根据id，回显相应的套餐信息
    SetmealDto getByIdWithsetmeal_dish(Long id);
    //修改套餐信息，并把修改过后的套餐对应的菜品更新到setmeal_dish表中，涉及到两张表
    void updateWithsetmeal_dish(SetmealDto setmealDto);
}
