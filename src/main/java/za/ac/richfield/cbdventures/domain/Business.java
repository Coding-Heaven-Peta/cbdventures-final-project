package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.domain.enums.PriceLevel;
import za.ac.richfield.cbdventures.domain.enums.ZoneRadius;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * BUSINESS entity - Phase 3 data dictionary.
 * owner_id -> USER (RESTRICT delete: active listings protect the account)
 * category_id -> CATEGORY (RESTRICT delete)
 * Amenities are a many-to-many relationship resolved by BUSINESS_AMENITY.
 */
@Entity
@Table(name = "business", indexes = {
        @Index(name = "idx_business_status", columnList = "status"),
        @Index(name = "idx_business_category", columnList = "category_id"),
        @Index(name = "idx_business_name", columnList = "name")
})
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_radius", nullable = false, length = 20)
    private ZoneRadius zoneRadius = ZoneRadius.PRIMARY_1KM;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_level", nullable = false, length = 10)
    private PriceLevel priceLevel = PriceLevel.R;

    /** Human readable hours, e.g. "Mon-Fri 07:00-17:00, Sat 08:00-13:00". */
    @Column(name = "operating_hours", length = 120)
    private String operatingHours;

    /**
     * Legacy single-window hours. Still accepted from older API clients and used
     * as the fallback when no per-day {@link #hours} rows exist.
     * New listings should send per-day hours instead.
     */
    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "whatsapp", length = 40)
    private String whatsapp;

    @Column(name = "website", length = 200)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BusinessStatus status = BusinessStatus.PENDING;

    @Column(name = "admin_note", length = 255)
    private String adminNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "business_amenity",
            joinColumns = @JoinColumn(name = "business_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id"))
    private Set<Amenity> amenities = new LinkedHashSet<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BusinessImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventListing> events = new ArrayList<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    /**
     * Opening hours, one row per weekday (authoritative source for "open now?").
     * CascadeType.ALL + orphanRemoval means replacing the week deletes the old rows.
     */
    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("dayOfWeek ASC")
    private List<BusinessHours> hours = new ArrayList<>();

    public Business() {
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addImage(BusinessImage image) {
        images.add(image);
        image.setBusiness(this);
    }

    public void addHours(BusinessHours row) {
        row.setBusiness(this);
        hours.add(row);
    }

    /** Replaces the whole week with the supplied rows. */
    public void replaceHours(List<BusinessHours> week) {
        hours.clear();
        if (week != null) {
            week.forEach(this::addHours);
        }
    }

    /** The row for the supplied weekday, or null when the owner never set one. */
    public BusinessHours hoursFor(DayOfWeek day) {
        return hours.stream().filter(row -> row.isFor(day)).findFirst().orElse(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public ZoneRadius getZoneRadius() {
        return zoneRadius;
    }

    public void setZoneRadius(ZoneRadius zoneRadius) {
        this.zoneRadius = zoneRadius;
    }

    public PriceLevel getPriceLevel() {
        return priceLevel;
    }

    public void setPriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(String operatingHours) {
        this.operatingHours = operatingHours;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public BusinessStatus getStatus() {
        return status;
    }

    public void setStatus(BusinessStatus status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<Amenity> amenities) {
        this.amenities = amenities;
    }

    public List<BusinessImage> getImages() {
        return images;
    }

    public void setImages(List<BusinessImage> images) {
        this.images = images;
    }

    public List<EventListing> getEvents() {
        return events;
    }

    public void setEvents(List<EventListing> events) {
        this.events = events;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public List<BusinessHours> getHours() {
        return hours;
    }

    public void setHours(List<BusinessHours> hours) {
        this.hours = hours;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }
}
