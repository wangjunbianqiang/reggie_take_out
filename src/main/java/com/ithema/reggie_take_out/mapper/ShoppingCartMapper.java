package com.ithema.reggie_take_out.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ithema.reggie_take_out.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

}
