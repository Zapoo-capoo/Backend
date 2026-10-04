package com.common.storage.util;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ConvertBase64Utils {
    public static String convertToBase64(String filePath) {
        try {
            // Đọc toàn bộ nội dung file thành mảng byte
            byte[] fileContent = Files.readAllBytes(new File(filePath).toPath());

            // Mã hóa mảng byte thành chuỗi base64
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            return "";
        }
    }

}
