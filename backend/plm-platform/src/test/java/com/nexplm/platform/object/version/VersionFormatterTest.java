package com.nexplm.platform.object.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionFormatterTest {

    @Test
    void letterSequenceCrossesAlphabetBoundary() {
        assertThat(VersionFormatter.letter(1)).isEqualTo("A");
        assertThat(VersionFormatter.letter(26)).isEqualTo("Z");
        assertThat(VersionFormatter.letter(27)).isEqualTo("AA");
        assertThat(VersionFormatter.letter(28)).isEqualTo("AB");
        assertThat(VersionFormatter.letter(33)).isEqualTo("AG");
        assertThat(VersionFormatter.letter(34)).isEqualTo("AH");
        assertThat(VersionFormatter.letter(52)).isEqualTo("AZ");
        assertThat(VersionFormatter.letter(53)).isEqualTo("BA");
        assertThat(VersionFormatter.letter(78)).isEqualTo("BZ");
        assertThat(VersionFormatter.letter(79)).isEqualTo("CA");
        assertThat(VersionFormatter.letter(702)).isEqualTo("ZZ");
        assertThat(VersionFormatter.letter(703)).isEqualTo("AAA");
    }

    @Test
    void labelCombinesRevisionAndIteration() {
        assertThat(VersionFormatter.label(1, 1)).isEqualTo("A.1");
        assertThat(VersionFormatter.label(2, 3)).isEqualTo("B.3");
        assertThat(VersionFormatter.label(null, null)).isEqualTo("A.1");
        assertThat(VersionFormatter.label(0, 0)).isEqualTo("A.1");
    }
}
