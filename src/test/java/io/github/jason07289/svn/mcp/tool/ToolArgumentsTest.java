package io.github.jason07289.svn.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolArgumentsTest {

    @Test
    void requireString_returnsTrimmed() {
        assertThat(ToolArguments.requireString(Map.of("k", "  x  "), "k")).isEqualTo("x");
    }

    @Test
    void requireString_missingOrBlank_throws() {
        assertThatThrownBy(() -> ToolArguments.requireString(Map.of(), "k"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("k");
        assertThatThrownBy(() -> ToolArguments.requireString(Map.of("k", ""), "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ToolArguments.requireString(Map.of("k", "   "), "k"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void optionalString_usesDefaultWhenAbsent() {
        assertThat(ToolArguments.optionalString(Map.of(), "k", "d")).isEqualTo("d");
    }

    @Test
    void optionalStringNullable_nullMapOrBlank_returnsNull() {
        assertThat(ToolArguments.optionalStringNullable(null, "k")).isNull();
        assertThat(ToolArguments.optionalStringNullable(Map.of(), "k")).isNull();
        assertThat(ToolArguments.optionalStringNullable(Map.of("k", ""), "k")).isNull();
        assertThat(ToolArguments.optionalStringNullable(Map.of("k", "  "), "k")).isNull();
        assertThat(ToolArguments.optionalStringNullable(Map.of("k", " x "), "k")).isEqualTo("x");
    }

    @Test
    void optionalLong_parsesNumberAndString() {
        assertThat(ToolArguments.optionalLong(Map.of("r", 5L), "r")).isEqualTo(5L);
        assertThat(ToolArguments.optionalLong(Map.of("r", 7), "r")).isEqualTo(7L);
        assertThat(ToolArguments.optionalLong(Map.of("r", "9"), "r")).isEqualTo(9L);
        assertThat(ToolArguments.optionalLong(Map.of(), "r")).isNull();
    }

    @Test
    void optionalInt_parsesNumberAndString() {
        assertThat(ToolArguments.optionalInt(Map.of("n", 3), "n")).isEqualTo(3);
        assertThat(ToolArguments.optionalInt(Map.of("n", "4"), "n")).isEqualTo(4);
    }

    @Test
    void optionalBoolean_parsesBooleanAndString() {
        assertThat(ToolArguments.optionalBoolean(Map.of("b", true), "b")).isTrue();
        assertThat(ToolArguments.optionalBoolean(Map.of("b", "false"), "b")).isFalse();
    }

    @Test
    void requireLong_throwsWhenMissing() {
        assertThatThrownBy(() -> ToolArguments.requireLong(Map.of(), "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("x");
    }

    @Test
    void parseIsoToDate_acceptsInstantAndOffset() {
        Date a = ToolArguments.parseIsoToDate("2025-03-26T12:00:00Z");
        Date b = ToolArguments.parseIsoToDate("2025-03-26T12:00:00+09:00");
        assertThat(a).isNotNull();
        assertThat(b).isNotNull();
    }

    @Test
    void calendarDayInclusiveBounds_coversLocalDay() {
        Date[] bounds = ToolArguments.calendarDayInclusiveBounds("2025-06-01", "Asia/Seoul");
        assertThat(bounds[0].before(bounds[1]) || bounds[0].equals(bounds[1])).isTrue();
    }

    @Test
    void optionalIsoDate_returnsNullWhenMissing() {
        assertThat(ToolArguments.optionalIsoDate(Map.of(), "d")).isNull();
    }

    @Test
    void optionalIsoDate_trimsAndParsesOffsetDateTime() {
        assertThat(ToolArguments.optionalIsoDate(Map.of("d", " 2025-03-26T12:00:00+09:00 "), "d"))
                .isNotNull();
    }

    @Test
    void parseIsoToDate_rejectsInvalidIsoText() {
        assertThatThrownBy(() -> ToolArguments.parseIsoToDate("not-a-date"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void calendarDayInclusiveBounds_handlesUtcAndDstSensitiveZone() {
        Date[] utcBounds = ToolArguments.calendarDayInclusiveBounds("2025-01-01", "UTC");
        Date[] seoulBounds = ToolArguments.calendarDayInclusiveBounds("2025-01-01", "Asia/Seoul");
        assertThat(utcBounds[0]).isNotEqualTo(seoulBounds[0]);
        assertThat(utcBounds[1]).isNotEqualTo(seoulBounds[1]);
    }
}
