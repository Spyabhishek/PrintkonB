package com.printkon.pdp.cms.repositories;

import com.printkon.pdp.cms.models.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
	List<Feature> findByEnabledTrueOrderByOrderIndexAsc();
}
