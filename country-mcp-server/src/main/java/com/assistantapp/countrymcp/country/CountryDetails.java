package com.assistantapp.countrymcp.country;

import java.util.List;

public record CountryDetails(
		String name,
		String officialName,
		String capital,
		String region,
		String subregion,
		Long population,
		List<Currency> currencies,
		List<String> languages,
		Double areaSquareKilometers,
		String drivingSide,
		List<String> callingCodes,
		Boolean euMember,
		Boolean schengenMember) {

	public record Currency(String code, String name, String symbol) {}
}
