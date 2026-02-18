import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class KeyGenTest {
    public static void main(String[] args) throws Exception {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);

        KeyPair kp = kpg.generateKeyPair();

        String pub = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String priv = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

        System.out.println("PUBLIC KEY BASE64:");
        System.out.println(pub);

        System.out.println("\nPRIVATE KEY BASE64:");
        System.out.println(priv);
    }
}
