/***********************************************************
 * @Description : JWT工具类
 * @author      : 梁山广(Liang Shan Guang)
 * @date        : 2019/11/17 23:55
 * @email       : liangshanguang2@gmail.com
 ***********************************************************/
package com.huawei.l00379880.admin.security;

import io.jsonwebtoken.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtTokenUtils {

    /**
     * 密钥
     */
    private static final String SECRET = "liangshanguang";

    /**
     * 权限列表名，根据这个键拿到权限列表
     */
    private static final String AUTHORITIES = "authorities";

    public static Authentication getAuthenticationFromToken(HttpServletRequest request) {
        Authentication authentication = null;
        // 获取请求携带的令牌
        String token = JwtTokenUtils.getToken(request);
        if (token != null) {
            // 请求令牌不能为空
            if (SecurityUtils.getAuthentication() == null) {
                // 上下文中Authentication为空
                Claims claims = getClaimsFromToken(token);
                if (claims == null) {
                    return null;
                }
                String username = claims.getSubject();
                if (username == null) {
                    return null;
                }
                if (isTokenExpired(token)) {
                    return null;
                }
                Object authors = claims.get(AUTHORITIES);
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (authors instanceof List) {
                    for (Object author : (List) authors) {
                        authorities.add(new GrantedAuthorityImpl((String) ((Map) author).get("authority")));
                    }
                }
                authentication = new JwtAuthenticationToken(username, null, authorities, token);
            } else {
                if (validateToken(token, SecurityUtils.getUsername())) {
                    // 如果上下文中Authentication非空，且请求令牌合法，则直接返回当前登录认证信息
                    authentication = SecurityUtils.getAuthentication();
                }
            }
        }
        return authentication;
    }

    /**
     * 验证token是否有效，一方面是正确，另一方面是没过期
     *
     * @param token    令牌
     * @param username 我们自己的用户名
     * @return 是否有效
     */
    private static boolean validateToken(String token, Object username) {
        String userName = getUsernameFromToken(token);
        return (userName.equals(username) && !isTokenExpired(token));
    }

    /**
     * 从token中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    private static String getUsernameFromToken(String token) {
        String username;
        try {
            Claims claims = getClaimsFromToken(token);
            username = claims.getSubject();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }

    /**
     * 判断令牌是否过期
     *
     * @param token 令牌
     * @return 是否过期
     */
    private static boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            // 如果过期时间在现在之前，说明token已经过期了
            return expiration.before(new Date());
        } catch (Exception e) {
            // 出异常，无法比较，表示还没过期
            return false;
        }
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private static Claims getClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            e.printStackTrace();
            claims = null;
        }
        return claims;
    }

    /**
     * 解析request获取请求token
     *
     * @param request 请求体
     * @return 解析出的token
     */
    private static String getToken(HttpServletRequest request) {
        String token = request.getHeader("Authentication");
        String tokenHead = "Bearer ";
        if (token == null) {
            // Authentication没有就尝试直接拿token
            token = request.getHeader("token");
        } else if (token.contains(tokenHead)) {
            // 含有Bearer就取Bearer后面的部分
            token = token.substring(tokenHead.length());
        }

        if ("".equals(token)) {
            token = null;
        }
        return token;
    }
}
