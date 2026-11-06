package za.ac.richfield.cbdventures.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.ac.richfield.cbdventures.domain.UserActivity;

import java.time.Instant;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    /** Most recent activity for one visitor, newest first. */
    List<UserActivity> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);

    /** Activity for one visitor since a cut-off (used to weight recommendations). */
    @Query("""
            select a from UserActivity a
            where a.user.id = :userId and a.createdAt >= :since
            order by a.createdAt desc
            """)
    List<UserActivity> findRecentForUser(@Param("userId") Long userId, @Param("since") Instant since);

    long countByBusinessId(Long businessId);
}
