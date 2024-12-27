package com.ns.membership.axon.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModifyMemberEvent {
    // - 개인정보 변경 이벤트
    //    - 이메일 인증이 필요한 비밀번호 변경
    //    - 초기 이메일 변경
    //    - account, name도 변경시 updatedAt(LocalDateTime)도 함께 변해야한다

    private String aggregateIdentifier;
    private String membershipId;

    private String account;
    private String name;
    private String email;
    private String password;
}
