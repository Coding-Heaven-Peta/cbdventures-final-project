package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.ac.richfield.cbdventures.domain.EventListing;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<EventListing, Long> {

    @Query("""
            select e from EventListing e
            join fetch e.business b
            join fetch b.category
            where b.status = :status and e.eventDateTime >= :from
            order by e.eventDateTime asc
            """)
    List<EventListing> findUpcomingForStatus(@Param("status") BusinessStatus status,
                                            @Param("from") LocalDateTime from);

    List<EventListing> findByBusinessIdOrderByEventDateTimeAsc(Long businessId);
}
