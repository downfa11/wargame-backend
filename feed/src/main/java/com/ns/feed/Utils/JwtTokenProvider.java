package com.ns.feed.Utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

@Component
public class JwtTokenProvider {

    private String jwtSecret; // secret key
    private long jwtExpirationInMs;
    private long refreshTokenExpirationInMs;

    public JwtTokenProvider(){
        this.jwtSecret="NYd4nEtyLtcU7cpS/1HTFVmQJd7MmrP+HafWoXZjWNOL7qKccOOUfQNEx5yvG6dfdpuBeyMs9eEbRmdBrPQCNg==";
        this.jwtExpirationInMs= 1000L * 300;
        this.refreshTokenExpirationInMs = 1000L * 360;
    }

    public Mono<String> getJwtToken(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("Authorization"));
    }

    public Mono<Long> getMembershipIdByToken(ServerWebExchange exchange) {
        return getJwtToken(exchange)
                .map(token -> token.replace("Bearer ", ""))
                .flatMap(this::parseClaims)
                .map(claims -> claims.get("sub", String.class))
                .map(Long::parseLong)
                .onErrorMap(e -> new RuntimeException("JwtToken is Invalid.", e));
    }

    public Mono<String> generateJwtToken(String membershipId) {
        return Mono.fromSupplier(() -> {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

            return Jwts.builder()
                    .setSubject(membershipId)
                    .setHeaderParam("type", "jwt")
                    .claim("id", membershipId)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(SignatureAlgorithm.HS256, jwtSecret)
                    .compact();
        }).map(token -> "Bearer " + token);
    }

    public Mono<String> generateRefreshToken(String membershipId) {
        return Mono.fromSupplier(() -> {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + refreshTokenExpirationInMs);

            return Jwts.builder()
                    .setSubject(membershipId)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(SignatureAlgorithm.HS512, jwtSecret)
                    .compact();
        });
    }

    public Mono<Boolean> validateJwtToken(String token) {
        return Mono.fromSupplier(() -> {
            try {
                Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token);
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                return false;
            }
        });
    }

    public Mono<Long> parseMembershipIdFromToken(String token) {
        return Mono.fromSupplier(() -> {
            Claims claims = Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody();
            return Long.parseLong(claims.getSubject());
        });
    }

    private Mono<Claims> parseClaims(String token) {
        return Mono.fromSupplier(() -> Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody());
    }
}