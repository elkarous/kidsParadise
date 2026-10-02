import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Vendor tool, NOT shipped with the application. Run with the JDK, no build needed:
 *
 *   java tools/LicenseKeyGenerator.java init  <private-key-file>       creates the key pair once, prints the public key
 *   java tools/LicenseKeyGenerator.java sign  <private-key-file> <ID>  prints the license key for a machine ID
 *
 * Keep the private key file secret and backed up, outside the repository: whoever has it can create licenses.
 * The public key printed by "init" goes into LicenseService.PUBLIC_KEY.
 */
public class LicenseKeyGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("init")) {
            Path file = Path.of(args[1]);
            if (Files.exists(file)) {
                throw new IllegalStateException(file + " already exists; refusing to overwrite an existing private key");
            }
            KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()));
            System.out.println("Private key written to " + file.toAbsolutePath());
            System.out.println("PUBLIC_KEY = \"" + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()) + "\"");
        } else if (args.length == 3 && args[0].equals("sign")) {
            byte[] encoded = Base64.getDecoder().decode(Files.readString(Path.of(args[1])).trim());
            PrivateKey key = KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(encoded));
            String machineId = args[2].trim().toUpperCase();
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(key);
            signature.update(("KinderERP|" + machineId).getBytes(StandardCharsets.UTF_8));
            System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign()));
        } else {
            System.err.println("Usage: init <private-key-file> | sign <private-key-file> <machine-id>");
            System.exit(1);
        }
    }
}
