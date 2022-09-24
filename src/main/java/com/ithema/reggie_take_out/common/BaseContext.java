package com.ithema.reggie_take_out.common;

/**
 * 通过Thread中的ThreadLocal变量动态的获取当前登录用户的id
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal =  new ThreadLocal<>();

    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentId(){
        return threadLocal.get();
    }
}
