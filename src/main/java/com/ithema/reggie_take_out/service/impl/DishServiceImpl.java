package com.ithema.reggie_take_out.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ithema.reggie_take_out.dto.DishDto;
import com.ithema.reggie_take_out.entity.Category;
import com.ithema.reggie_take_out.entity.Dish;
import com.ithema.reggie_take_out.entity.DishFlavor;
import com.ithema.reggie_take_out.mapper.CategoryMapper;
import com.ithema.reggie_take_out.mapper.DishMapper;
import com.ithema.reggie_take_out.service.CategoryService;
import com.ithema.reggie_take_out.service.DishFlavorService;
import com.ithema.reggie_take_out.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;
    /**
     * 新增菜品到dish表中，同时保存对应的口味到DishFlavor表中。
     * 设计两张表的操作，重写Service中的save方法
     * 这里的DishDto接收前端页面传过来的数据。里面有个列表是DishFlavor里的数据
     *      * 从前端页面中可以看到，页面只传了DishFlavor里面的name和value字段。并没有传过来dish_id这个字段。
     *      * 而DishFlavor是根据dish的dish_id来联系起来的。
     *      * 所以我们还要获取到dish_id
     * @param dishDto
     */
    @Transactional  //这里涉及了对数据库的更改操作，需要加上@Transactional事务注解
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish中
        this.save(dishDto);   //这里使用this或者super都可以。this表示调用的是本类的save方法；super调用的是父类Dish的save方法。因为DishDto继承了Dish。所以是一样的

        //页面点击保存的时候，数据库就会使用雪花算法生成一个dish_id。mybatis-plus实现了将生成的dish_id返回的功能，也就是getId
        Long dishId = dishDto.getId();  //菜品id

        //菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味到DishFlavor表中，需要用到DishFlavorService
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override    //这里没有涉及对数据库的更改操作，只是简单的查询，不需要加上@Transactional事务注解
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表中查
        Dish dish = this.getById(id);  //this表示的是dishService

        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);

        //查询当前菜品对应的口味信息,从dish_flavor中查询
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(dishFlavorLambdaQueryWrapper);

        dishDto.setFlavors(flavors);
        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新对应的口味信息
     * @param dishDto
     */
    @Transactional
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表的基本信息
        this.updateById(dishDto);

        //更新dish_flavor表的信息：可以分为两步，
        // 1.先删除掉当前菜品对应的口味信息--dish_flavor表的delete操作

        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);

        //再添加当前提交过来的口味数据--dish_flavor表的insert操作
        List<DishFlavor> flavors = dishDto.getFlavors();

        //和上面的saveWithFlavor一样，需要把dish_id给set进去
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }
}
