package com.printkon.pdp.order.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.printkon.pdp.order.models.Order;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.common.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByCustomer(User customer);

	List<Order> findByStatus(OrderStatus status);

	List<Order> findByAssignedOperator(User operator);

	List<Order> findByAssignedOperatorId(Long operatorId);

	// NEW: Find by orderId (external identifier)
	Optional<Order> findByOrderId(String orderId);

	// NEW: Find by orderId with customer eager loading
	@Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.orderId = :orderId")
	Optional<Order> findByOrderIdWithCustomer(@Param("orderId") String orderId);

	// NEW: Find by orderId with operator eager loading
	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.assignedOperator WHERE o.orderId = :orderId")
	Optional<Order> findByOrderIdWithOperator(@Param("orderId") String orderId);

	@Query("SELECT o FROM Order o WHERE o.status = :status AND o.assignedOperator = :operator")
	List<Order> findByStatusAndAssignedOperator(@Param("status") OrderStatus status, @Param("operator") User operator);

	List<Order> findByCustomerOrderByCreatedAtDesc(User customer);

	// NEW: Check if orderId exists
	boolean existsByOrderId(String orderId);
}