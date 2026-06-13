package com.toni.FoodApp.analytics.service;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class DailyStatsService {
    private final RestaurantRepository restaurantRepository;
    @Transactional
    public void performDailyReset() {
        log.info("Executing Daily Stats Reset for all restaurants...");
        restaurantRepository.resetAllDailyCounts();
        String today = LocalDate.now().toString();

        log.info("Daily Stats Reset completed successfully. Tracker set to: {}", today);
    }
}
