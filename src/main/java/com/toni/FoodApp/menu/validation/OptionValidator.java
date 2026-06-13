package com.toni.FoodApp.menu.validation;

import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.menu.dtos.MenuOptionFlatDTO;
import com.toni.FoodApp.menu.dtos.OptionSelection;
import com.toni.FoodApp.menu.repository.OptionRepository;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.repository.BranchMenuItemRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@RequiredArgsConstructor
@Component
public class OptionValidator {

    private final BranchMenuItemRepository branchMenuItemRepository;
    private final OptionRepository optionRepository;

    public List<MenuOptionFlatDTO> validateOptionSelections(List<OptionSelection> userSelections, Long branchMenuId) {

        List<MenuOptionFlatDTO> rawDbOptions = optionRepository.findAvailableOptionsRaw(branchMenuId, branchMenuId);

        Map<Long, List<MenuOptionFlatDTO>> dbGroups = rawDbOptions.stream()
                .collect(Collectors.groupingBy(MenuOptionFlatDTO::getGroupId));

        Map<Long, List<Long>> userSelectionMap = userSelections.stream()
                .collect(Collectors.groupingBy(
                        OptionSelection::getOptionGroupId,
                        Collectors.flatMapping(
                                selection -> selection.getVariantIds().stream(),
                                Collectors.toList()
                        )
                ));

        for (Long userGroupId : userSelectionMap.keySet()) {
            if (!dbGroups.containsKey(userGroupId)) {
                throw new IllegalArgumentException(
                        String.format("Option Group ID %d is not supported by this branch menu item.", userGroupId)
                );
            }
        }

        // Validate Min/Max constraints and Variant availability
        dbGroups.forEach((groupId, groupRows) -> {

            MenuOptionFlatDTO groupConfig = groupRows.getFirst(); // Use .get(0) if not on Java 21+
            int minRequired = groupConfig.getMinSelection();
            int maxAllowed = groupConfig.getMaxSelection();

            // Get what the user selected for this group (or empty list if they picked nothing)
            List<Long> userPickedVariants = userSelectionMap.getOrDefault(groupId, Collections.emptyList());
            int selectionCount = userPickedVariants.size();

            if (selectionCount < minRequired) {
                throw new IllegalArgumentException(
                        String.format("Option Group '%s' requires at least %d selection(s). You selected %d.",
                                groupConfig.getGroupName(), minRequired, selectionCount)
                );
            }
            if (selectionCount > maxAllowed) {
                throw new IllegalArgumentException(
                        String.format("Option Group '%s' allows maximum %d selection(s). You selected %d.",
                                groupConfig.getGroupName(), maxAllowed, selectionCount)
                );
            }

            // Ensure the specific variants are supported by this branch
            Set<Long> validVariantIds = groupRows.stream()
                    .map(MenuOptionFlatDTO::getVariantId)
                    .collect(Collectors.toSet());

            for (Long pickedId : userPickedVariants) {
                if (!validVariantIds.contains(pickedId)) {
                    throw new IllegalArgumentException(
                            String.format("Variant ID %d is not valid or not supported by this menu item for Group '%s'.",
                                    pickedId, groupConfig.getGroupName())
                    );
                }
            }
        });

        return rawDbOptions;
    }
}