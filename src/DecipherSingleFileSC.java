import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class DecipherSingleFileSC {

    public static void main(String[] args) {
        // Verifica se um nome de arquivo foi fornecido
        if (args.length != 1) {
            System.err.println("Uso: java DecipherSingleFile <nome do arquivo cifrado>");
            return;
        }

        String encryptedFileName = args[0]; // Nome do arquivo cifrado recebido na linha de comando

        try {
            // Carregar a keystore do Silva
            FileInputStream is = new FileInputStream("silva.keystore");
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = "123456".toCharArray(); // Senha da keystore
            keystore.load(is, password);

            // Obter a chave privada do Silva
            String alias = "silva"; // Alias da chave do Silva
            PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password);

            // Nome do arquivo da chave secreta cifrada (assume-se uma convenção de nomes)
            String encryptedSecretKeyFileName = encryptedFileName.replace(".cifrado", ".chave_secreta.maria");

            // Decifrar a chave secreta
            byte[] encryptedKeyBytes = Files.readAllBytes(new File(encryptedSecretKeyFileName).toPath());
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedKeyBytes = cipher.doFinal(encryptedKeyBytes);
            SecretKey secretKey = new SecretKeySpec(decryptedKeyBytes, "AES");

            // Decifrar o arquivo
            byte[] encryptedFileBytes = Files.readAllBytes(new File(encryptedFileName).toPath());
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedFileBytes = cipher.doFinal(encryptedFileBytes);

            // Salvar o arquivo decifrado
            String decryptedFileName = encryptedFileName.replace(".cifrado", ".decifrado");
            try (FileOutputStream fos = new FileOutputStream(decryptedFileName)) {
                fos.write(decryptedFileBytes);
            }

            System.out.println("Arquivo decifrado com sucesso: " + decryptedFileName);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao decifrar o arquivo.");
        }
    }
}
