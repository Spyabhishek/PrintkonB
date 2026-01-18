package com.printkon.pdp.cms.repositories;

import com.printkon.pdp.cms.models.LandingStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandingStatRepository extends JpaRepository<LandingStat, Long> {
	List<LandingStat> findByEnabledTrueOrderByOrderIndexAsc();
}
