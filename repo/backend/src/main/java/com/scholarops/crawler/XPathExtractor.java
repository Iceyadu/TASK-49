package com.scholarops.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class XPathExtractor {

    public List<String> extract(String html, String xpathExpression) {
        Document doc = Jsoup.parse(html);
        List<String> results = new ArrayList<>();

        String cssEquivalent = convertXPathToCssSelector(xpathExpression);
        Elements elements = doc.select(cssEquivalent);

        for (Element element : elements) {
            if (xpathExpression.contains("@")) {
                String attrName = xpathExpression.substring(xpathExpression.lastIndexOf("@") + 1);
                attrName = attrName.replaceAll("[\\[\\]']", "");
                results.add(element.attr(attrName));
            } else {
                results.add(element.text());
            }
        }
        return results;
    }

    private String convertXPathToCssSelector(String xpath) {
        String css = xpath;
        css = css.replace("//", "");
        css = css.replaceAll("/", " > ");
        css = css.replaceAll("\\[@class='([^']+)'\\]", ".$1");
        css = css.replaceAll("\\[@id='([^']+)'\\]", "#$1");
        css = css.replaceAll("\\[@([^=]+)='([^']+)'\\]", "[$1='$2']");
        css = css.replaceAll("\\[\\d+\\]", "");
        css = css.replaceAll("/@\\w+", "");
        return css.trim();
    }
}
