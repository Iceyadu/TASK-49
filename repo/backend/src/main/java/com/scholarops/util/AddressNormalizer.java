package com.scholarops.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AddressNormalizer {

    private static final Map<String, String> ABBREVIATIONS = new LinkedHashMap<>();
    private static final Map<String, String> STATE_ABBREVIATIONS = new LinkedHashMap<>();

    static {
        // Street type abbreviations (order matters for replacement)
        ABBREVIATIONS.put("\\bSt\\b\\.?", "Street");
        ABBREVIATIONS.put("\\bAve\\b\\.?", "Avenue");
        ABBREVIATIONS.put("\\bBlvd\\b\\.?", "Boulevard");
        ABBREVIATIONS.put("\\bDr\\b\\.?", "Drive");
        ABBREVIATIONS.put("\\bLn\\b\\.?", "Lane");
        ABBREVIATIONS.put("\\bRd\\b\\.?", "Road");
        ABBREVIATIONS.put("\\bCt\\b\\.?", "Court");
        ABBREVIATIONS.put("\\bPl\\b\\.?", "Place");
        ABBREVIATIONS.put("\\bPkwy\\b\\.?", "Parkway");
        ABBREVIATIONS.put("\\bCir\\b\\.?", "Circle");
        ABBREVIATIONS.put("\\bTer\\b\\.?", "Terrace");
        ABBREVIATIONS.put("\\bTrl\\b\\.?", "Trail");
        ABBREVIATIONS.put("\\bWay\\b\\.?", "Way");
        ABBREVIATIONS.put("\\bHwy\\b\\.?", "Highway");
        ABBREVIATIONS.put("\\bSq\\b\\.?", "Square");

        // Directional abbreviations
        ABBREVIATIONS.put("\\bN\\b\\.?\\s", "North ");
        ABBREVIATIONS.put("\\bS\\b\\.?\\s", "South ");
        ABBREVIATIONS.put("\\bE\\b\\.?\\s", "East ");
        ABBREVIATIONS.put("\\bW\\b\\.?\\s", "West ");
        ABBREVIATIONS.put("\\bNE\\b\\.?\\s", "Northeast ");
        ABBREVIATIONS.put("\\bNW\\b\\.?\\s", "Northwest ");
        ABBREVIATIONS.put("\\bSE\\b\\.?\\s", "Southeast ");
        ABBREVIATIONS.put("\\bSW\\b\\.?\\s", "Southwest ");

        // Unit abbreviations
        ABBREVIATIONS.put("\\bApt\\b\\.?", "Apartment");
        ABBREVIATIONS.put("\\bSte\\b\\.?", "Suite");
        ABBREVIATIONS.put("\\bFl\\b\\.?", "Floor");

        // US state abbreviations
        STATE_ABBREVIATIONS.put("AL", "Alabama");
        STATE_ABBREVIATIONS.put("AK", "Alaska");
        STATE_ABBREVIATIONS.put("AZ", "Arizona");
        STATE_ABBREVIATIONS.put("AR", "Arkansas");
        STATE_ABBREVIATIONS.put("CA", "California");
        STATE_ABBREVIATIONS.put("CO", "Colorado");
        STATE_ABBREVIATIONS.put("CT", "Connecticut");
        STATE_ABBREVIATIONS.put("DE", "Delaware");
        STATE_ABBREVIATIONS.put("FL", "Florida");
        STATE_ABBREVIATIONS.put("GA", "Georgia");
        STATE_ABBREVIATIONS.put("HI", "Hawaii");
        STATE_ABBREVIATIONS.put("ID", "Idaho");
        STATE_ABBREVIATIONS.put("IL", "Illinois");
        STATE_ABBREVIATIONS.put("IN", "Indiana");
        STATE_ABBREVIATIONS.put("IA", "Iowa");
        STATE_ABBREVIATIONS.put("KS", "Kansas");
        STATE_ABBREVIATIONS.put("KY", "Kentucky");
        STATE_ABBREVIATIONS.put("LA", "Louisiana");
        STATE_ABBREVIATIONS.put("ME", "Maine");
        STATE_ABBREVIATIONS.put("MD", "Maryland");
        STATE_ABBREVIATIONS.put("MA", "Massachusetts");
        STATE_ABBREVIATIONS.put("MI", "Michigan");
        STATE_ABBREVIATIONS.put("MN", "Minnesota");
        STATE_ABBREVIATIONS.put("MS", "Mississippi");
        STATE_ABBREVIATIONS.put("MO", "Missouri");
        STATE_ABBREVIATIONS.put("MT", "Montana");
        STATE_ABBREVIATIONS.put("NE", "Nebraska");
        STATE_ABBREVIATIONS.put("NV", "Nevada");
        STATE_ABBREVIATIONS.put("NH", "New Hampshire");
        STATE_ABBREVIATIONS.put("NJ", "New Jersey");
        STATE_ABBREVIATIONS.put("NM", "New Mexico");
        STATE_ABBREVIATIONS.put("NY", "New York");
        STATE_ABBREVIATIONS.put("NC", "North Carolina");
        STATE_ABBREVIATIONS.put("ND", "North Dakota");
        STATE_ABBREVIATIONS.put("OH", "Ohio");
        STATE_ABBREVIATIONS.put("OK", "Oklahoma");
        STATE_ABBREVIATIONS.put("OR", "Oregon");
        STATE_ABBREVIATIONS.put("PA", "Pennsylvania");
        STATE_ABBREVIATIONS.put("RI", "Rhode Island");
        STATE_ABBREVIATIONS.put("SC", "South Carolina");
        STATE_ABBREVIATIONS.put("SD", "South Dakota");
        STATE_ABBREVIATIONS.put("TN", "Tennessee");
        STATE_ABBREVIATIONS.put("TX", "Texas");
        STATE_ABBREVIATIONS.put("UT", "Utah");
        STATE_ABBREVIATIONS.put("VT", "Vermont");
        STATE_ABBREVIATIONS.put("VA", "Virginia");
        STATE_ABBREVIATIONS.put("WA", "Washington");
        STATE_ABBREVIATIONS.put("WV", "West Virginia");
        STATE_ABBREVIATIONS.put("WI", "Wisconsin");
        STATE_ABBREVIATIONS.put("WY", "Wyoming");
        STATE_ABBREVIATIONS.put("DC", "District of Columbia");
    }

    private AddressNormalizer() {
        // Utility class
    }

    /**
     * Normalizes a raw US address string.
     * Expands common abbreviations and formats as: Street, City, State ZIP.
     *
     * @param rawAddress the raw address string
     * @return normalized address string
     */
    public static String normalize(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return "";
        }

        String address = rawAddress.trim();

        // Normalize multiple spaces
        address = address.replaceAll("\\s{2,}", " ");

        // Expand street type and directional abbreviations
        for (Map.Entry<String, String> entry : ABBREVIATIONS.entrySet()) {
            address = address.replaceAll("(?i)" + entry.getKey(), entry.getValue());
        }

        // Try to parse and reformat as: Street, City, State ZIP
        // Common patterns: "123 Main St, Springfield, IL 62701"
        // or "123 Main St Springfield IL 62701"
        Pattern zipPattern = Pattern.compile(
                "^(.+?),\\s*(.+?),\\s*([A-Za-z]{2})\\s+(\\d{5}(?:-\\d{4})?)$");
        Matcher matcher = zipPattern.matcher(address);
        if (matcher.matches()) {
            String street = matcher.group(1).trim();
            String city = matcher.group(2).trim();
            String stateCode = matcher.group(3).trim().toUpperCase();
            String zip = matcher.group(4).trim();

            String stateName = STATE_ABBREVIATIONS.getOrDefault(stateCode, stateCode);
            return String.format("%s, %s, %s %s", street, city, stateName, zip);
        }

        // Try pattern without commas: "123 Main St Springfield IL 62701"
        Pattern noCommaPattern = Pattern.compile(
                "^(\\d+.+?)\\s+([A-Z][a-z]+(?:\\s[A-Z][a-z]+)*)\\s+([A-Z]{2})\\s+(\\d{5}(?:-\\d{4})?)$");
        Matcher noCommaMatcher = noCommaPattern.matcher(address);
        if (noCommaMatcher.matches()) {
            String street = noCommaMatcher.group(1).trim();
            String city = noCommaMatcher.group(2).trim();
            String stateCode = noCommaMatcher.group(3).trim().toUpperCase();
            String zip = noCommaMatcher.group(4).trim();

            String stateName = STATE_ABBREVIATIONS.getOrDefault(stateCode, stateCode);
            return String.format("%s, %s, %s %s", street, city, stateName, zip);
        }

        // Return the abbreviation-expanded address if no pattern matched
        return address;
    }
}
