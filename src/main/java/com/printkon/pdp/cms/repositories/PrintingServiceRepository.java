package com.printkon.pdp.cms.repositories;

import com.printkon.pdp.cms.models.PrintingServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintingServiceRepository extends JpaRepository<PrintingServiceEntity, Long> {
	List<PrintingServiceEntity> findByEnabledTrueOrderByOrderIndexAsc();
}
