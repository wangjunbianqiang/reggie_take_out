package com.ithema.reggie_take_out.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ithema.reggie_take_out.common.BaseContext;
import com.ithema.reggie_take_out.common.R;
import com.ithema.reggie_take_out.dto.DishDto;
import com.ithema.reggie_take_out.dto.OrdersDto;
import com.ithema.reggie_take_out.entity.Category;
import com.ithema.reggie_take_out.entity.Dish;
import com.ithema.reggie_take_out.entity.OrderDetail;
import com.ithema.reggie_take_out.entity.Orders;
import com.ithema.reggie_take_out.service.OrderDetailService;
import com.ithema.reggie_take_out.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;
    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据:{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 移动端个人中心界面显示订单信息
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(int page,int pageSize){

        //获取用户id
        Long userId = BaseContext.getCurrentId();
        //用户获取orders基本属性，根据ordersId查询OrderDetails
        Page<Orders> ordersPage = new Page<>(page,pageSize);
        
        //用于展示在页面上
        Page<OrdersDto> ordersDtoPage = new Page<>();
        
        LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();

        ordersLambdaQueryWrapper.eq(Orders::getUserId,userId);

        ordersLambdaQueryWrapper.orderByDesc(Orders::getOrderTime);

        orderService.page(ordersPage,ordersLambdaQueryWrapper);

        //将订单细节展示在页面上，需要根据ordersId查询到对应的订单详情，并展示在页面上

        //pageInfo对象拷贝到dishDtoPage，拷贝的是分页信息,忽略了展示要展示的list属性的records。可以通过设置断点来调试查看
        BeanUtils.copyProperties(ordersPage,ordersDtoPage,"records");
        //手动处理records
        List<Orders> records = ordersPage.getRecords();  //分页信息查询到的记录
        //根据分页查询的记录一个一个处理
        List<OrdersDto> list = records.stream().map((item) -> {
            //这里是自己new出来对象，需要在把分页查询出来的item复制到这个新new出来的对象中，
            // 不然这个新new出来的属性中除了下面的setCategoryName，没有其它属性了
            OrdersDto ordersDto = new OrdersDto();

            BeanUtils.copyProperties(item,ordersDto);

            Long orderId = item.getId(); //获取订单id
            //在orderDetail表中需要根据order_id查询对应的数据

            LambdaQueryWrapper<OrderDetail> orderDetailLambdaQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailLambdaQueryWrapper.eq(OrderDetail::getOrderId,orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(orderDetailLambdaQueryWrapper);
            //给orderDetail表中设置相应的OrderDetail数据，里面偶遇前端需要的商品数量也就是number
            ordersDto.setOrderDetails(orderDetailList);
            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);

        return R.success(ordersDtoPage);
    }

    /**
     * 管理端查看订单
     * @param page
     * @param pageSize
     * @param number
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String number,String beginTime,String endTime) throws ParseException {
        log.info("beginTime:{},endTime:{}",beginTime,endTime);
        //字符串转成Date
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


        Page<Orders> orderPage = new Page<>(page,pageSize);

        LambdaQueryWrapper<Orders> orderLambdaQueryWrapper = new LambdaQueryWrapper<>();

        orderLambdaQueryWrapper.eq(StringUtils.isNotEmpty(number),Orders::getNumber,number);


        if(beginTime != null && endTime != null){
            Date bTime = format.parse(beginTime);
            Date eTime = format.parse(endTime);
            orderLambdaQueryWrapper.between(Orders::getCheckoutTime,bTime,eTime);
        }

        orderLambdaQueryWrapper.orderByDesc(Orders::getOrderTime);

        orderService.page(orderPage,orderLambdaQueryWrapper);

        List<Orders> records = orderPage.getRecords();
        records = records.stream().map((item) -> {
            item.setUserName(item.getConsignee());
            return item;
        }).collect(Collectors.toList());
        orderPage.setRecords(records);

        return R.success(orderPage);
    }

    /**
     * 管理端修改订单状态，当状态修改为已完成时也就是status=4，不知道应不应该将对应的订单以及OrderDetail中的数据给删掉
     * 当状态修改为已取消时也就是status=5,应该取消购物车里面的数据。但是前端还没有实现（暂时不写）
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> updateStatus(@RequestBody Orders orders){
        Long id = orders.getId();
        Orders orders1 = orderService.getById(id);
        orders1.setStatus(orders.getStatus());
        orderService.updateById(orders1);
        return R.success("状态修改成功");
    }

    /**
     * 再来一单，应该跳转到首页，并将相应菜品添加到购物车中。但是前端页面好像只写了跳转到首页
     * 1. 判断商品是否下架。下架的商品简单粗暴的解决，不显示。或者加入购物车的时候，显示商品下架。
     *
     * 2. 判断商品价格是否有改动，不管是否有改动，都需要去获取最新的价格.，这里可以做得比较好的就是把原来的价格进行比较，说明是降价与涨价.
     *
     * 3. 判断是否商品还有库存，没有库存也可以加到商品缺少库存的提醒中，但是不可用作为提交订单的依据。
     * @return
     */
    @PostMapping("/again")
    public R<String> again(){        //感觉都点问题
        return R.success("再来一单...");
    }
}
