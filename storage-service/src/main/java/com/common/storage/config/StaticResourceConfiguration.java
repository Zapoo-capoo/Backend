package com.common.storage.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class StaticResourceConfiguration implements WebMvcConfigurer {
    // Cache-Control headers are set directly in FileController for /web/* and /thumbnail/*
}