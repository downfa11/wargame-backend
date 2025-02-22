package com.ns.membership.adapter.in.web;

import com.ns.membership.application.port.out.FindUserPort;
import com.ns.membership.application.port.out.ModifyUserPort;
import com.ns.membership.application.service.MailService;
import com.ns.membership.application.service.UserService;
import com.ns.membership.dto.PasswordResetRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class MailController {
    private final String UNAUTHORIZATED_MEMBERSHIP_ID_ERROR_MESSAGE = "membershipId가 유효하지 않습니다.";
    private final String PASSWORD_CHANGED_SUCCESS_MESSAGE = "비밀번호가 성공적으로 변경되었습니다.";
    private final String EMAIL_SEND_PROCESS_ERROR_MESSAGE = "이메일 전송 중 오류가 발생했습니다: ";
    private final String NOT_FOUND_MEMBERSHIP_ID_ERROR_MESSAGE = "해당하는 membershipId를 찾을 수 없습니다.";
    private final String EMAIL_SUCCESS_PASSWORD_CHANGE_MESSAGE = "비밀번호 변경을 위한 이메일 인증 메일이 전송되었습니다.";
    private final String EMAIL_SUCCESS_REGISTER_MESSAGE = "회원가입을 위한 이메일 인증 메일이 전송되었습니다.";
    private final String UNAUTHORIZATED_CODE_ERROR_MESSAGE = "인증 코드가 유효하지 않거나 일치하지 않습니다.";

    private final FindUserPort findUserPort;
    private final ModifyUserPort modifyUserPort;

    private final MailService mailService;


    @PostMapping("/membership/register/email-send")
    public Mono<ResponseEntity<String>> sendRegistrationVerificationEmail(@RequestParam String email, ServerWebExchange exchange) {

        return Mono.fromCallable(() -> {
            try {
                String code = mailService.sendRegisterMessage(email);
                exchange.getSession().map(webSession -> {
                    webSession.getAttributes().put("registerCode", code);
                    return ResponseEntity.ok(EMAIL_SUCCESS_REGISTER_MESSAGE);
                });
                return ResponseEntity.ok(EMAIL_SUCCESS_REGISTER_MESSAGE);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(EMAIL_SEND_PROCESS_ERROR_MESSAGE + e.getMessage());
            }
        });
    }

    @PostMapping("/membership/password-reset/email-send")
    public Mono<ResponseEntity<String>> sendPasswordResetVerificationEmail(ServerWebExchange exchange) {
        Long membershipId = 1L; // todo. jwtTokenProvider.getMembershipIdByToken(exchange);

        return findUserPort.findUserByMembershipId(membershipId)
                .flatMap(membership -> {
                    if (membership == null) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(NOT_FOUND_MEMBERSHIP_ID_ERROR_MESSAGE));
                    }

                    String email = membership.getEmail();
                    if (email == null) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(UNAUTHORIZATED_MEMBERSHIP_ID_ERROR_MESSAGE));
                    }

                    return Mono.fromCallable(() -> {
                        try {
                            String code = mailService.sendPasswordResetMessage(email);
                            exchange.getSession().map(webSession -> {
                                webSession.getAttributes().put("passwordResetCode", code);
                                return ResponseEntity.ok(EMAIL_SUCCESS_PASSWORD_CHANGE_MESSAGE);
                            });
                            return ResponseEntity.ok(EMAIL_SUCCESS_PASSWORD_CHANGE_MESSAGE);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(EMAIL_SEND_PROCESS_ERROR_MESSAGE + e.getMessage());
                        }
                    });
                });
    }

    @PostMapping("/membership/password-reset")
    public Mono<ResponseEntity<String>> resetPassword(@RequestBody PasswordResetRequest request, ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(webSession -> {
                    String sessionCode = (String) webSession.getAttributes().get("passwordResetCode");
                    Long membershipId = 1l; // jwtTokenProvider.getMembershipIdbyToken();

                    if (sessionCode == null || !sessionCode.equals(request.getVerify())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZATED_CODE_ERROR_MESSAGE));
                    }

                    return findUserPort.findByAccount(request.getAccount())
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MEMBERSHIP_ID_ERROR_MESSAGE)))
                            .flatMap(user -> {
                                if (!membershipId.equals(user.getId()))
                                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZATED_MEMBERSHIP_ID_ERROR_MESSAGE));

                                return modifyUserPort.resetPassword(user.getId(), request.getNewPassword())
                                        .thenReturn(ResponseEntity.ok(PASSWORD_CHANGED_SUCCESS_MESSAGE));
                            });
                });
    }

}


