package com.printkon.pdp.cms.repositories;

import com.printkon.pdp.cms.models.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
	List<Testimonial> findByEnabledTrueOrderByOrderIndexAsc();
}
