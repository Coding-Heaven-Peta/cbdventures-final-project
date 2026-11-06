package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;
import za.ac.richfield.cbdventures.domain.enums.ActivityType;

import java.time.Instant;

/**
 * USER_ACTIVITY - a lightweight trail of what a signed-in visitor looked at.
 *
 * <p>Adopted from the group's database design: instead of inferring interest from
 * reviews alone, the platform records profile views and searches. The
 * recommendation engine then weights recent activity, and the home screen can
 * show "recently viewed".</p>
 *
 * <p>Deleting a user removes their activity ({@code ON DELETE CASCADE}); deleting
 * a listing keeps the history by nulling the reference
 * ({@code ON DELETE SET NULL}), so popularity counts survive a business leaving.</p>
 */
@Entity
@Table(name = "user_activity", indexes = {
        @Index(name = "idx_activity_user", columnList = "user_id"),
        @Index(name = "idx_activity_business", columnList = "business_id"),
        @Index(name = "idx_activity_created", columnList = "created_at")
})
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Null when the activity is not tied to a listing (e.g. a search). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserActivity() {
    }

    public UserActivity(User user, Business business, ActivityType type) {
        this.user = user;
        this.business = business;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
