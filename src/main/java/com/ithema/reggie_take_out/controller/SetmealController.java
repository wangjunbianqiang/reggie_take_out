package com.ithema.reggie_take_out.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ithema.reggie_take_out.common.CustomException;
import com.ithema.reggie_take_out.common.R;
import com.ithema.reggie_take_out.dto.DishDto;
import com.ithema.reggie_take_out.dto.SetmealDto;
import com.ithema.reggie_take_out.entity.*;
import com.ithema.reggie_take_out.service.CategoryService;
import com.ithema.reggie_take_out.service.SetmealDishService;
import com.ithema.reggie_take_out.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 点击保存按钮后，新增套餐
     * 优化：添加后，需要把redis中的缓存清理掉
     * @param setmealDto
     * @return
     */
    @PostMapping
    @CacheEvict(value = "setmealCache",allEntries = true)   //清理redis中所有名称为setmealCache的缓存
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info(setmealDto.toString());
        //需要在setmealService中重写save方法
        setmealService.saveWithsetmeal_dish(setmealDto);
        return R.success("新增套餐成功......");
    }


    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        //构造分页构造器,用于查询公共属性，主要是为了查出来category_id，再根据category_id查出来对应的套餐类型名，展示在页面上
        Page<Setmeal> pageInfo = new Page(page, pageSize);

        //用于展示在页面中
        Page<SetmealDto> setmealDtoPage = new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();

        //添加过滤条件，相当于if(name == null) 就不执行
        queryWrapper.like(StringUtils.isNotEmpty(name),Setmeal::getName,name);

        //添加排序条件
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        //执行查询
        setmealService.page(pageInfo,queryWrapper);

        //到这为止，页面上只有套餐分类名称没有显示


        //setmealDtoPage，拷贝的是分页信息,忽略了展示要展示的list属性的records。可以通过设置断点来调试查看
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");
        //手动处理records
        List<Setmeal> records = pageInfo.getRecords();  //分页信息查询到的记录
        //根据分页查询的记录一个一个处理
        List<SetmealDto> list = records.stream().map((item) -> {
            //这里是自己new出来对象，需要在把分页查询出来的item复制到这个新new出来的对象中，
            // 不然这个新new出来的属性中除了下面的setCategoryName，没有其它属性了
            SetmealDto setmealDto = new SetmealDto();

            BeanUtils.copyProperties(item,setmealDto);

            Long categoryId = item.getCategoryId();  //分类id
            //根据category_id查询category_name,需要用到category,所以要注入categoryService
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            setmealDto.setCategoryName(categoryName);
            return setmealDto;
        }).collect(Collectors.toList());


        setmealDtoPage.setRecords(list);

        return R.success(setmealDtoPage);
    }


    /**
     * 点击修改按钮，进行页面回显。涉及到两张表，setmeal以及setmeal_dish。需要重写setmealService中的getById方法
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> get(@PathVariable Long id){
        SetmealDto setmealDto =  setmealService.getByIdWithsetmeal_dish(id);
        return R.success(setmealDto);

    }


    /**
     * 修改套餐信息
     * 优化：更新后应该清理掉相应的缓存
     * @param setmealDto
     * @return
     */
    @PutMapping
    @CacheEvict(value = "setmealCache",allEntries = true)   //清理redis中所有名称为setmealCache的缓存
    public R<String> update(@RequestBody SetmealDto setmealDto){
        setmealService.updateWithsetmeal_dish(setmealDto);
        return R.success("修改套餐成功......");

    }



    /**
     * 批量删除
     * 优化：删除后需要将redis中的名称为setmealCache的缓存清理掉
     * @param ids
     * @return
     */
    @DeleteMapping
    @CacheEvict(value = "setmealCache",allEntries = true)   //清理redis中所有名称为setmealCache的缓存
    public R<String> deleteByBatch(String ids){  //ids是dish_id
        if(ids == ""){
            return R.error("id为空,请选择删除对象");
        }
        String[] split = ids.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) {
//            long id = Long.parseLong(s);
            list.add(s);
        }

        //判断套餐的状态，如果选中的套餐中有正在售卖的，则直接抛出异常
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(Setmeal::getId,list);
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus,1);

        long count = setmealService.count(setmealLambdaQueryWrapper);
        if(count > 0){
            return R.error("套餐正在售卖中，不能删除");
        }

        setmealService.removeBatchByIds(list); //参数是一个list集合


        LambdaQueryWrapper<SetmealDish> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,ids);
        dishFlavorLambdaQueryWrapper.in(SetmealDish::getSetmealId,list);
        setmealDishService.remove(dishFlavorLambdaQueryWrapper);

        return R.success("批量删除套餐成功......");
    }


    /**
     * 批量停售和起售菜品状态
     * @PathVariable int status  接收路径参数status
     * @param status 接收传过来的status，
     * @param ids 接收传过来的ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> stop(@PathVariable int status, String ids){
        String[] split = ids.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) {
//            long id = Long.parseLong(s);
// serializable序列化之后的id，可以接受string 和Number类型的数据。Number是integer,long...的父类，所以这里不用转换成long型，可以直接接收String的字符串id
            list.add(s);
        }
        List<Setmeal> setmealList = setmealService.listByIds(list);
//        dishService.getById(serializable id);  传入的是id:String|Long等，不是实体对象，就是对应你表的主键
        for (Setmeal setmeal : setmealList) {
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }

        return R.success("状态信息修改成功...");
    }


    /**
     * 根据套餐id查询该套餐下对应的菜品
     * @param setmeal
     * 优化：使用spring-cache注解将套餐放入redis中
     * @return
     */
    @GetMapping("/list")
    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status")  //将套餐信息放入缓存中，key为前端获取的setmeal.categoryId + '_' + setmeal.status拼接而成
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();

        setmealLambdaQueryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId,setmeal.getCategoryId());
        //这里的对照值不是1，而是getStatus。移动端页面只需要显示在status=1的菜品，而后端页面需要全部能看见。所以不能只写1，需要根据传过来的status进行判断展示
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus,setmeal.getStatus());


        setmealLambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(setmealLambdaQueryWrapper);
        return R.success(list);
    }



}
