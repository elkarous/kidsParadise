package com.kindererp.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppInfoTest {

    @Test
    void versionAndNameComeFromTheBuild() {
        assertThat(AppInfo.name()).isEqualTo("KinderERP");
        assertThat(AppInfo.version()).matches("\\d+\\.\\d+\\.\\d+.*").doesNotContain("@", "${");
    }
}
