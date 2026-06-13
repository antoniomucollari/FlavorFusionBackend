package com.toni.FoodApp.config;

import com.toni.FoodApp.restaurant.dtos.SimpleBranchDto;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        //global settings
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STRICT);


        modelMapper.typeMap(RestaurantBranch.class, SimpleBranchDto.class)
                .addMappings(mapper -> {
                        mapper.map(src -> src.getManager().getId(), SimpleBranchDto::setManagerId);
                        mapper.map(src -> src.getManager().getName(), SimpleBranchDto::setManagerName);
                    }
                );

        return modelMapper;
    }

}
