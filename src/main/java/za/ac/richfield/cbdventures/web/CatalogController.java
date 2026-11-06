package za.ac.richfield.cbdventures.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ac.richfield.cbdventures.domain.Amenity;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.repo.AmenityRepository;
import za.ac.richfield.cbdventures.repo.BusinessRepository;
import za.ac.richfield.cbdventures.repo.CategoryRepository;
import za.ac.richfield.cbdventures.service.ZoneService;
import za.ac.richfield.cbdventures.web.dto.CatalogDtos;
import za.ac.richfield.cbdventures.web.dto.ZoneView;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference data used to build the filter panel and the five category tiles.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;
    private final BusinessRepository businessRepository;
    private final ZoneService zoneService;

    public CatalogController(CategoryRepository categoryRepository,
                             AmenityRepository amenityRepository,
                             BusinessRepository businessRepository,
                             ZoneService zoneService) {
        this.categoryRepository = categoryRepository;
        this.amenityRepository = amenityRepository;
        this.businessRepository = businessRepository;
        this.zoneService = zoneService;
    }

    @GetMapping("/categories")
    public List<CatalogDtos.CategoryView> categories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(category -> new CatalogDtos.CategoryView(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        category.getDescription(),
                        category.getIconKey(),
                        businessRepository.countByCategoryId(category.getId())))
                .toList();
    }

    @GetMapping("/amenities")
    public List<CatalogDtos.AmenityView> amenities() {
        return amenityRepository.findAllByOrderByNameAsc().stream()
                .map(this::toView)
                .toList();
    }

    @GetMapping("/meta/zone")
    public ZoneView zone() {
        return zoneService.view();
    }

    @GetMapping("/meta/price-levels")
    public List<Map<String, Object>> priceLevels() {
        return Arrays.stream(PriceLevel.values())
                .map(level -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("level", level.name());
                    entry.put("weight", level.weight());
                    entry.put("label", level.label());
                    return entry;
                })
                .toList();
    }

    /** Everything the filter panel needs in one request. */
    @GetMapping("/meta/filters")
    public Map<String, Object> filters() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("categories", categories());
        payload.put("amenities", amenities());
        payload.put("priceLevels", priceLevels());
        payload.put("zone", zoneService.view());
        payload.put("sorts", List.of(
                Map.of("id", "recommended", "label", "Recommended"),
                Map.of("id", "distance", "label", "Closest first"),
                Map.of("id", "price", "label", "Lowest price first"),
                Map.of("id", "rating", "label", "Highest rated"),
                Map.of("id", "name", "label", "Name (A-Z)")));
        return payload;
    }

    private CatalogDtos.AmenityView toView(Amenity amenity) {
        return new CatalogDtos.AmenityView(amenity.getId(), amenity.getName(), amenity.getIconKey(),
                amenity.getCategory() == null ? null : amenity.getCategory().getName());
    }
}
