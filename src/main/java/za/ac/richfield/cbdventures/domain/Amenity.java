package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;

/**
 * AMENITY entity - standardised operational features (Free Wi-Fi, Backup Power,
 * Student Specials, Quiet Atmosphere...). Extracted from Business during 3NF
 * normalisation to remove the repeating group "amenities = Wi-Fi, Backup Power".
 */
@Entity
@Table(name = "amenity", uniqueConstraints = @UniqueConstraint(columnNames = "amenity_name"))
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "amenity_id")
    private Long id;

    /** Optional grouping category, as specified in the Phase 3 data dictionary. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "amenity_name", nullable = false, length = 50)
    private String name;

    @Column(name = "icon_key", length = 40)
    private String iconKey;

    public Amenity() {
    }

    public Amenity(Category category, String name, String iconKey) {
        this.category = category;
        this.name = name;
        this.iconKey = iconKey;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }
}
