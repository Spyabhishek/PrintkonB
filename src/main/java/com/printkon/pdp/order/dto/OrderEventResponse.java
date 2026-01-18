package com.printkon.pdp.order.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderEventResponse {
	private String eventType;
	private String message;
	private LocalDateTime createdAt;
	private String performedBy;
}