package com.example.demo.shortenurl.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CLICKEVENTS")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SHORTCODE", nullable = false, length = 10)
    private String shortCode;

    @Column(name = "CLICKEDAT")
    private LocalDateTime clickedAt = LocalDateTime.now();

    @Column(name = "IPADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "COUNTRY", length = 100)
    private String country;

    @Column(name = "DEVICETYPE", length = 50)
    private String deviceType;

    @Column(name = "BROWSER", length = 100)
    private String browser;

    @Column(name = "USERAGENT", length = 500)
    private String userAgent;

    @Column(name = "REFERER", length = 500)
    private String referer;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public LocalDateTime getClickedAt() { return clickedAt; }
    public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
}
