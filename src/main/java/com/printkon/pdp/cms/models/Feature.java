package com.printkon.pdp.cms.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "features")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feature {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(length = 1000)
	private String description;

	@Column(name = "icon_url", length = 1000)
	private String iconUrl;

	@Column(name = "order_index")
	@Builder.Default
	private Integer orderIndex = 0;

	@Column(nullable = false)
	@Builder.Default
	private Boolean enabled = true;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
