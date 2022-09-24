//package com.ithema.reggie_take_out.filter;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//
//@Slf4j
//public class LoginInteceptor implements HandlerInterceptor {
//    //目标方法执行之前
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //可以查看拦截的请求
////        String requestURI = request.getRequestURI();
////        log.info("拦截的请求是{}",requestURI);
//
//
//        //登录检查逻辑
//        HttpSession session = request.getSession();
//        Object loginUser = session.getAttribute("employee");
//        if(loginUser != null){
//            //放行
//            return true;
//        }
//        //否则就拦截跳转到登录页
////        session.setAttribute("msg","请先登录");
////        response.sendRedirect("/");
//        request.setAttribute("msg","请先登录");
//        request.getRequestDispatcher("/backend/page/login/login.html").forward(request,response);
//        return false;
//    }
//
//
//    //目标方法执行之后
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
//    }
//
//
//    //页面渲染之前
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
//    }
//}
