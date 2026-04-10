package com.scholarops.crawler;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RegexExtractor {

    public List<String> extract(String content, String regex) {
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            if (matcher.groupCount() > 0) {
                results.add(matcher.group(1));
            } else {
                results.add(matcher.group());
            }
        }
        return results;
    }

    public String extractFirst(String content, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
        }
        return null;
    }

    public List<List<String>> extractAllGroups(String content, String regex) {
        List<List<String>> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            List<String> groups = new ArrayList<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                groups.add(matcher.group(i));
            }
            results.add(groups);
        }
        return results;
    }
}
