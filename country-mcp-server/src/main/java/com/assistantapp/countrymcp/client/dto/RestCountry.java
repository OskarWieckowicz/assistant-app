package com.assistantapp.countrymcp.client.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RestCountry(
		Names names,
		List<Capital> capitals,
		String region,
		String subregion,
		Long population,
		List<Currency> currencies,
		List<Language> languages,
		Area area,
		Cars cars,
		@JsonProperty("calling_codes") List<String> callingCodes,
		Memberships memberships) {

	public record Currency(String code, String name, String symbol) {}

	public record Language(String name) {}

	public record Area(Double kilometers) {}

	public record Cars(@JsonProperty("driving_side") String drivingSide) {}

	public record Memberships(Boolean eu, Boolean schengen) {}

	public record Names(
			String common,
			String official) {
	}

	public record Capital(
			String name,
			Boolean primary) {
	}
}
