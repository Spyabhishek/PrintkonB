package com.printkon.pdp.common.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.Map;

@Converter
public class JsonAttributeConverter implements AttributeConverter<Map<String, Object>, String> {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, Object> attribute) {
		if (attribute == null || attribute.isEmpty())
			return null;
		try {
			return mapper.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to convert map to JSON string", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty())
			return Collections.emptyMap();
		try {
			return mapper.readValue(dbData, Map.class);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to convert JSON string to map", e);
		}
	}
}
