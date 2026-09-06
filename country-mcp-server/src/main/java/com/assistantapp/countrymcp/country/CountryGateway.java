package com.assistantapp.countrymcp.country;

import java.util.Optional;

public interface CountryGateway {

	Optional<CountryDetails> findByName(String countryName);
}
