package com.toni.FoodApp.scheduler;

import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.aws.AWSS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

@Slf4j
@Component
@Profile("import_file")
@RequiredArgsConstructor
public class ImageUploader implements CommandLineRunner {

    private final AWSS3Service awsS3Service;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Starting bulk upload (original files)...");

        Path imageFolder = Paths.get("src/main/resources/json/restaurants/img/");
        //C:\Users\anton\OneDrive\Desktop\Tailwind\FoodApp\backend\src\main\resources\json\restaurants\sachPizza\img

        if (!Files.exists(imageFolder)) {
            log.error("Image folder not found: {}", imageFolder.toAbsolutePath());
            return;
        }

        try (Stream<Path> files = Files.walk(imageFolder)) {

            files.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {


                            // Read ORIGINAL bytes (no compression, no resize)
                            byte[] fileBytes = Files.readAllBytes(file);

                            // Build S3 key (folder + filename)
                            String fileName = file.getFileName().toString();

// split by last "_"
                            int lastUnderscore = fileName.lastIndexOf("_");

                            if (lastUnderscore == -1) {
                                // fallback if no underscore
                                throw new IllegalArgumentException("Invalid file format: " + fileName);
                            }

                            String folder = fileName.substring(0, lastUnderscore);
                            String actualFileName = fileName.substring(lastUnderscore + 1);

// build S3 key
                            String s3Key = folder + "/" + actualFileName;

                            // Detect content type safely
                            String contentType = Files.probeContentType(file);
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }

                            // Upload directly
                            URI uploadedUrl = awsS3Service.uploadFile(
                                    s3Key,
                                    fileBytes,
                                    contentType
                            );

                            log.info("Uploaded: {}", uploadedUrl);

                        } catch (Exception e) {
                            log.error("Failed to upload file: {}", file, e);
                        }
                    });
        }

        System.out.println("Bulk upload finished.");
    }
}