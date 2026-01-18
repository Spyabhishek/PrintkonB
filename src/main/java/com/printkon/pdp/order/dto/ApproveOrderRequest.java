package com.printkon.pdp.order.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ApproveOrderRequest {
	private Long operatorId;
	private LocalDate deadline;
	private String adminNotes; // Optional notes from admin
}