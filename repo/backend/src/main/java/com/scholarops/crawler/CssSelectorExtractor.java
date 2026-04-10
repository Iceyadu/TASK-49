package com.scholarops.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CssSelectorExtractor {

    public List<String> extract(String html, String selector) {
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select(selector);
        List<String> results = new ArrayList<>();
        for (Element element : elements) {
            results.add(element.text());
        }
        return results;
    }

    public List<String> extractAttributes(String html, String selector, String attribute) {
        Document doc = Jsoup.parse(html);
        Elements elements = doc.select(selector);
        List<String> results = new ArrayList<>();
        for (Element element : elements) {
            String attrValue = element.attr(attribute);
            if (!attrValue.isEmpty()) {
                results.add(attrValue);
            }
        }
        return results;
    }

    public String extractFirst(String html, String selector) {
        Document doc = Jsoup.parse(html);
        Element element = doc.selectFirst(selector);
        return element != null ? element.text() : null;
    }
}
