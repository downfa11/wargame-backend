package com.ns.membership.application.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    @Value("${spring.mail.username}")
    private String username;

    private final JavaMailSender javaMailSender;
    private String authNum;

    public MimeMessage createRegisterMessage(String to) throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = javaMailSender.createMimeMessage();

        message.addRecipients(Message.RecipientType.TO, to);
        message.setSubject("wargame: 회원가입 인증");

        String msgg = buildEmailMessageContent();
        message.setText(msgg, "utf-8", "html");
        message.setFrom(new InternetAddress(username, "wargame"));
        return message;
    }

    private String buildEmailMessageContent() {
        return "<div style='margin:100px;'>" +
                "<h1> 안녕하세요!</h1>" +
                "<br>" +
                "<p>wargame 서비스를 이용해주셔서 감사합니다<p>"+
                "<br>" +
                "<p>아래 코드를 회원가입 창으로 돌아가 입력해주세요<p>" +
                "<br>" +
                "<div align='center' style='border:1px solid black; font-family:verdana';>" +
                "<h3 style='color:blue;'>회원가입 인증 코드입니다.</h3>" +
                "<div style='font-size:130%'>" +
                "CODE : <strong>" + authNum + "</strong>" +
                "</div></div>";
    }

    public MimeMessage createPasswordResetMessage(String to)throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = javaMailSender.createMimeMessage();

        message.addRecipients(Message.RecipientType.TO, to);
        message.setSubject("wargame: 비밀번호 변경 인증");

        String msgg = buildPasswordResetMessageContent();
        message.setText(msgg, "utf-8", "html");
        message.setFrom(new InternetAddress(username, "wargame"));
        return message;
    }

    private String buildPasswordResetMessageContent() {
        return "<div style='margin:100px;'>" +
                "<h1> 안녕하세요!</h1>" +
                "<br>" +
                "<p>wargame 서비스를 이용해주셔서 감사합니다<p>"+
                "<br>" +
                "<p>아래 코드를 비밀번호 변경 창으로 돌아가 입력해주세요<p>" +
                "<br>" +
                "<div align='center' style='border:1px solid black; font-family:verdana';>" +
                "<h3 style='color:blue;'>비밀번호 변경을 위한 인증 코드입니다.</h3>" +
                "<div style='font-size:130%'>" +
                "CODE : <strong>" + authNum + "</strong>" +
                "</div></div>";
    }

    public String createCode(){
        Random random = new Random();
        StringBuffer key = new StringBuffer();

        for(int i = 0; i< 8; i++){	// 인증 코드 8자리
            int index = random.nextInt(3);

            switch (index) {
                case 0 -> key.append((char) ((int) random.nextInt(26) + 97));
                case 1 -> key.append((char) (int) random.nextInt(26) + 65);
                case 2 -> key.append(random.nextInt(9));
            }
        }
        return authNum = key.toString();
    }


    public String sendRegisterMessage(String sendEmail) throws MessagingException, UnsupportedEncodingException {
        authNum = createCode();
        MimeMessage message = createRegisterMessage(sendEmail);

        try{
            javaMailSender.send(message);
        }catch (MailException es){
            es.printStackTrace();
            throw new IllegalArgumentException();
        }

        return authNum;
    }

    public String sendPasswordResetMessage(String sendEmail) throws MessagingException, UnsupportedEncodingException {
        authNum = createCode();
        MimeMessage message = createPasswordResetMessage(sendEmail);

        try{
            javaMailSender.send(message);
        }catch (MailException es){
            es.printStackTrace();
            throw new IllegalArgumentException();
        }

        return authNum;
    }

}
