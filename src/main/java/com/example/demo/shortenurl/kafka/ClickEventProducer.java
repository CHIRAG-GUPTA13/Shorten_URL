package com.example.demo.shortenurl.kafka;

import com.example.demo.shortenurl.config.KafkaConfig;
import com.example.demo.shortenurl.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventProducer {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    public void sendClickEvent(ClickEventMessage message) {
        CompletableFuture<SendResult<String, ClickEventMessage>> future = 
            kafkaTemplate.send(KafkaConfig.CLICK_EVENTS_TOPIC, message.getShortCode(), message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent click event for shortCode={} to partition={} with offset={}", 
                    message.getShortCode(), 
                    result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send click event for shortCode={}: {}", 
                    message.getShortCode(), ex.getMessage());
            }
        });
    }
}
