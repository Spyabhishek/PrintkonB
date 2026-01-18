package com.printkon.pdp.order.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.printkon.pdp.order.models.OrderEvent;

import java.util.List;
import java.util.Optional;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
	List<OrderEvent> findByOrderIdOrderByCreatedAtDesc(Long orderId);

	// In OrderEventRepository
	List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);

	Optional<OrderEvent> findFirstByOrderIdAndEventTypeOrderByCreatedAtAsc(Long orderId, String eventType);
}
