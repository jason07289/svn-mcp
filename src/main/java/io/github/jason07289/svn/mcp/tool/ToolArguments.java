package io.github.jason07289.svn.mcp.tool;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

final class ToolArguments {

    private ToolArguments() {}

    static String requireString(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return s;
    }

    static String optionalString(Map<String, Object> args, String key, String defaultValue) {
        Object v = args.get(key);
        if (v == null) {
            return defaultValue;
        }
        return String.valueOf(v);
    }

    /** Trims; returns {@code null} if absent or blank. */
    static String optionalStringNullable(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    static Long optionalLong(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    static Integer optionalInt(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(v));
    }

    static Boolean optionalBoolean(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    static long requireLong(Map<String, Object> args, String key) {
        Long v = optionalLong(args, key);
        if (v == null) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return v;
    }

    /** ISO-8601 instant or offset datetime; null if argument absent. */
    static Date optionalIsoDate(Map<String, Object> args, String key) {
        String s = optionalStringNullable(args, key);
        if (s == null) {
            return null;
        }
        return parseIsoToDate(s);
    }

    static Date parseIsoToDate(String s) {
        try {
            return Date.from(Instant.parse(s));
        } catch (DateTimeParseException e) {
            return Date.from(OffsetDateTime.parse(s).toInstant());
        }
    }

    /**
     * Inclusive calendar day in the given zone (midnight through last instant of that local date).
     */
    static Date[] calendarDayInclusiveBounds(String calendarDate, String timeZoneId) {
        LocalDate d = LocalDate.parse(calendarDate);
        ZoneId z = ZoneId.of(timeZoneId);
        ZonedDateTime start = d.atStartOfDay(z);
        ZonedDateTime end = d.atTime(LocalTime.MAX).atZone(z);
        return new Date[] {Date.from(start.toInstant()), Date.from(end.toInstant())};
    }
}
