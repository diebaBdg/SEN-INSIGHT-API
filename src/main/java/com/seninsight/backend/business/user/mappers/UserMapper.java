package com.seninsight.backend.business.user.mappers;

import com.seninsight.backend.business.user.User;
import com.seninsight.backend.business.user.dtos.UserDTO;
import com.seninsight.backend.config.EntityMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends EntityMapper<UserDTO, User> {

}