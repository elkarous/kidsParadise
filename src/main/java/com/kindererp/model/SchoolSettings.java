package com.kindererp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Everything that is specific to one school: identity shown on screens and printouts, currency,
 * fees and the school-year calendar. There is exactly one row (id = 1).
 */
@Entity
@Table(name = "school_settings")
@Getter
@Setter
@NoArgsConstructor
public class SchoolSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "name_fr", nullable = false, length = 200)
    private String nameFr = "";

    @Column(name = "name_ar", nullable = false, length = 200)
    private String nameAr = "";

    @Column(length = 300)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    /** PNG/JPEG bytes of the school logo, or null. */
    @Column(name = "logo")
    private byte[] logo;

    /**
     * ISO country code of the school (TN, DZ, MA, FR, ...). Drives regional formats such as Arabic
     * month names (جانفي in Tunisia/Algeria, يناير elsewhere).
     */
    @Column(nullable = false, length = 2)
    private String region = "TN";

    /** Currency label shown next to amounts, e.g. "DT", "DA", "DH", "€". */
    @Column(name = "currency_symbol", nullable = false, length = 10)
    private String currencySymbol = "DT";

    /** Decimals displayed for amounts (3 for TND, 2 for most others). */
    @Column(name = "currency_decimals", nullable = false)
    private int currencyDecimals = 3;

    /** Monthly tuition per child. */
    @Column(name = "monthly_fee", nullable = false)
    private BigDecimal monthlyFee = BigDecimal.ZERO;

    /** Total monthly discount for a family with exactly two enrolled children. */
    @Column(name = "discount_two_children", nullable = false)
    private BigDecimal discountTwoChildren = BigDecimal.ZERO;

    /** Total monthly discount for a family with three or more enrolled children. */
    @Column(name = "discount_three_plus_children", nullable = false)
    private BigDecimal discountThreePlusChildren = BigDecimal.ZERO;

    /** First billable month of a school year (1-12), e.g. 9 = September. */
    @Column(name = "year_start_month", nullable = false)
    private int yearStartMonth = 9;

    /** Last billable month of a school year (1-12), e.g. 6 = June. */
    @Column(name = "year_end_month", nullable = false)
    private int yearEndMonth = 6;

    @Column(name = "setup_completed", nullable = false)
    private boolean setupCompleted;

    @Column(name = "trial_start_date")
    private LocalDate trialStartDate;

    @Column(name = "license_key", length = 200)
    private String licenseKey;

    /** School name in the requested language, falling back to the other one. */
    public String displayName(boolean arabic) {
        String preferred = arabic ? nameAr : nameFr;
        String fallback = arabic ? nameFr : nameAr;
        return preferred != null && !preferred.isBlank() ? preferred : (fallback == null ? "" : fallback);
    }
}
