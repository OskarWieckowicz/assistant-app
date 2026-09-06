package com.assistantapp.countrymcp.country;

public record CountryDetails(
		String name,
		String officialName,
		String capital,
		String region,
		String subregion,
		Long population) {
}
