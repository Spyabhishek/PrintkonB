package com.printkon.pdp.role.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.printkon.pdp.common.enums.RequestStatus;
import com.printkon.pdp.role.models.RoleUpgradeRequest;
import com.printkon.pdp.user.models.User;

public interface RoleUpgradeRequestRepository extends JpaRepository<RoleUpgradeRequest, Long> {

	List<RoleUpgradeRequest> findByStatus(RequestStatus status);

	List<RoleUpgradeRequest> findByUser(User user);
}
