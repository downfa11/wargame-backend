package com.ns.behaviorbatch.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogDocument {  // 사용자의 행위 데이터 (분석 전)
    private String userId;
    private String action;
    private String message;
    private LocalDateTime timestamp;
}

