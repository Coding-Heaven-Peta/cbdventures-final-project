package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * USER_PREFERENCE - stores the preferences the recommendation engine learns from.
 * Supports FR-04: "Application must display personalised recommendations based on
 * saved preferences and user activity."
 */
@Entity
@Table(name = "user_preference")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "max_price_level", length = 10)
    private PriceLevel maxPriceLevel;

    @Column(name = "max_distance_km")
    private Double maxDistanceKm;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "preference_category",
            joinColumns = @JoinColumn(name = "preference_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> preferredCategories = new LinkedHashSet<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserPreference() {
    }

    public UserPreference(User user) {
        this.user = user;
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

    public PriceLevel getMaxPriceLevel() {
        return maxPriceLevel;
    }

    public void setMaxPriceLevel(PriceLevel maxPriceLevel) {
        this.maxPriceLevel = maxPriceLevel;
    }

    public Double getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public void setMaxDistanceKm(Double maxDistanceKm) {
        this.maxDistanceKm = maxDistanceKm;
    }

    public Set<Category> getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(Set<Category> preferredCategories) {
        this.preferredCategories = preferredCategories;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
