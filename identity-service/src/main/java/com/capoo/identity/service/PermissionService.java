package com.capoo.identity.service;

import java.util.List;

import com.capoo.identity.dto.request.PermissionRequest;
import com.capoo.identity.dto.response.PermissionResponse;

public interface PermissionService {
    PermissionResponse create(PermissionRequest request);

    List<PermissionResponse> getAll();

    void delete(String permission);
}
