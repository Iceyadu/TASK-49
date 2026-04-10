package com.scholarops.crawler;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class JsonPathExtractor {

    private final Configuration config;

    public JsonPathExtractor() {
        this.config = Configuration.defaultConfiguration()
                .addOptions(Option.SUPPRESS_EXCEPTIONS)
                .addOptions(Option.ALWAYS_RETURN_LIST);
    }

    @SuppressWarnings("unchecked")
    public List<Object> extract(String json, String jsonPath) {
        try {
            Object result = JsonPath.using(config).parse(json).read(jsonPath);
            if (result instanceof List) {
                return (List<Object>) result;
            }
            return Collections.singletonList(result);
        } catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    public String extractFirstAsString(String json, String jsonPath) {
        List<Object> results = extract(json, jsonPath);
        if (!results.isEmpty() && results.get(0) != null) {
            return results.get(0).toString();
        }
        return null;
    }

    public List<String> extractAsStrings(String json, String jsonPath) {
        List<Object> results = extract(json, jsonPath);
        List<String> strings = new ArrayList<>();
        for (Object obj : results) {
            if (obj != null) {
                strings.add(obj.toString());
            }
        }
        return strings;
    }
}
