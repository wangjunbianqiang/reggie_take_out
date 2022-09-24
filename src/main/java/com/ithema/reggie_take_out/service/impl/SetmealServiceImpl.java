package com.ithema.reggie_take_out.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ithema.reggie_take_out.dto.SetmealDto;
import com.ithema.reggie_take_out.entity.Setmeal;
import com.ithema.reggie_take_out.entity.SetmealDish;
import com.ithema.reggie_take_out.mapper.SetmealMapper;
import com.ithema.reggie_take_out.service.SetmealDishService;
import com.ithema.reggie_take_out.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;  //用来将根据套餐id获取到的菜品信息保存到套餐信息setmeal_dish表中

    /**
     * 保存新增套餐
     * 套餐的菜品设置对应的菜品id
     * @param setmealDto
     */
    @Transactional              //不加上这个注解也没有报错
    @Override
    public void saveWithsetmeal_dish(SetmealDto setmealDto) {
        //保存套餐的基本信息到setmel表中
        this.save(setmealDto);

        //获取到套餐的id
        Long setmealId = setmealDto.getId();

        //获取套餐相关的菜品列表,并为每个菜品设置相应的套餐id，也就是setmealId
        List<SetmealDish> dishList = setmealDto.getSetmealDishes();
        dishList = dishList.stream().map((item) -> {    //相当于foreach，循环遍历赋值
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());

        //将根据套餐id获取到的菜品信息保存到套餐信息setmeal_dish表中
        setmealDishService.saveBatch(dishList);
    }

    /**
     * 页面信息回显，涉及到两张表
     *
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithsetmeal_dish(Long id) {
        //查询套餐的基本信息
        Setmeal setmeal = this.getById(id);

        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);

        LambdaQueryWrapper<SetmealDish> setmealDtoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDtoLambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());

        List<SetmealDish> setmealDishes = setmealDishService.list(setmealDtoLambdaQueryWrapper);

        setmealDto.setSetmealDishes(setmealDishes);
        return setmealDto;
    }

    @Override
    public void updateWithsetmeal_dish(SetmealDto setmealDto) {
        //更新套餐基本信息
        this.updateById(setmealDto);

        //先把套餐对应的setmeal_dish中的菜品先删掉--delete操作
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishLambdaQueryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());  //setmealDto.getId()也就是setmeal表中的id
        setmealDishService.remove(setmealDishLambdaQueryWrapper);

        //再添加进来更新的菜品
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        setmealDishes = setmealDishes.stream().map((item)->{
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        setmealDishService.saveBatch(setmealDishes);
    }
}
