package com.example.demo.shortenurl.controller;

import com.example.demo.shortenurl.dto.QrCodeResponseDto;
import com.example.demo.shortenurl.service.QrCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for QR code generation endpoints.
 * 
 * Endpoints:
 * - GET /api/urls/{shortCode}/qr - Returns JSON with Base64-encoded QR code
 * - GET /api/urls/{shortCode}/qr/image - Returns raw PNG image
 */
@RestController
@RequestMapping("/api/urls")
public class QrCodeController {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeController.class);

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * Get QR code as JSON with Base64-encoded PNG.
     * 
     * @param shortCode The short code to generate QR for
     * @return JSON response with QR code data
     */
    @GetMapping("/{shortCode}/qr")
    public ResponseEntity<QrCodeResponseDto> getQrCode(@PathVariable String shortCode) {
        logger.info("GET /api/urls/{}/qr", shortCode);
        
        QrCodeResponseDto qrCode = qrCodeService.generateQrCode(shortCode);
        return ResponseEntity.ok(qrCode);
    }

    /**
     * Get QR code as raw PNG image for direct embedding in <img> tags.
     * 
     * @param shortCode The short code to generate QR for
     * @return PNG image with Content-Type: image/png
     */
    @GetMapping("/{shortCode}/qr/image")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable String shortCode) {
        logger.info("GET /api/urls/{}/qr/image", shortCode);
        
        byte[] qrPng = qrCodeService.generateQrCodePng(shortCode);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("inline", "qr-" + shortCode + ".png");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(qrPng);
    }
}
