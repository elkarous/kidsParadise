package com.kindererp.service;

import com.kindererp.model.SchoolSettings;
import com.kindererp.repository.SchoolSettingsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Per-machine licensing with a 30-day trial.
 *
 * <p>The machine ID is derived from the Windows MachineGuid. A license key is an Ed25519 signature
 * of "KinderERP|&lt;machine id&gt;" made with the vendor's private key (tools/LicenseKeyGenerator.java);
 * the application only contains the public key, so keys cannot be forged from the application.
 * The trial start date is kept both in the database and in the user's registry, and the earliest wins.
 */
@Slf4j
@Service
public class LicenseService {

    public static final int TRIAL_DAYS = 30;
    static final String PUBLIC_KEY = "MCowBQYDK2VwAyEAwwR/4XECUCYF7iAVL4IcJwoTsDCenwHv+eZFuy1mTEY=";
    private static final String SIGNED_PREFIX = "KinderERP|";
    private static final Preferences PREFS = Preferences.userRoot().node("com/kindererp/license");

    public enum State { LICENSED, TRIAL, EXPIRED }

    public record Status(State state, long trialDaysLeft, String machineId) {
        public boolean allowsUse() {
            return state != State.EXPIRED;
        }
    }

    private final SchoolSettingsRepository settingsRepository;
    private final PublicKey publicKey;
    private String machineId;

    @Autowired
    public LicenseService(SchoolSettingsRepository settingsRepository) {
        this(settingsRepository, PUBLIC_KEY);
    }

    LicenseService(SchoolSettingsRepository settingsRepository, String base64PublicKey) {
        this.settingsRepository = settingsRepository;
        try {
            this.publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64PublicKey)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Invalid license public key", e);
        }
    }

    @Transactional
    public Status status() {
        SchoolSettings settings = settingsRepository.findById(SchoolSettings.SINGLETON_ID).orElse(null);
        if (settings != null && isValid(settings.getLicenseKey(), machineId())) {
            return new Status(State.LICENSED, 0, machineId());
        }
        LocalDate trialStart = trialStart(settings);
        return evaluate(trialStart, LocalDate.now(), machineId());
    }

    /** Trial rule, independent of storage: 30 days from the first launch, day 1 included. */
    static Status evaluate(LocalDate trialStart, LocalDate today, String machineId) {
        long daysLeft = TRIAL_DAYS - ChronoUnit.DAYS.between(trialStart, today);
        return daysLeft > 0 ? new Status(State.TRIAL, daysLeft, machineId) : new Status(State.EXPIRED, 0, machineId);
    }

    @Transactional
    public void activate(String licenseKey) {
        String key = licenseKey == null ? "" : licenseKey.trim();
        if (!isValid(key, machineId())) {
            throw new BusinessException("license.error.invalid");
        }
        SchoolSettings settings = settingsRepository.findById(SchoolSettings.SINGLETON_ID)
                .orElseThrow(() -> new BusinessException("license.error.setupFirst"));
        settings.setLicenseKey(key);
        log.info("License activated for machine {}", machineId());
    }

    public boolean isValid(String licenseKey, String forMachineId) {
        if (licenseKey == null || licenseKey.isBlank()) {
            return false;
        }
        try {
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update((SIGNED_PREFIX + forMachineId).getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getUrlDecoder().decode(licenseKey.trim()));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            return false;
        }
    }

    /** Stable identifier of this computer, e.g. "3F2A-91C0-77DE-0B14". */
    public synchronized String machineId() {
        if (machineId == null) {
            byte[] hash;
            try {
                hash = MessageDigest.getInstance("SHA-256").digest(("KinderERP-machine|" + rawMachineId()).getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            String hex = HexFormat.of().withUpperCase().formatHex(hash, 0, 8);
            machineId = String.join("-", hex.substring(0, 4), hex.substring(4, 8), hex.substring(8, 12), hex.substring(12, 16));
        }
        return machineId;
    }

    private LocalDate trialStart(SchoolSettings settings) {
        LocalDate fromDb = settings == null ? null : settings.getTrialStartDate();
        LocalDate fromRegistry = null;
        String saved = PREFS.get("trialStart", null);
        if (saved != null) {
            try {
                fromRegistry = LocalDate.parse(saved);
            } catch (RuntimeException e) {
                log.warn("Ignoring unreadable trial date {}", saved);
            }
        }
        LocalDate start = earliest(earliest(fromDb, fromRegistry), LocalDate.now());
        if (!start.toString().equals(saved)) {
            PREFS.put("trialStart", start.toString());
        }
        if (settings != null && !start.equals(fromDb)) {
            settings.setTrialStartDate(start);
        }
        return start;
    }

    private static LocalDate earliest(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        return b == null || a.isBefore(b) ? a : b;
    }

    private static String rawMachineId() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                Process process = new ProcessBuilder("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
                        .redirectErrorStream(true).start();
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                process.waitFor(5, TimeUnit.SECONDS);
                for (String line : output.split("\\R")) {
                    if (line.contains("MachineGuid")) {
                        String[] parts = line.trim().split("\\s+");
                        return parts[parts.length - 1];
                    }
                }
            }
            for (String file : new String[]{"/etc/machine-id", "/var/lib/dbus/machine-id"}) {
                Path path = Path.of(file);
                if (Files.isReadable(path)) {
                    return Files.readString(path).trim();
                }
            }
        } catch (IOException e) {
            log.warn("Could not read the machine identifier", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            return System.getProperty("user.name", "unknown");
        }
    }
}
