package com.toni.FoodApp.restaurant.service;

import com.toni.FoodApp.analytics.service.AnalyticsService;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.response.SerializablePage;
import com.toni.FoodApp.restaurant.dtos.BranchSummaryDto;
import com.toni.FoodApp.restaurant.dtos.RestaurantBranchDetailsDto;
import com.toni.FoodApp.restaurant.dtos.RestaurantFilterCriteria;
import com.toni.FoodApp.restaurant.dtos.response.RestaurantSummaryDTO;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.mapper.RestaurantBranchMapper;
import com.toni.FoodApp.restaurant.mapper.RestaurantDtoMapper;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RestaurantQueryCacheService {

    //manual constructor
    private final RestaurantRepository restaurantRepository;
    private final RestaurantDtoMapper restaurantDtoMapper;
    private final BranchRepository branchRepository;
    private final RestaurantBranchMapper restaurantBranchMapper;
    public RestaurantQueryCacheService(RestaurantRepository restaurantRepository, RestaurantDtoMapper restaurantDtoMapper, BranchRepository branchRepository, RestaurantBranchMapper restaurantBranchMapper) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantDtoMapper = restaurantDtoMapper;
        this.branchRepository = branchRepository;
        this.restaurantBranchMapper = restaurantBranchMapper;
    }

    @Cacheable(
            value = "dashboardRestaurants",
            key = "#sort + ':' + #pageable.pageNumber + ':' + #roundedLat + ',' + #roundedLng + ':' + (#criteria != null ? #criteria.toCacheKey() : 'no-filters')",

            condition = "#criteria == null or !#criteria.isSearchActive()",

            unless = "#result.content.isEmpty()"
    )
    public SerializablePage<RestaurantSummaryDTO> getCachedDashboardPage(
            String sort,
            Pageable pageable,
            Double roundedLat,
            Double roundedLng,
            List<Long> nearbyBranchIds,
            RestaurantFilterCriteria criteria) {

        log.info("CACHE MISS: Querying database for grid {},{}", roundedLat, roundedLng);

        Page<Restaurant> entityPage = switch (sort.toLowerCase()) {
            case "rating" -> restaurantRepository.findDynamicAndRankedByRating(nearbyBranchIds, roundedLng, roundedLat, criteria, pageable);
            case "prep_time" -> restaurantRepository.findDynamicAndRankedByPrepTime(nearbyBranchIds, roundedLng, roundedLat, criteria, pageable);
            case "time", "delivery_time" -> restaurantRepository.findDynamicAndRankedByTime(nearbyBranchIds, roundedLng, roundedLat, criteria, pageable);
            case "trending", "popularity" -> restaurantRepository.findDynamicAndRankedByTrending(nearbyBranchIds, roundedLng, roundedLat, criteria, pageable);
            default -> restaurantRepository.findDynamicAndRankedByDefault(nearbyBranchIds, roundedLng, roundedLat, criteria, pageable);
        };

        Page<RestaurantSummaryDTO> dtoPage = entityPage.map(restaurant -> {
            List<RestaurantBranch> resBranch = branchRepository.findByRestaurantId(restaurant.getId());
            List<RestaurantBranch> validBranches = resBranch.stream()
                    .filter(branch -> branch.getManager() != null)
                    .filter(RestaurantBranch::isActive)
                    .filter(branch -> !branch.isClosed())
                    .filter(branch -> nearbyBranchIds.contains(branch.getId()))
                    .collect(Collectors.toList());

            // 1. Map to your exact DTO
            RestaurantSummaryDTO dto = restaurantDtoMapper.mapToSummaryDto(
                    restaurant,
                    roundedLat,
                    roundedLng,
                    validBranches
            );

            // 2. Collapse to the single best branch based on the sort criteria
            if (dto.getBranches() != null && dto.getBranches().size() > 1) {
                BranchSummaryDto bestBranch = selectBestBranch(dto.getBranches(), sort);
                dto.setBranches(List.of(bestBranch));
            }

            return dto;
        });

        return new SerializablePage<>(dtoPage);
    }
    // Determines the single best branch based on active sorting criteria.
    private BranchSummaryDto selectBestBranch(List<BranchSummaryDto> branches, String sort) {
        return switch (sort.toLowerCase()) {
            case "rating" -> branches.stream()
                    // Ensure null ratings don't break the stream, treating null as lowest
                    .max(Comparator.comparing(BranchSummaryDto::getRating,
                            Comparator.nullsFirst(BigDecimal::compareTo)))
                    .orElse(branches.getFirst());
            case "trending", "popularity" -> branches.stream()
                    // Safely handles Boolean object wrapper (null defaults to false)
                    .max(Comparator.comparing(b -> Boolean.TRUE.equals(b.getIsTrending())))
                    .orElse(branches.getFirst());
            default -> branches.stream()
                    // Primitive double comparison for closest distance proxy
                    .min(Comparator.comparingDouble(BranchSummaryDto::getDistanceInKm))
                    .orElse(branches.getFirst());
        };
    }
    @Cacheable(value = "restaurantDetails", key = "#branchId")
    public RestaurantBranchDetailsDto getBranchDetails(Long branchId) {
        log.info("Cache Miss! Hitting Database for branch id: {}", branchId);

        RestaurantBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found with id: " + branchId));

        return restaurantBranchMapper.mapToBranchDetailsDto(branch);
    }
}