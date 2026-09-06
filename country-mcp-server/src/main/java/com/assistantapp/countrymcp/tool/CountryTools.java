package com.assistantapp.countrymcp.tool;

import com.assistantapp.countrymcp.country.CountryDetails;
import com.assistantapp.countrymcp.country.CountryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.assistantapp.countrymcp.country.CountryGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class CountryTools {

        private final CountryGateway countryGateway;

        @McpTool(name = "get_country", description = "Looks up a country's common name, official name, capital, region, subregion and population from REST Countries. Use for questions about these country facts.", generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
        public CountryDetails getCountry(
                        @McpToolParam(description = "Common country name in English, for example Poland or Germany. Translate the user's country name to English before calling.") String countryName) {
                log.info("Executing MCP tool get_country [countryName={}]", countryName);
                return countryGateway.findByName(countryName)
                                .orElseThrow(() -> new CountryNotFoundException(countryName));
        }
}
