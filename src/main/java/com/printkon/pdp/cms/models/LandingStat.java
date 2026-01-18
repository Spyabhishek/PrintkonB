package com.printkon.pdp.cms.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "landing_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandingStat {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String label;

	private String value;

	@Column(name = "is_dynamic")
	@Builder.Default
	private Boolean isDynamic = false;

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
