package com.ithema.reggie_take_out.dto;

import com.ithema.reggie_take_out.entity.Setmeal;
import com.ithema.reggie_take_out.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;  //用来将套餐setmeal对应的菜品添加到套餐信息setmeal_dish表中

    private String categoryName;  //用来展示在分页页面中
}
