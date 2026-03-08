package com.example.demo.shortenurl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String shortCode;
    private Instant clickedAt;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private String referer;
    private String userAgent;
}
