package com.assistantapp.countrymcp.country;

public class CountryNotFoundException extends RuntimeException {

	public CountryNotFoundException(String countryName) {
		super("Country not found: " + countryName);
	}
}
