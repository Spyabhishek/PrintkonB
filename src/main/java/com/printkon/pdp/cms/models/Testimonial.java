package com.printkon.pdp.cms.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "testimonials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Testimonial {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String author;

	private String role;

	@Column(length = 2000)
	private String quote;

	@Column(name = "image_url", length = 1000)
	private String imageUrl;

	@Column(nullable = false)
	@Builder.Default
	private Boolean enabled = true;

	@Column(name = "order_index")
	@Builder.Default
	private Integer orderIndex = 0;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
