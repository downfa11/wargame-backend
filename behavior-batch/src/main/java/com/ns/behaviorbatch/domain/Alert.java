package com.ns.behaviorbatch.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {  // 분석한 결과로 검출된 부적절한 행위의 사용자 데이터


    private String userId;
    private String message;
    private AlertType alertType;
    private boolean handled = false;

    public Alert(String userId, String message, AlertType alertType) {
        this.userId = userId;
        this.message = message;
        this.alertType = alertType;
    }
}