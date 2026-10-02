package com.kindererp.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseServiceTest {

    private static String sign(KeyPair pair, String machineId) throws Exception {
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(("KinderERP|" + machineId).getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    @Test
    void acceptsOnlyKeysSignedForThisMachineWithTheVendorKey() throws Exception {
        KeyPair vendor = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        KeyPair attacker = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        LicenseService service = new LicenseService(null, Base64.getEncoder().encodeToString(vendor.getPublic().getEncoded()));

        assertThat(service.isValid(sign(vendor, "AAAA-BBBB-CCCC-DDDD"), "AAAA-BBBB-CCCC-DDDD")).isTrue();
        assertThat(service.isValid(sign(vendor, "AAAA-BBBB-CCCC-DDDD"), "1111-2222-3333-4444")).isFalse();
        assertThat(service.isValid(sign(attacker, "AAAA-BBBB-CCCC-DDDD"), "AAAA-BBBB-CCCC-DDDD")).isFalse();
        assertThat(service.isValid("not-a-key", "AAAA-BBBB-CCCC-DDDD")).isFalse();
        assertThat(service.isValid("", "AAAA-BBBB-CCCC-DDDD")).isFalse();
    }

    @Test
    void trialLastsThirtyDays() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        assertThat(LicenseService.evaluate(start, start, "ID").trialDaysLeft()).isEqualTo(30);
        assertThat(LicenseService.evaluate(start, start.plusDays(29), "ID").state()).isEqualTo(LicenseService.State.TRIAL);
        assertThat(LicenseService.evaluate(start, start.plusDays(30), "ID").state()).isEqualTo(LicenseService.State.EXPIRED);
        assertThat(LicenseService.evaluate(start, start.plusDays(30), "ID").allowsUse()).isFalse();
    }

    @Test
    void machineIdIsStableAndFormatted() {
        LicenseService service = new LicenseService(null);
        assertThat(service.machineId()).matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}");
        assertThat(new LicenseService(null).machineId()).isEqualTo(service.machineId());
    }
}
