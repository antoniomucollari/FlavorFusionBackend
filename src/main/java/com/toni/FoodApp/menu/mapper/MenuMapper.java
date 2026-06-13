package com.toni.FoodApp.menu.mapper;

import com.toni.FoodApp.menu.dtos.*;
import com.toni.FoodApp.menu.entity.OptionGroup;
import com.toni.FoodApp.menu.entity.OptionVariant;
import com.toni.FoodApp.restaurant.entity.BranchOptionConfig;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class MenuMapper {
    private final ModelMapper modelMapper;
    public OptionGroupDTO mapToOptionGroupDto(OptionGroup option, List<VariantDTO> variants) {
        return
                OptionGroupDTO.builder()
                        .isDeleted(option.isDeleted())
                        .name(option.getName())
                        .id(option.getId())
                        .minSelection(option.getMinSelection())
                        .maxSelection(option.getMaxSelection())
                        .variants(variants)
                        .build();
    }

    public OptionGroupDTO mapToOptionGroupDtoFromFlatDTO(OptionGroup option, List<MenuOptionFlatDTO> allVariantsForMenu) {

        return
                OptionGroupDTO.builder()
                        .isDeleted(option.isDeleted())
                        .name(option.getName())
                        .id(option.getId())
                        .minSelection(option.getMinSelection())
                        .maxSelection(option.getMaxSelection())
                        .variants(mapToVariantsFromDbResults(
                                allVariantsForMenu.stream()
                                        .filter(variant ->
                                                Objects.equals(variant.getGroupId(), option.getId())).toList()))
                        .build();
    }
    public OptionGroup mapToOptionGroup(OptionGroupDTO optionDTO, Restaurant restaurant){
        OptionGroup optionGroup = OptionGroup.builder()
                .restaurant(restaurant)
                .name(optionDTO.getName())
                .maxSelection(optionDTO.getMaxSelection())
                .minSelection(optionDTO.getMinSelection())
                .build();
        List<OptionVariant> variants = optionDTO.getVariants().stream()
                .map(variantDto -> OptionVariant.builder()
                        .name(variantDto.getName())
                        .recommendedPrice(variantDto.getRecommendedPrice())
                        .group(optionGroup)
                        .build())
                .toList();
        optionGroup.setVariants(variants);
        return optionGroup;
    }
    public BranchOptionGroupDto mapToBranchGroupDto(OptionGroup group, Map<Long, BranchOptionConfig> configMap) {
        List<BranchOptionVariantDto> variantDtos = group.getVariants().stream()
                .map(variant -> {
                    BranchOptionConfig config = configMap.get(variant.getId());
                    boolean isOverwritten = (config != null);
                    boolean isAvailable = ! isOverwritten || !Boolean.FALSE.equals(config.getIsAvailable());
                    BigDecimal effectivePrice = isOverwritten && config.getPriceOverride() != null
                            ? config.getPriceOverride()
                            : variant.getRecommendedPrice();

                    return BranchOptionVariantDto.builder()
                            .variantId(variant.getId())
                            .name(variant.getName())
                            .globalPrice(variant.getRecommendedPrice()) // Useful for UI comparison
                            .price(effectivePrice)
                            .isAvailable(isAvailable)
                            .isOverwritten(isOverwritten)
                            .build();
                })
                .toList();

        return BranchOptionGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .minSelection(group.getMinSelection())
                .maxSelection(group.getMaxSelection())
                .variants(variantDtos)
                .build();
    }

    public List<VariantDTO> mapToListVariantDto(List<OptionVariant> variants){
        return variants.stream().map(this::mapToVariantDTO).toList();
    }
    public VariantDTO mapToVariantDTO(OptionVariant variant){
        return VariantDTO.builder()
                .id(variant.getId())
                .isAvailable(true)
                .name(variant.getName())
                .isDeleted(variant.isDeleted())
                .recommendedPrice(variant.getRecommendedPrice())
                .build();
    }

    public List<VariantDTO> mapToVariantsFromDbResults(List<MenuOptionFlatDTO> variants){
    /*
        Documentation: This map is mainly used for mapping the menuOptionFlatDto a dto that is returned by a
        query in findAvailableOptionsRaw. THis method accept two parameters branchid and menu id to give all available,
        non deleted variants within a big dto with other fields. Maping this is very important and this replaces the
        depricated getEffectivePrice method in menuService which is very slow. The reason is slow is latency becasue the method
        requires many database calls while this way is only one call per menu.
     */
        if (variants == null || variants.isEmpty()) {
            return new ArrayList<>();
        }
        return variants.stream()
                .map(flatDto -> {
                    VariantDTO variant = new VariantDTO();
                    variant.setId(flatDto.getVariantId());
                    variant.setName(flatDto.getVariantName());
                    variant.setRecommendedPrice(flatDto.getEffectivePrice());
                    variant.setDeleted(false);
                    variant.setIsAvailable(true);
                    return variant;
                })
                .collect(Collectors.toList());
    }

    public List<OptionGroupDTO> mapFlatOptionsToOptionGroupDtos(List<MenuOptionFlatDTO> flatOptions) {
        if (flatOptions == null || flatOptions.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, OptionGroupDTO> groupMap = new LinkedHashMap<>();

        for (MenuOptionFlatDTO flatDto : flatOptions) {

            // 1. Get or create the OptionGroupDTO
            OptionGroupDTO group = groupMap.computeIfAbsent(flatDto.getGroupId(), id ->
                    OptionGroupDTO.builder()
                            .id(flatDto.getGroupId())
                            // Note: Make sure to map your group name/selections.
                            // Adjust the getters if your FlatDTO names differ slightly!
                            .name(flatDto.getGroupName())
                            .minSelection(flatDto.getMinSelection())
                            .maxSelection(flatDto.getMaxSelection())
                            .isDeleted(false) // Assuming false since the DB query filters for available/non-deleted
                            .variants(new ArrayList<>())
                            .build()
            );

            // 2. Create the VariantDTO
            VariantDTO variant = VariantDTO.builder()
                    .id(flatDto.getVariantId())
                    .name(flatDto.getVariantName())
                    .recommendedPrice(flatDto.getEffectivePrice())
                    .isAvailable(true)
                    .isDeleted(false)
                    .build();

            // 3. Attach variant to the group
            group.getVariants().add(variant);
        }

        return new ArrayList<>(groupMap.values());
    }
}
