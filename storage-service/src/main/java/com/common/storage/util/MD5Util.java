package com.common.storage.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MD5Util {

    @Value("${signature.md5.secret-key}")
    private String secretKey;

    public String encode(String stringI) {
        try {
            String md5 = secretKey + "-" + stringI;
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            return new String(Hex.encodeHex(array));
        } catch (java.security.NoSuchAlgorithmException ex) {
            log.error("MD5 algorithm not available", ex);
            return "";
        }
    }
}
