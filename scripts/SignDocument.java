// SPDX-License-Identifier: GPL-3.0-or-later
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/** Run with Java 21 source-file mode. Secrets are read only from the environment. */
public class SignDocument {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input.json output.json");
        char[] storePassword = System.getenv("NEWPIPET_STORE_PASSWORD").toCharArray();
        KeyStore store = KeyStore.getInstance(
            Path.of(System.getenv("NEWPIPET_KEYSTORE")).toFile(), storePassword);
        PrivateKey key = (PrivateKey) store.getKey(System.getenv("NEWPIPET_KEY_ALIAS"),
            System.getenv("NEWPIPET_KEY_PASSWORD").toCharArray());
        if (!"RSA".equals(key.getAlgorithm())) throw new IllegalArgumentException("Use an RSA key");
        byte[] payload = Files.readAllBytes(Path.of(args[0]));
        if (payload.length > 65536) throw new IllegalArgumentException("Document too large");
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(payload);
        String encoded = Base64.getEncoder().encodeToString(payload);
        String signature = Base64.getEncoder().encodeToString(signer.sign());
        Files.writeString(Path.of(args[1]), "{\"payload\":\"" + encoded
            + "\",\"signature\":\"" + signature + "\"}\n", StandardCharsets.UTF_8);
    }
}
