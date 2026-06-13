package com.toni.FoodApp.role.services;

import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.role.dto.RoleDTO;

import java.util.List;

public interface RoleService {
    Response<RoleDTO> createRole(RoleDTO roleDTO);
    Response<RoleDTO> updateRole(RoleDTO roleDTO);
    Response<List<RoleDTO>> getAllRole();
    Response<?> deleteRole(Long id);
}
