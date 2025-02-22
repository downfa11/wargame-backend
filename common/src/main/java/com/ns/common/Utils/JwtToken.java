package com.ns.common.utils;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@AllArgsConstructor
public class JwtToken {

    @Getter
    private final String membershipId;
    @Getter
    private final String jwtToken;
    @Getter
    private final String refreshToken;

    @Value
    public static class MembershipId {
        public MembershipId(String value) {
            this.membershipId = value;
        }

        String membershipId;
    }

    @Value
    public static class MembershipJwtToken {
        public MembershipJwtToken(String jwtToken) {
            this.jwtToken = jwtToken;
        }

        String jwtToken;
    }

    @Value
    public static class MembershipRefreshToken {
        public MembershipRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        String refreshToken;
    }
}
