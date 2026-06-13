package com.toni.FoodApp.restaurant.entity;
import com.toni.FoodApp.BeanUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.geo.Point;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostRemove;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RestaurantBranchListener {

    // The key we will use in Redis for all our branch locations
    private static final String REDIS_GEO_KEY = "restaurant:locations";

    @PostPersist
    @PostUpdate
    public void onSave(RestaurantBranch branch) {
        StringRedisTemplate redisTemplate = BeanUtil.getBean(StringRedisTemplate.class);
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        if (branch.getLocation() != null && branch.isActive()) {
            geoOps.add(
                    REDIS_GEO_KEY,
                    new org.springframework.data.geo.Point(branch.getLocation().getX(), branch.getLocation().getY()),
                    branch.getId().toString()
            );
        } else {
            onRemove(branch);
        }
    }

    @PostRemove
    public void onRemove(RestaurantBranch branch) {
        StringRedisTemplate redisTemplate = BeanUtil.getBean(StringRedisTemplate.class);
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        // Remove from the GEO index
        geoOps.remove(REDIS_GEO_KEY, branch.getId().toString());
    }
}
