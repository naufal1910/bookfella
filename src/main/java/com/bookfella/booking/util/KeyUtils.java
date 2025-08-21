package com.bookfella.booking.util;

import java.text.Normalizer;

public final class KeyUtils {
    private KeyUtils() {}

    public static final int SEARCH_TTL_SECONDS = 60;
    public static final int IDEMPOTENCY_TTL_SECONDS = 600;

    public static String normalizeQuery(String q) {
        if (q == null) return "";
        String s = Normalizer.normalize(q.trim().toLowerCase(), Normalizer.Form.NFKC);
        return s.replaceAll("\\s+", " ");
    }

    public static String searchCityKey(String city) {
        return "search:city:" + (city == null ? "" : city.trim());
    }

    public static String searchQueryKey(String q) {
        return "search:q:" + normalizeQuery(q);
    }
}
