package com.assistantapp.countrymcp.tool;

import com.assistantapp.countrymcp.country.CountryDetails;
import com.assistantapp.countrymcp.country.CountryNotFoundException;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.assistantapp.countrymcp.country.CountryGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CountryTools {

    private final CountryGateway countryGateway;

    @McpTool(name = "get_country",
            description = "Returns information about a country using its common name",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true))
    public CountryDetails getCountry(
            @McpToolParam(description = "Common country name, for example Germany") String countryName) {

        return countryGateway.findByName(countryName)
                .orElseThrow(() -> new CountryNotFoundException(countryName));
    }
}
