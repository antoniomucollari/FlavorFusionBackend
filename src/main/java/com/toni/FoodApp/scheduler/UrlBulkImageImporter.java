package com.toni.FoodApp.scheduler;

import com.toni.FoodApp.aws.AWSS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
@Profile("import_url")
@RequiredArgsConstructor
public class UrlBulkImageImporter implements CommandLineRunner {
    private final AWSS3Service awsS3Service;
    private final RestTemplate restTemplate = new RestTemplate();
//    Path outputPath = Paths.get("url.txt");


    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting simple text-based import...");

//        // 1. Setup the output file
//        Path outputPath = Paths.get("uploaded_urls.txt");
//        Files.deleteIfExists(outputPath);
//        Files.createFile(outputPath);

        // 2. Read the new text file from resources
        InputStream inputStream = getClass().getResourceAsStream("/json/restaurants/to_upload.api");
        if (inputStream == null) {
            System.out.println("Error: to_upload.api not found!");
            return;
        }

        // 3. Read the file line by line
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {

                // Skip empty lines
                if (line.trim().isEmpty()) continue;

                // Split the line by spaces into 3 parts
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;

                String folderName = parts[0];
                String fileName = parts[1];   // e.g., "cover.png?w=1080"
                String sourceUrl = parts[2];  // e.g., "https://..."

                // 1. Strip any query parameters from the filename (fixes AccessDenied in CloudFront)
                if (fileName.contains("?")) {
                    fileName = fileName.substring(0, fileName.indexOf("?"));
                }

                // 2. Force the extension to be .jpg since you are using outputFormat("jpg")
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".png";
                } else {
                    fileName = fileName + ".png";
                }

                System.out.println("Downloading: " + fileName);
                byte[] originalImageBytes = restTemplate.getForObject(sourceUrl, byte[].class);

                if (originalImageBytes != null) {
                    // Keep the resize logic so your app stays fast!
                    ByteArrayInputStream bais = new ByteArrayInputStream(originalImageBytes);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Thumbnails.of(bais)
                            .size(600, 600)
                            .outputFormat("png")
                            .toOutputStream(baos);
                    byte[] resizedImageBytes = baos.toByteArray();

                    // Build the exact S3 key you requested: "french-tacos/691b...png"
                    String s3Key = folderName + "/" + fileName;

                    // Upload to AWS
                    URI uploadedUrl = awsS3Service.uploadFile(s3Key, resizedImageBytes, "image/png");
                    log.info("Uploaded successfully: {}", uploadedUrl.toString());

                    // Write ONLY the URL to the file to match your requested format
//                    String fileLine = uploadedUrl.toString() + System.lineSeparator();
//                    Files.write(outputPath, fileLine.getBytes(), StandardOpenOption.APPEND);
                }
            }
        }

        System.out.println("Bulk upload complete! Check uploaded_urls.txt.");
    }
}