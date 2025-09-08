package com.example.eco.util;

import com.example.odyssey.model.entity.Operator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class JwtTokenUtil {


    private static String SECRET = "ODYSSEY_SECRET_KEY";

    private static Long EXPIRATION = 604800000L;

    public static String generateToken(Operator operator) {


        return "Bearer " + Jwts.builder()
                .setSubject(operator.getId().toString())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    public static Integer getOperatorId(HttpServletRequest request) {

        String authorization = request.getHeader("Authorization");

        String customerId = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(authorization.substring(7))
                .getBody()
                .getSubject();

        return Integer.valueOf(customerId);
    }

    public static boolean isTokenExpired(String token) {

        Date expiration = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token.substring(7))
                .getBody()
                .getExpiration();

        return expiration.before(new Date());
    }
}
