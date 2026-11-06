package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;

/**
 * CATEGORY entity - the five approved discovery classifications.
 * Categories are data, not code (NFR: Maintainability - "adding a new business
 * category should mean adding data, not rewriting code").
 */
@Entity
@Table(name = "category", uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category_name", nullable = false, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, length = 80)
    private String slug;

    @Column(name = "description", length = 255)
    private String description;

    /** Icon key resolved by the front-end to an inline SVG glyph. */
    @Column(name = "icon_key", length = 40)
    private String iconKey;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public Category() {
    }

    public Category(String name, String slug, String description, String iconKey, Integer sortOrder) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.iconKey = iconKey;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
