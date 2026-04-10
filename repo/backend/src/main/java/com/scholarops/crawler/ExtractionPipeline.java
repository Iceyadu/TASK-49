package com.scholarops.crawler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExtractionPipeline {

    private static final Logger logger = LoggerFactory.getLogger(ExtractionPipeline.class);

    private final XPathExtractor xPathExtractor;
    private final CssSelectorExtractor cssSelectorExtractor;
    private final RegexExtractor regexExtractor;
    private final JsonPathExtractor jsonPathExtractor;
    private final ObjectMapper objectMapper;

    public ExtractionPipeline(XPathExtractor xPathExtractor, CssSelectorExtractor cssSelectorExtractor,
                              RegexExtractor regexExtractor, JsonPathExtractor jsonPathExtractor,
                              ObjectMapper objectMapper) {
        this.xPathExtractor = xPathExtractor;
        this.cssSelectorExtractor = cssSelectorExtractor;
        this.regexExtractor = regexExtractor;
        this.jsonPathExtractor = jsonPathExtractor;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(String content, String extractionMethod,
                                        String ruleDefinitionJson, String fieldMappingsJson) {
        Map<String, Object> extractedData = new HashMap<>();

        try {
            Map<String, String> rules = objectMapper.readValue(ruleDefinitionJson,
                    new TypeReference<Map<String, String>>() {});
            Map<String, String> fieldMappings = objectMapper.readValue(fieldMappingsJson,
                    new TypeReference<Map<String, String>>() {});

            for (Map.Entry<String, String> rule : rules.entrySet()) {
                String fieldName = rule.getKey();
                String expression = rule.getValue();
                String targetField = fieldMappings.getOrDefault(fieldName, fieldName);

                List<String> results = switch (extractionMethod.toUpperCase()) {
                    case "XPATH" -> xPathExtractor.extract(content, expression);
                    case "CSS_SELECTOR" -> cssSelectorExtractor.extract(content, expression);
                    case "REGEX" -> regexExtractor.extract(content, expression);
                    case "JSONPATH" -> jsonPathExtractor.extractAsStrings(content, expression);
                    default -> throw new IllegalArgumentException("Unsupported extraction method: " + extractionMethod);
                };

                if (results.size() == 1) {
                    extractedData.put(targetField, results.get(0));
                } else if (!results.isEmpty()) {
                    extractedData.put(targetField, results);
                }
            }
        } catch (Exception e) {
            logger.error("Extraction pipeline failed: {}", e.getMessage());
            throw new RuntimeException("Extraction pipeline execution failed", e);
        }

        return extractedData;
    }

    public Map<String, Object> validateTypes(Map<String, Object> data, String typeValidationsJson) {
        if (typeValidationsJson == null || typeValidationsJson.isBlank()) {
            return data;
        }

        try {
            Map<String, String> typeValidations = objectMapper.readValue(typeValidationsJson,
                    new TypeReference<Map<String, String>>() {});

            Map<String, Object> validated = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();
                String expectedType = typeValidations.get(field);

                if (expectedType != null && value != null) {
                    validated.put(field, coerceType(value.toString(), expectedType));
                } else {
                    validated.put(field, value);
                }
            }
            return validated;
        } catch (Exception e) {
            logger.error("Type validation failed: {}", e.getMessage());
            return data;
        }
    }

    private Object coerceType(String value, String type) {
        return switch (type.toLowerCase()) {
            case "string" -> value;
            case "integer", "int" -> Integer.parseInt(value.replaceAll("[^\\d-]", ""));
            case "long" -> Long.parseLong(value.replaceAll("[^\\d-]", ""));
            case "double", "decimal" -> Double.parseDouble(value.replaceAll("[^\\d.\\-]", ""));
            case "boolean" -> Boolean.parseBoolean(value);
            default -> value;
        };
    }
}
