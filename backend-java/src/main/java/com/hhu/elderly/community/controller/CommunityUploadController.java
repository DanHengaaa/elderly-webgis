package com.hhu.elderly.community.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/community/upload")
public class CommunityUploadController {

    private static final String BASE_DIR = "D:/Elderly/uploads/community";

    @PostMapping("/image")
    public Map<String, Object> uploadImage(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            return Map.of(
                    "success", false,
                    "message", "文件为空"
            );
        }

        String originalFilename = file.getOriginalFilename();

        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        if (!suffix.matches("\\.(jpg|jpeg|png|webp|gif)")) {
            return Map.of(
                    "success", false,
                    "message", "仅支持 jpg、jpeg、png、webp、gif 图片"
            );
        }

        LocalDate today = LocalDate.now();

        Path dir = Path.of(
                BASE_DIR,
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue())
        );

        Files.createDirectories(dir);

        String filename = UUID.randomUUID() + suffix;
        Path targetPath = dir.resolve(filename);

        file.transferTo(targetPath.toFile());

        String url = "/uploads/community/"
                + today.getYear()
                + "/"
                + String.format("%02d", today.getMonthValue())
                + "/"
                + filename;

        return Map.of(
                "success", true,
                "url", url
        );
    }
}