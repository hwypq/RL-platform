/*package org.example.rlplatform.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.rlplatform.utils.JwtUtil;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        try {
            Map<String,Object> claims = JwtUtil.parseToken(token);
            //业务数据存储到Threadlocal中
            ThreadLocalUtil.set(claims);
            return true;    //放行
        } catch (Exception e){
            response.setStatus(401);
            return false;   // 不放行
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
*/