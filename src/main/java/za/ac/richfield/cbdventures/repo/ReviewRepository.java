package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.ac.richfield.cbdventures.domain.Review;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

    Optional<Review> findByUserIdAndBusinessId(Long userId, Long businessId);

    long countByBusinessId(Long businessId);

    boolean existsByBusinessIdAndUserId(Long businessId, Long userId);

    /**
     * Aggregate rating per business in one query (avoids an N+1 problem when
     * rendering a result list). Returns rows of [businessId, averageRating, reviewCount].
     */
    @Query("""
            select r.business.id, avg(r.rating), count(r)
            from Review r
            where r.business.id in :businessIds
            group by r.business.id
            """)
    List<Object[]> summariseForBusinesses(@Param("businessIds") Collection<Long> businessIds);
}
