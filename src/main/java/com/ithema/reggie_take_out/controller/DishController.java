package com.ithema.reggie_take_out.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ithema.reggie_take_out.common.R;
import com.ithema.reggie_take_out.dto.DishDto;
import com.ithema.reggie_take_out.entity.Category;
import com.ithema.reggie_take_out.entity.Dish;
import com.ithema.reggie_take_out.entity.DishFlavor;
import com.ithema.reggie_take_out.entity.Setmeal;
import com.ithema.reggie_take_out.service.CategoryService;
import com.ithema.reggie_take_out.service.DishFlavorService;
import com.ithema.reggie_take_out.service.DishService;
import jdk.nashorn.internal.ir.CallNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    //分页查询要根据category_id查询category_name,需要用到category,所以要注入categoryService
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * 这里的DishDto接收前端页面传过来的数据。里面有个列表是DishFlavor里的数据
     * 从前端页面中可以看到，页面只传了DishFlavor里面的name和value字段。并没有传过来dish_id这个字段。
     * 而DishFlavor是根据dish的dish_id来联系起来的。
     * 所以我们还要获取到dish_id
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){  //这里dishDto并没有接收到dish_id，需要由DishService中重写的方法来提供
        log.info(dishDto.toString());
        dishService.saveWithFlavor(dishDto);

        //1.清理redis中的全部缓存           感觉最好是全部清理，因为其它的套餐啥的都涉及了菜品
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        //精准清理
//        String key = "dish_" + dishDto.getCategoryId() + "_1";
//        redisTemplate.delete(key);
        return R.success("新增菜品成功......");
    }

    /**
     * 因为这个页面涉及到了category中的category_name，页面响应的数据数category_id但是页面要展示的是category_name
     * 而实体类Dish中并没有这个字段，所以下面的page对象中的泛型不能是根据Dish查出来的pageInfo。
     * 而之前我们创建的DishDto类是继承了Dish类的，并且还额外增加了category_name属性
     * 所以我们可以根据Dish查出来的category_id，再把根据category_id查询出来的category_name用set添加到DishDto实体类中
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);

        //构造分页构造器,用于查询category_id，及其他公共属性。再根据category_id查出来对应的菜品分类名
        Page<Dish> pageInfo = new Page(page, pageSize);

        //用于展示在页面中
        Page<DishDto> dishDtoPage = new Page<>();

        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        //添加过滤条件，相当于if(name == null) 就不执行
        queryWrapper.like(StringUtils.isNotEmpty(name),Dish::getName,name);

        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行查询
        dishService.page(pageInfo,queryWrapper);

        //将菜品分类展示在页面上，需要根据categoey_id查询到对应的菜品类名，并展示在页面上

        //pageInfo对象拷贝到dishDtoPage，拷贝的是分页信息,忽略了展示要展示的list属性的records。可以通过设置断点来调试查看
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");
        //手动处理records
        List<Dish> records = pageInfo.getRecords();  //分页信息查询到的记录
        //根据分页查询的记录一个一个处理
        List<DishDto> list = records.stream().map((item) -> {
            //这里是自己new出来对象，需要在把分页查询出来的item复制到这个新new出来的对象中，
            // 不然这个新new出来的属性中除了下面的setCategoryName，没有其它属性了
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();  //分类id
            //根据category_id查询category_name,需要用到category,所以要注入categoryService
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            dishDto.setCategoryName(categoryName);
            return dishDto;
        }).collect(Collectors.toList());


        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 看前端页面url的形式，http://localhost:8080/dish/1570333771255836674
     * 使用了restful风格，路径里面携带了参数
     * 所以得用@PathVarisble注解获取路径里面的参数id
     * 根据id查询菜品信息和对应的口味信息，回显到页面中
     * 涉及到多表联查，去Service中扩展方法
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * 为避免后端增加记录或者更新记录造成的脏读，需要在增加记录和更新记录时，把redis中的缓存清理掉
     * 有两种方式：
     * 一种是全部清理；另外一种是只清理对应的分类(精确清理)
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());
        dishService.updateWithFlavor(dishDto);
        //1.清理redis中的全部缓存           感觉最好是全部清理，因为其它的套餐啥的都涉及了菜品
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);

        //精准清理
//        String key = "dish_" + dishDto.getCategoryId() + "_1";
//        redisTemplate.delete(key);

        return R.success("修改菜品成功......");
    }


//    /**
//     * 根据菜品id，删除菜品,及相应菜品的口味信息
//     * @param ids
//     * @return
//     */
//    @DeleteMapping
//    public R<String> delete(Long ids){  //ids是dish_id
//
//
//        dishService.removeById(ids);
//
//        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,ids);
//        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);
//
//        return R.success("删除菜品成功......");
//    }



    /**
     * 批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> deleteByBatch(String ids){  //ids是dish_id
        String[] split = ids.split(",");
        List<Long> list = new ArrayList<>();
        for (String s : split) {
            long id = Long.parseLong(s);
            list.add(id);
        }

        //判断套餐的状态，如果选中的套餐中有正在售卖的，则直接抛出异常
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.in(Dish::getId,list);
        dishLambdaQueryWrapper.eq(Dish::getStatus,1);

        long count = dishService.count(dishLambdaQueryWrapper);
        if(count > 0){
            return R.error("菜品正在售卖中，不能删除");
        }

        dishService.removeBatchByIds(list); //参数是一个list集合




        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,ids);
        dishFlavorLambdaQueryWrapper.in(DishFlavor::getDishId,list);
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);

        return R.success("批量删除菜品成功......");
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
        List<Dish> dishes = dishService.listByIds(list);
//        dishService.getById(serializable id);  传入的是id:String|Long等，不是实体对象，就是对应你表的主键
        for (Dish dish : dishes) {
            dish.setStatus(status);
            dishService.updateById(dish);
        }

        return R.success("状态信息修改成功...");
    }


//    /**
//     * 根据分类id查询该分类下对应的菜品
//     * @param dish
//     * @return
//     */
//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        //根据菜品分类id查询菜品
//        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
//
//        dishLambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId,dish.getCategoryId());
//
//        //添加条件，查询状态为1（起售状态的菜品)
//        dishLambdaQueryWrapper.eq(Dish::getStatus,1);
//
//
//        dishLambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
//
//        List<Dish> list = dishService.list(dishLambdaQueryWrapper);
//        return R.success(list);
//    }


    /**
     * 根据分类id查询该分类下对应的菜品
     * redis优化，根据分类id缓存菜品
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        //动态获取key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();  //dish_15487464987684878_1   作为redis中的key

        List<DishDto> dishDtoList = null;
        //先从redis中获取缓存数据，根据分类ID进行获取，需要先生成Category_id作为key
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //如果存在，直接返回，就不需要查询数据库了
        if(dishDtoList != null){
            return R.success(dishDtoList);
        }

        //根据菜品分类id查询菜品
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();

        dishLambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId,dish.getCategoryId());

        //添加条件，查询状态为1（起售状态的菜品)
        dishLambdaQueryWrapper.eq(Dish::getStatus,1);


        dishLambdaQueryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(dishLambdaQueryWrapper);

        //根据分页查询的记录一个一个处理
       dishDtoList = list.stream().map((item) -> {
            //这里是自己new出来对象，需要在把分页查询出来的item复制到这个新new出来的对象中，
            // 不然这个新new出来的属性中除了下面的setCategoryName，没有其它属性了
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            Long categoryId = item.getCategoryId();  //分类id
            //根据category_id查询category_name,需要用到category,所以要注入categoryService
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            dishDto.setCategoryName(categoryName);

            //当前菜品的id,移动端需要显示口味信息在规格里
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);

            return dishDto;
        }).collect(Collectors.toList());

        //如果不存在，就需要从数据库中查询，再将查询到的数据放到redis中去，根据分类Id放到redis中,设置过期时间60分钟
        redisTemplate.opsForValue().set(key,dishDtoList,60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }

}
