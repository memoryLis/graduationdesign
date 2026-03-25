package com.hmall.common.interceptor;

import cn.hutool.core.util.StrUtil;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ClassName: UserInfoInterceptor
 * Package: com.hmall.common.interceptor
 * Description:
 *
 * @Author liang
 * @Create 2024/10/14 17:39
 * @Version jdk17.0
 */
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userInfo = request.getHeader("user-info");
        if(StrUtil.isNotBlank(userInfo)){
            //放入Threadlocal
            UserContext.setUser(Long.valueOf(userInfo));

        }
        return  true;


    }

    //处理完所有业务后，才会执行afterCompletion()方法。
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.removeUser();
    }
}
