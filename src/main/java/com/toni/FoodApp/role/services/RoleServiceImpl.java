package com.toni.FoodApp.role.services;

import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.role.dto.RoleDTO;
import com.toni.FoodApp.role.entity.Role;
import com.toni.FoodApp.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService{
    private final RoleRepository roleRepository;
    private final ModelMapper modelMapper;

    @Override
    public Response<RoleDTO> createRole(RoleDTO roleDTO) {
        Role role = modelMapper.map(roleDTO, Role.class);
        if(roleRepository.findByName(role.getName()).isPresent()){
            throw new BadRequestException("Role with name " + role.getName() + " already exists");
        }
        Role savedRole = roleRepository.save(role);
        return Response.<RoleDTO>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Role created successfully")
                .data(modelMapper.map(savedRole, RoleDTO.class))
                .build();
    }

    @Override
    public Response<RoleDTO> updateRole(RoleDTO roleDTO) {
        Role existingRole = roleRepository.findById(roleDTO.getId())
                .orElseThrow(()-> new RuntimeException("Role not found"));

        if(roleRepository.findByName(roleDTO.getName()).isPresent()){
            throw new BadRequestException("Role with name " + roleDTO.getName() + " already exists");
        }
        existingRole.setName(roleDTO.getName());
        Role updatedRole = roleRepository.save(existingRole);
        return Response.<RoleDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role updated successfully")
                .data(modelMapper.map(updatedRole, RoleDTO.class))
                .build();
    }

    @Override
    public Response<List<RoleDTO>> getAllRole() {
        List<Role> roles = roleRepository.findAll();
        List<RoleDTO> roleDtos = roles.stream().map(role -> modelMapper.map(role, RoleDTO.class)).toList();

        return Response.<List<RoleDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Roles retrieved successfully")
                .data(roleDtos)
                .build();
    }

    @Override
    public Response<?> deleteRole(Long id) {
        if(!roleRepository.existsById(id)) throw new NotFoundException("Role not found");

        roleRepository.deleteById(id);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role deleted successfully")
                .build();
    }
}
