package com.seninsight.backend.business.user;


import com.seninsight.backend.business.user.dtos.UserCreateDTO;
import com.seninsight.backend.business.user.dtos.UserDTO;
import com.seninsight.backend.business.user.dtos.UserUpdateDTO;

import java.util.List;
import java.util.UUID;

public interface IUserService {


    List<UserDTO> getAllUsers();

    List<UserDTO> getUsersByRole(String roleCode);

    List<UserDTO> getInstructeurs();

    List<UserDTO> getDemandeurs();

    UserDTO getUserById(UUID id);

    UserDTO getUserByEmail(String email);

    UserDTO getUserByUsername(String username);

    UserDTO createUser(UserCreateDTO createDTO);

    UserDTO updateUser(UUID id, UserUpdateDTO updateDTO);

    void deleteUser(UUID id);

    UserDTO activateUser(UUID id);

    UserDTO deactivateUser(UUID id);

    UserDTO suspendUser(UUID id);


}