package com.toni.FoodApp.aws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import java.net.URL;
@Service
@Slf4j
@RequiredArgsConstructor
public class AWSS3ServiceImpl implements  AWSS3Service{
    private final S3Client s3Client;
    @Value("${aws.cloudfront.domain}")
    private String cloudFrontDomain;
    @Value("${aws.s3.bucket}")
    private String bucketName;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    @Override
    public URI uploadFile(String keyName, MultipartFile file) {
        log.info("Inside awsS3Service uploadFile() with MultipartFile");
        try {
            // Extract bytes and content type, then pass to the core method
            return uploadFile(keyName, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Could not read bytes from MultipartFile: " + e.getMessage());
        }
    }

    @Override
    public URI uploadFile(String keyName, byte[] fileBytes, String contentType) {
        log.info("Inside awsS3Service uploadFile() with byte[]");
        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File too large. Max allowed is 5MB");
        }
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .contentType(contentType)
                    .build();

            // 1. Upload the file to S3 as usual
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            String finalUrl = cloudFrontDomain + "/" + keyName;

            return new URI(finalUrl);

        } catch (Exception e) {
            throw new RuntimeException("AWS S3 Upload Failed: " + e.getMessage());
        }
    }
    @Override
    public String uploadImage(MultipartFile imageFile, String folder) {
        String safeFolder = (folder == null || folder.isBlank()) ? "" : folder.replaceAll("/+$", "");
        String originalFilename = imageFile.getOriginalFilename();
        String safeFilename = (originalFilename != null) ? originalFilename.replaceAll(" ", "_") : "file";
        String imageName = UUID.randomUUID().toString() + "_" + safeFilename;
        URI newImageUrl = uploadFile(safeFolder + "/" + imageName, imageFile);
        return newImageUrl.toString();
    }

    @Override
    public String replaceImage(MultipartFile newFile, String oldFileUrl, String folder) {
        // Check if user actually sent a file
        if (newFile == null || newFile.isEmpty()) {
            return oldFileUrl; // keep existing if no new file is uploaded
        }

        // If old file exists, delete it
        if (oldFileUrl != null && !oldFileUrl.isBlank()) {
            String keyName = oldFileUrl.substring(oldFileUrl.lastIndexOf("/") + 1);
            deleteFile(folder + "/" + keyName);
            log.info("File {} deleted successfully from {}", keyName, folder);
        }

        // Upload new file
        return uploadImage(newFile, folder);
    }

    @Override
    public void deleteFile(String keyName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(keyName)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        log.info("File {} deleted successfully from {}", keyName, bucketName) ;
    }
}
