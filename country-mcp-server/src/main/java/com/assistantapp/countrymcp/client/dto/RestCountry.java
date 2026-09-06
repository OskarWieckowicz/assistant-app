package com.assistantapp.countrymcp.client.dto;

import java.util.List;

public record RestCountry(
		Names names,
		List<Capital> capitals,
		String region,
		String subregion,
		Long population) {

	public record Names(
			String common,
			String official) {
	}

	public record Capital(
			String name,
			Boolean primary) {
	}
}
