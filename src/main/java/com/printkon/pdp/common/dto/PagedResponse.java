package com.printkon.pdp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagedResponse<T> {
	private List<T> content;
	private int pageNumber;
	private int pageSize;
	private long totalElements;
	private int totalPages;
	private boolean last;
	private boolean first;

	// Helper method to create PagedResponse from Spring Page
	public static <T> PagedResponse<T> of(Page<T> page) {
		return PagedResponse.<T>builder().content(page.getContent()).pageNumber(page.getNumber())
				.pageSize(page.getSize()).totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
				.last(page.isLast()).first(page.isFirst()).build();
	}
}