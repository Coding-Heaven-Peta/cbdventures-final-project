package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.richfield.cbdventures.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderBySortOrderAsc();

    Optional<Category> findBySlugIgnoreCase(String slug);

    Optional<Category> findByNameIgnoreCase(String name);
}
