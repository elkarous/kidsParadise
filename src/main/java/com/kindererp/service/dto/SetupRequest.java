package com.kindererp.service.dto;

import com.kindererp.model.SchoolSettings;

/** Everything the first-launch wizard collects. */
public record SetupRequest(SchoolSettings settings,
                           String adminUsername,
                           String adminFullName,
                           String adminPassword,
                           String schoolYearName,
                           boolean loadDemoData) {
}
