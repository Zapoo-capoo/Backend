package com.capoo.identity.service;

import java.util.List;

import com.capoo.identity.dto.request.RoleRequest;
import com.capoo.identity.dto.response.RoleResponse;

public interface RoleService {
    RoleResponse create(RoleRequest request);

    List<RoleResponse> getAll();

    void delete(String role);
}
