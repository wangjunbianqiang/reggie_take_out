package com.ithema.reggie_take_out.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ithema.reggie_take_out.entity.Category;

public interface CategoryService extends IService<Category> {
    void removeById(Long id);
}
