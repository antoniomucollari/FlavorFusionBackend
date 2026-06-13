package com.toni.FoodApp.aws;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URL;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/upload")
public class TestAWSUpload {
    private final AWSS3Service awsS3Service;
    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("keyName") String keyName){
        URI savedFile = awsS3Service.uploadFile(keyName, file);
        return ResponseEntity.ok(savedFile.toString());
    }
}
