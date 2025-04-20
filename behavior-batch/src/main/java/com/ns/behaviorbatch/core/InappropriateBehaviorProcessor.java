package com.ns.behaviorbatch.core;

import com.ns.behaviorbatch.domain.Alert;
import com.ns.behaviorbatch.service.InappropriateBehaviorService;
import com.ns.behaviorbatch.domain.LogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class InappropriateBehaviorProcessor implements ItemProcessor<LogDocument, Alert> {

    private final InappropriateBehaviorService inappropriateBehaviorService;

    @Override
    public Alert process(LogDocument logDocument) {
        Alert alert = inappropriateBehaviorService.isInappropriate(logDocument);

        if (alert != null) {
            alert.setHandled(false);
            return alert;
        }

        return null;
    }
}


