package com.ithema.reggie_take_out.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ithema.reggie_take_out.common.CustomException;
import com.ithema.reggie_take_out.entity.Category;
import com.ithema.reggie_take_out.entity.Dish;
import com.ithema.reggie_take_out.entity.Setmeal;
import com.ithema.reggie_take_out.mapper.CategoryMapper;
import com.ithema.reggie_take_out.service.CategoryService;
import com.ithema.reggie_take_out.service.DishService;
import com.ithema.reggie_take_out.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据category_id查看此是否词分类下是否有相关的菜品或者套餐
     * @param id
     */
    @Override
    public void removeById(Long id) {
        //构造条件查询器，根据分类id进行查询
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);
        long count1 = dishService.count(dishLambdaQueryWrapper);

        //查询当前分类是否关联了菜品，如果已经关联，抛出一个异常
        if(count1 > 0){
            //已经关联了菜品，抛出异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        //查看是否包含了套餐，如果已经关联了套餐，就抛出异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        long count2 = setmealService.count(setmealLambdaQueryWrapper);

        if(count2 > 0){
            //已经关联了套餐，抛出异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }

        //正常删除分类
        super.removeById(id);  //调用父类的removeById方法，也就是框架提供的ServiceImpl里的
    }
}
