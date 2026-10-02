package com.kindererp.service;

import com.kindererp.model.SchoolSettings;
import com.kindererp.service.dto.SetupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.kindererp.service.Checks.require;

/** First launch: school identity, first administrator account and first school year, in one transaction. */
@Service
@RequiredArgsConstructor
public class SetupService {

    private final SchoolSettingsService settingsService;
    private final UserService userService;
    private final SchoolYearService schoolYearService;
    private final DemoDataService demoDataService;

    @Transactional
    public void completeSetup(SetupRequest request) {
        require(!settingsService.isSetupCompleted(), "setup.error.alreadyDone");

        SchoolSettings settings = request.settings();
        settings.setSetupCompleted(true);
        if (settings.getTrialStartDate() == null) {
            settings.setTrialStartDate(LocalDate.now());
        }
        settingsService.save(settings);
        userService.createUser(request.adminUsername(), request.adminFullName(), request.adminPassword());
        schoolYearService.createYear(request.schoolYearName());
        if (request.loadDemoData()) {
            demoDataService.load();
        }
    }
}
