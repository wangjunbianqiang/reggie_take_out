package com.ithema.reggie_take_out.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ithema.reggie_take_out.common.R;
import com.ithema.reggie_take_out.entity.User;
import com.ithema.reggie_take_out.service.UserService;
import com.ithema.reggie_take_out.utils.SMSUtils;
import com.ithema.reggie_take_out.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机短信验证码
     * 用redis来优化的思路
     * 1.在UserController中注入RedisTemplate对象
     * 2.在UserController的SendMsg方法中将随机生成的验证码放入redis中，设置过期时间5分钟
     * 3.在UserController中的login方法中，从Redis中获取验证码，如果登录成功的话，就在redis中把这个验证码删除
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session){

        String phone = user.getPhone();

        if(StringUtils.isNotEmpty(phone)){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            log.info("code = {}",code);

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //需要将生成的验证码保存在session中，session一般保存时间是30分钟
            // 而一般要求的短信验证码有效时间是1分钟，用redis来优化，将验证码放入redis中
//            session.setAttribute(phone,code);   //手机号作为key,验证码作为值

            //将生成的验证码加入redis中，设置过期时间5分钟
            redisTemplate.opsForValue().set(phone,code,5, TimeUnit.MINUTES);
            return R.success("手机验证码发送成功");
        }
        return R.error("短信发送失败");
    }


    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session){
        log.info(map.toString());
        //获取手机号
        String phone = map.get("phone").toString();
        
        //获取验证码
        String code = map.get("code").toString();
        
        // 从session中获取保存的验证码
//        Object codeInsession = session.getAttribute(phone);

        //从redis中获取验证码
        Object codeInsession = redisTemplate.opsForValue().get(phone);

        //进行验证码的比对
        if(codeInsession != null && codeInsession.equals(code)){
            //如果比对成功，说明登录成功
            LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userLambdaQueryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(userLambdaQueryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否是新用户，如果是新用户就自动完成注册
                user = new User();
                user.setStatus(1);;
                user.setPhone(phone);
                userService.save(user);

            }
            session.setAttribute("user",user.getId());  //把当前这个用户的id放到session中，因为过滤器会校验用户
            //登录成功，把验证码从redis中删除
            redisTemplate.delete(phone);
            return R.success(user);
        }
        return R.error("登录失败");
    }

    /**
     * 移动端用户退出登录
     * @return
     */
    @PostMapping("/loginout")
    public R<String> loginout(HttpServletRequest request){
        //退出登录要清理session中保存的当前员工的id
        request.getSession().removeAttribute("user");
        return R.success("退出登录成功");
    }
}
