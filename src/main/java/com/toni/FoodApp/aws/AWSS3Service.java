package com.toni.FoodApp.aws;

import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URL;

public interface AWSS3Service {
    URI uploadFile(String keyName, MultipartFile file);
    void deleteFile(String keyName);
    String uploadImage(MultipartFile imageFile, String folder);
    String replaceImage(MultipartFile newFile, String oldFileUrl, String folder);
    URI uploadFile(String keyName, byte[] fileBytes, String contentType);
}
