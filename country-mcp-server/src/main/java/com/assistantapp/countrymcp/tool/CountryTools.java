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

        @McpTool(name = "get_country", description = """
                        Looks up country facts from REST Countries: common and official names, capital,
                        region, subregion, country population, currencies (code, name, symbol), languages,
                        area in square kilometers, driving side (left/right), international calling codes,
                        and EU and Schengen membership. Use for questions about these country facts.
                        Data describes the country, not individual cities. Null means unavailable, not false.
                        Currency data does not include exchange rates.
                        """, generateOutputSchema = true, annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
        public CountryDetails getCountry(
                        @McpToolParam(description = "Common country name in English, for example Poland or Germany. Translate the user's country name to English before calling.") String countryName) {
                log.info("Executing MCP tool get_country [countryName={}]", countryName);
                return countryGateway.findByName(countryName)
                                .orElseThrow(() -> new CountryNotFoundException(countryName));
        }
}
