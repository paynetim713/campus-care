package com.example.campus_care_backend.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.example.campus_care_backend.service.AuthService;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private final AuthService authService;

    public UploadController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/photo")
    public Map<String, Object> uploadPhoto(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam("file") MultipartFile file) throws IOException {

        authService.requireUserByToken(token);

        if (file == null || file.isEmpty()) throw new IllegalArgumentException("missing file");

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            String clean = StringUtils.getFilename(original);
            if (clean != null && clean.contains(".")) ext = clean.substring(clean.lastIndexOf('.'));
        }
        if (ext.isBlank()) ext = ".jpg";

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String name = "p_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = dir.resolve(name);
        file.transferTo(target);

        String urlPath = "/" + name;
        return Map.of("url", urlPath, "filename", name, "size", file.getSize());
    }
}