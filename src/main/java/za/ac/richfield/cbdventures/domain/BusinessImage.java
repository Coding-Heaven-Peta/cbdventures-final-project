package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;

/**
 * BUSINESS_IMAGE entity - a business can showcase zero or many photos, but
 * exactly one must be flagged as the primary display thumbnail
 * (Phase 3 media rules).
 */
@Entity
@Table(name = "business_image")
public class BusinessImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "caption", length = 160)
    private String caption;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    public BusinessImage() {
    }

    public BusinessImage(String imageUrl, String caption, boolean primary) {
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.primary = primary;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
