package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import za.ac.richfield.cbdventures.domain.Amenity;

import java.util.Collection;
import java.util.List;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    @EntityGraph(attributePaths = "category")
    List<Amenity> findAllByOrderByNameAsc();

    List<Amenity> findByNameIn(Collection<String> names);

    @EntityGraph(attributePaths = "category")
    List<Amenity> findByIdIn(Collection<Long> ids);
}
