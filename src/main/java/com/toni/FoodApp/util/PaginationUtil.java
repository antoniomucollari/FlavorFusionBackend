package com.toni.FoodApp.util;

import org.springframework.data.domain.Sort;
import java.util.Map;

public class PaginationUtil {

    public static Sort buildSort(String sortBy, String sortDirection, Map<String, String> fieldMapping, String defaultField) {

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String entityField = defaultField;
        if (sortBy != null && fieldMapping.containsKey(sortBy)) {
            entityField = fieldMapping.get(sortBy);
        }

        return Sort.by(direction, entityField);
    }
}