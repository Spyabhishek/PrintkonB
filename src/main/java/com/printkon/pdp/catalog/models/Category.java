package com.printkon.pdp.catalog.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_category_id", columnList = "category_id", unique = true),
    @Index(name = "idx_category_name", columnList = "name"),
    @Index(name = "idx_category_slug", columnList = "slug"),
    @Index(name = "idx_category_created_at", columnList = "created_at"),
    @Index(name = "idx_category_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category ID is required")
    @Size(min = 8, max = 8, message = "Category ID must be exactly 8 characters")
    @Column(name = "category_id", nullable = false, unique = true, length = 8)
    private String categoryId;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(min = 2, max = 60, message = "Slug must be between 2 and 60 characters")
    @Column(nullable = false, unique = true)
    private String slug;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id", foreignKey = @ForeignKey(name = "fk_category_parent"))
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> subCategories = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void generateCategoryId() {
        if (this.categoryId == null) {
            this.categoryId = generateCustomCategoryId();
        }
        if (this.slug == null && this.name != null) {
            this.slug = generateSlug(this.name);
        }
    }

    @PreUpdate
    protected void updateSlug() {
        if (this.slug == null && this.name != null) {
            this.slug = generateSlug(this.name);
        }
    }

    private String generateCustomCategoryId() {
        String prefix = "CAT";
        String random = String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
        return prefix + random;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    // Business logic methods
    public boolean hasSubCategories() {
        return subCategories != null && !subCategories.isEmpty();
    }

    public boolean isRootCategory() {
        return parentCategory == null;
    }

    public void addSubCategory(Category subCategory) {
        if (this.subCategories == null) {
            this.subCategories = new ArrayList<>();
        }
        this.subCategories.add(subCategory);
        subCategory.setParentCategory(this);
    }
}