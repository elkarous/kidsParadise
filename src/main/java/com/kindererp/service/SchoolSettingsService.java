package com.kindererp.service;

import com.kindererp.model.SchoolSettings;
import com.kindererp.repository.SchoolSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.kindererp.service.Checks.*;

@Service
@RequiredArgsConstructor
public class SchoolSettingsService {

    private final SchoolSettingsRepository repository;

    /** Current settings, or unsaved defaults before the setup wizard has run. */
    @Transactional(readOnly = true)
    public SchoolSettings get() {
        return repository.findById(SchoolSettings.SINGLETON_ID).orElseGet(SchoolSettings::new);
    }

    @Transactional(readOnly = true)
    public boolean isSetupCompleted() {
        return repository.findById(SchoolSettings.SINGLETON_ID).map(SchoolSettings::isSetupCompleted).orElse(false);
    }

    @Transactional
    public SchoolSettings save(SchoolSettings settings) {
        validate(settings);
        settings.setId(SchoolSettings.SINGLETON_ID);
        settings.setNameFr(settings.getNameFr() == null ? "" : settings.getNameFr().trim());
        settings.setNameAr(settings.getNameAr() == null ? "" : settings.getNameAr().trim());
        settings.setAddress(trimToNull(settings.getAddress()));
        settings.setPhone(trimToNull(settings.getPhone()));
        settings.setEmail(trimToNull(settings.getEmail()));
        return repository.save(settings);
    }

    static void validate(SchoolSettings s) {
        require(!isBlank(s.getNameFr()) || !isBlank(s.getNameAr()), "validation.school.nameRequired");
        require(isBlank(s.getPhone()) || isValidPhone(s.getPhone()), "validation.phone.invalid");
        require(isBlank(s.getEmail()) || isValidEmail(s.getEmail()), "validation.email.invalid");
        require(!isBlank(s.getCurrencySymbol()), "validation.currency.required");
        require(s.getCurrencyDecimals() >= 0 && s.getCurrencyDecimals() <= 3, "validation.currency.decimals");
        require(s.getRegion() != null && s.getRegion().matches("[A-Z]{2}"), "validation.region.invalid");
        require(isNonNegative(s.getMonthlyFee()) && isNonNegative(s.getDiscountTwoChildren())
                && isNonNegative(s.getDiscountThreePlusChildren()), "validation.amount.negative");
        require(s.getYearStartMonth() >= 1 && s.getYearStartMonth() <= 12
                && s.getYearEndMonth() >= 1 && s.getYearEndMonth() <= 12, "validation.month.invalid");
    }
}
