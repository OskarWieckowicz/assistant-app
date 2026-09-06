package com.assistantapp.countrymcp.client.dto;

import java.util.List;

public record RestCountriesResponse(Data data) {

	public record Data(List<RestCountry> objects) {
	}
}
