package com.example.demo.shortenurl.kafka;

import com.example.demo.shortenurl.config.KafkaConfig;
import com.example.demo.shortenurl.dto.ClickEventMessage;
import com.example.demo.shortenurl.entity.ClickEvent;
import com.example.demo.shortenurl.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final ClickEventRepository clickEventRepository;

    @KafkaListener(
        topics = KafkaConfig.CLICK_EVENTS_TOPIC,
        groupId = "click-events-group"
    )
    public void consumeClickEvent(ClickEventMessage message) {
        log.debug("Received click event for shortCode={}", message.getShortCode());
        
        try {
            ClickEvent clickEvent = new ClickEvent();
            clickEvent.setShortCode(message.getShortCode());
            clickEvent.setClickedAt(LocalDateTime.ofInstant(
                message.getClickedAt(), 
                ZoneId.systemDefault()
            ));
            clickEvent.setIpAddress(message.getIpAddress());
            clickEvent.setDeviceType(message.getDeviceType());
            clickEvent.setBrowser(message.getBrowser());
            clickEvent.setUserAgent(message.getUserAgent());
            clickEvent.setReferer(message.getReferer());
            
            clickEventRepository.save(clickEvent);
            log.debug("Saved click event for shortCode={}", message.getShortCode());
        } catch (Exception e) {
            log.error("Error processing click event for shortCode={}: {}", 
                message.getShortCode(), e.getMessage(), e);
        }
    }
}
