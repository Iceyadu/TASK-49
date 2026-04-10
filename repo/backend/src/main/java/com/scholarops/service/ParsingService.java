package com.scholarops.service;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
public class ParsingService {

    private static final Logger logger = LoggerFactory.getLogger(ParsingService.class);

    /**
     * Extracts content from HTML using an XPath expression via Jsoup.
     * Jsoup uses its own selectXpath method for XPath-like queries.
     *
     * @param html       the HTML content
     * @param expression the XPath expression
     * @return list of extracted text values
     */
    public List<String> extractWithXPath(String html, String expression) {
        if (html == null || html.isBlank() || expression == null || expression.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Document document = Jsoup.parse(html);
            Elements elements = document.selectXpath(expression);
            List<String> results = new ArrayList<>();
            for (Element element : elements) {
                results.add(element.text());
            }
            return results;
        } catch (Exception e) {
            logger.error("XPath extraction failed for expression '{}': {}", expression, e.getMessage());
            throw new IllegalArgumentException("Invalid XPath expression: " + expression, e);
        }
    }

    /**
     * Extracts content from HTML using a CSS selector via Jsoup.
     *
     * @param html     the HTML content
     * @param selector the CSS selector
     * @return list of extracted text values
     */
    public List<String> extractWithCssSelector(String html, String selector) {
        if (html == null || html.isBlank() || selector == null || selector.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Document document = Jsoup.parse(html);
            Elements elements = document.select(selector);
            List<String> results = new ArrayList<>();
            for (Element element : elements) {
                results.add(element.text());
            }
            return results;
        } catch (Exception e) {
            logger.error("CSS selector extraction failed for '{}': {}", selector, e.getMessage());
            throw new IllegalArgumentException("Invalid CSS selector: " + selector, e);
        }
    }

    /**
     * Extracts content from text using a Java regular expression pattern.
     * Returns all capturing group 1 matches, or full matches if no groups defined.
     *
     * @param content the text content
     * @param pattern the regex pattern
     * @return list of matched strings
     */
    public List<String> extractWithRegex(String content, String pattern) {
        if (content == null || content.isBlank() || pattern == null || pattern.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Pattern compiled = Pattern.compile(pattern, Pattern.DOTALL | Pattern.MULTILINE);
            Matcher matcher = compiled.matcher(content);
            List<String> results = new ArrayList<>();

            while (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    results.add(matcher.group(1));
                } else {
                    results.add(matcher.group());
                }
            }
            return results;
        } catch (PatternSyntaxException e) {
            logger.error("Regex extraction failed for pattern '{}': {}", pattern, e.getMessage());
            throw new IllegalArgumentException("Invalid regex pattern: " + pattern, e);
        }
    }

    /**
     * Extracts content from JSON using a JsonPath expression.
     *
     * @param json the JSON content
     * @param path the JsonPath expression
     * @return list of extracted values as strings
     */
    public List<String> extractWithJsonPath(String json, String path) {
        if (json == null || json.isBlank() || path == null || path.isBlank()) {
            return Collections.emptyList();
        }

        try {
            Object result = JsonPath.read(json, path);
            List<String> results = new ArrayList<>();

            if (result instanceof List<?>) {
                for (Object item : (List<?>) result) {
                    results.add(item != null ? item.toString() : "");
                }
            } else if (result != null) {
                results.add(result.toString());
            }
            return results;
        } catch (PathNotFoundException e) {
            logger.warn("JsonPath '{}' not found in JSON content", path);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("JsonPath extraction failed for '{}': {}", path, e.getMessage());
            throw new IllegalArgumentException("Invalid JsonPath expression: " + path, e);
        }
    }

    /**
     * Dispatches to the correct extraction method based on the specified method.
     *
     * @param content        the content to extract from
     * @param method         the extraction method (XPATH, CSS_SELECTOR, REGEX, JSON_PATH)
     * @param ruleDefinition the rule/expression/pattern to apply
     * @return list of extracted values
     */
    public List<String> extract(String content, String method, String ruleDefinition) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Extraction method must not be blank");
        }

        switch (method.toUpperCase()) {
            case "XPATH":
                return extractWithXPath(content, ruleDefinition);
            case "CSS_SELECTOR":
            case "CSS":
                return extractWithCssSelector(content, ruleDefinition);
            case "REGEX":
                return extractWithRegex(content, ruleDefinition);
            case "JSON_PATH":
            case "JSONPATH":
                return extractWithJsonPath(content, ruleDefinition);
            default:
                throw new IllegalArgumentException("Unsupported extraction method: " + method);
        }
    }
}
