package com.ithema.reggie_take_out.filter;

import com.alibaba.fastjson.JSON;
import com.ithema.reggie_take_out.common.BaseContext;
import com.ithema.reggie_take_out.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*") //拦截所有请求
@Slf4j
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;


        //1.获取本次请求的URI
        String requestURI = request.getRequestURI();

        log.info("本次的请求是：{}",requestURI);

        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login",
                "/doc.html",
                "/webjars/**",
                "/swagger-resources",
                "/v2/api-docs"
        };

        //2.判断本次请求是否需要处理
        boolean check = check(urls, requestURI);

        //3.如果不需要处理则直接放行
        if(check){
            log.info("本次的请求{}不需要处理，放行",requestURI);
            filterChain.doFilter(request,response);
            return;
        }
        //4-1.判断登录状态，如果已经登录就直接放行
        Object employee = request.getSession().getAttribute("employee");
        if (employee != null) {
            log.info("用户已登录，用户id为：{}",employee);

            //LoginFilter中的doFilter方法中的线程id,以及EmployeeController中的update方法中的id以及MyMetaObjecthandler中的updateFill方法中的线程id是同一个线程
//            long id = Thread.currentThread().getId();
//            log.info("线程id是：{}",id);

            //通过session获取到与当前登录用户的id，在通过自己创建的BaseContext类的方法实现设置当前用户id
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request,response);
            return;
        }

        //4-2.判断登录状态，如果已经登录就直接放行
        Object user = request.getSession().getAttribute("user");
        if (user != null) {
            log.info("用户已登录，用户id为：{}",user);

            //LoginFilter中的doFilter方法中的线程id,以及EmployeeController中的update方法中的id以及MyMetaObjecthandler中的updateFill方法中的线程id是同一个线程
//            long id = Thread.currentThread().getId();
//            log.info("线程id是：{}",id);

            //通过session获取到与当前登录用户的id，在通过自己创建的BaseContext类的方法实现设置当前用户id
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request,response);
            return;
        }

        log.info("用户未登录");
        //前后端分离，后端不在处理页面了，只响应数据。前端执行页面的跳转
        //5.如果为登录则返回登录结果,通过输出流的方式向客户端页面响应数据。
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

//        log.info("拦截到请求:{}",request.getRequestURI());
//        filterChain.doFilter(request,response);

    }

    /**
     * 路径匹配，用来检查本次请求是否放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls,String requestURI){
        for(String url : urls){
            boolean match = PATH_MATCHER.match(url, requestURI);
            if(match){
                return true;
            }
        }
        return false;
    }
}
