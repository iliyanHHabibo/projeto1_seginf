import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.security.InvalidKeyException;


public class mySNS{

    private String serverAddressPort;
    private String serverAddress;
    private int serverPort;
    private String doctorUsername;
    private String patientUsername;
    private List<String> scFiles = new ArrayList<>();
    private List<String> saFiles = new ArrayList<>();
    private List<String> seFiles = new ArrayList<>();
    private List<String> gFiles = new ArrayList<>();

    public static void main(String[] args) {
        mySNS mySNS = new mySNS();
        mySNS.parseArgs(args);
        String mode;
        if (mySNS.scFiles != null && !mySNS.scFiles.isEmpty()){
            mode = "sc";
            connectAndSend(mySNS.serverAddress, mySNS.serverPort, mode, mySNS.patientUsername, mySNS.doctorUsername, "123456", mySNS.scFiles);
        }
        //else if para os outros modos (sa, se, g)
        else if (mySNS.saFiles != null && !mySNS.saFiles.isEmpty()) {
            mode = "sa";
            connectAndSend(mySNS.serverAddress, mySNS.serverPort, mode, mySNS.patientUsername, mySNS.doctorUsername, "123456", mySNS.saFiles);
            
        }
    }

    private void parseArgs(String[] args) {
        List<String> currentList = null;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-a":
                    serverAddressPort = args[++i];
                    String[] splitAddressPort = serverAddressPort.split(":");
                    if (splitAddressPort.length != 2) {
                        throw new IllegalArgumentException("Server address and port must be in the format 'address:port'");
                    }
                    serverAddress = splitAddressPort[0];
                    serverPort = Integer.parseInt(splitAddressPort[1]);
                    break;
                case "-m":
                    doctorUsername = args[++i];
                    break;
                case "-u":
                    patientUsername = args[++i];
                    break;
                case "-sc":
                    scFiles = new ArrayList<>();
                    currentList = scFiles; //from now on whatever we add to currentList will be added to scFiles. because currentList and scFiles are pointing to the same memory location. meaning they are the same list.
                    break;
                case "-sa":
                    saFiles = new ArrayList<>();
                    currentList = saFiles; 
                    break;
                case "-se":
                    seFiles = new ArrayList<>();
                    currentList = seFiles;
                    break;
                case "-g":
                    gFiles = new ArrayList<>();
                    currentList = gFiles;
                    break;
                default:
                    if (currentList != null) {
                        currentList.add(args[i]);
                    } else {
                        System.out.println("Unknown argument or option needs a value: " + args[i]);
                    }
                    break;
            }
        }
    }

    //method to verify if the files exist
    private static boolean verifyFileExistence(String filename) {
        //returns false if the file does not exist. returns true if the file exists.
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Erro: O ficheiro  " + filename + " não existe.");
            return false;
        }
        return true;
    }

    //method to verify if keystore for patient exists
    private static boolean verifyKeystoreExistence(String patientName) {
        //returns false if the keystore does not exist. returns true if the keystore exists.
        String keystore = patientName + ".keystore";

        File keystoreFile = new File(keystore);

        if (!keystoreFile.exists()) {
            System.err.println("Erro: Keystore" + keystore + " não encontrada.");
            return false;
        }
        return true;
    }

    // method to generate AES key. 128 bits
    public static SecretKey generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static PrivateKey getPrivateKey(String username, String keystorePassword, String alias) {
        try {
            // Carregar a keystore
            FileInputStream is = new FileInputStream(username+".keystore");
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, keystorePassword.toCharArray());
            is.close();

            // Obter a chave privada
            KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias,
                    new KeyStore.PasswordProtection(keystorePassword.toCharArray()));
            if (pkEntry == null) {
                System.err.println("Entrada para o alias '" + alias + "' não encontrada ou senha incorreta.");
                return null;
            }

            return pkEntry.getPrivateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static PublicKey getPublicKey(String username, String keystorePassword, String alias) {
        try {
            // Carregar a keystore
            FileInputStream is = new FileInputStream(username+".keystore");
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, keystorePassword.toCharArray());
            is.close();

            // Obter o certificado do usuário
            Certificate cert = keystore.getCertificate(alias);
            if (cert == null) {
                System.err.println("Certificado para o alias '" + alias + "' não encontrado na keystore.");
                return null;
            }

            // Retornar a chave pública associada ao certificado
            return cert.getPublicKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static PublicKey getDoctorPublicKey(String patientUsername, String doctorUsername, String keyStorePassword) {
        try{
            //we get the doctor's certificate from the patient's keystore

            //load the patient's keystore
            FileInputStream is = new FileInputStream(patientUsername + ".keystore");
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, keyStorePassword.toCharArray());

            //get the doctor's certificate from the keystore
            Certificate doctorCertificate = keystore.getCertificate(doctorUsername);
            if (doctorCertificate == null){
                System.err.println("Erro: Certificado do médico não encontrado.");
                return null;
            }

            //return the doctor's public key from the certificate
            return doctorCertificate.getPublicKey();

        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Erro ao acessar a keystore ou ao obter a chave pública.");
        }
        return null;

    }

    //use the secret AES key to encrypt the file and sending it to the server
    //receives success or failure message from the server
    public static void encryptFileAndSend (String filename, SecretKey key, Socket socket, DataOutputStream dataOutputStream){
        try{
            // Initialize the cipher for encryption with the generated key
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);

            // Open input and output streams for reading from and writing to files
            FileInputStream fis = new FileInputStream(filename); // Open input stream to read from file filename 
            
            //get the OutputStream of the Socket to send data to the server
            OutputStream socketOut = socket.getOutputStream();

            //tell the server that we have begun sending the file
            dataOutputStream.writeUTF("inicio do envio do ficheiro:" + filename);
            dataOutputStream.flush(); //guarantee that the data that may be in the buffer is sent to the server

            // Lê o arquivo e criptografa
            byte[] fileBytes = Files.readAllBytes(Paths.get(filename));
            byte[] encryptedFileBytes = c.doFinal(fileBytes);
            
            // Envia o tamanho do arquivo criptografado
            dataOutputStream.writeInt(encryptedFileBytes.length);
            
            // Envia o arquivo criptografado
            dataOutputStream.write(encryptedFileBytes);

            // Close the file input stream
            fis.close();

            System.out.println("Ficheiro " + filename + " enviado com sucesso.");

            // Preparar para receber a resposta do servidor
            DataInputStream serverResponseStream = new DataInputStream(socket.getInputStream());
            String serverResponse = serverResponseStream.readUTF(); // Ler a resposta do servidor

            System.out.println("Resposta do servidor: " + serverResponse);

        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Erro ao encriptar o ficheiro.");
        }
    }

    public static void encryptAndSendSecretKey(SecretKey key, PublicKey publicKey, Socket socket, String filename, DataOutputStream dataOutputStream) {
        try {
            // Initialize a Cipher for RSA encryption
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    
            // Encrypt the AES key with the RSA public key
            byte[] encryptedKey = cipher.doFinal(key.getEncoded());
    
            // Inform the server that we are about to send an encrypted key
            dataOutputStream.writeUTF("inicio do envio da chave secreta encriptada:" + filename);
            dataOutputStream.flush();
    
            // Send the size of the encrypted key
            dataOutputStream.writeInt(encryptedKey.length);
            dataOutputStream.flush();
    
            // Send the encrypted key
            dataOutputStream.write(encryptedKey);
            dataOutputStream.flush();
    
            System.out.println("Chave secreta encriptada e tamanho enviados com sucesso.");
    
            // Prepare to receive the server's response
            DataInputStream serverResponseStream = new DataInputStream(socket.getInputStream());
            String serverResponse = serverResponseStream.readUTF(); // Read the server's response
    
            System.out.println("Resposta do servidor: " + serverResponse);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao encriptar e enviar a chave secreta.");
        }
    }
    

    //method to do everything regarding the option -sc
    public static void sc (Socket socket, List<String> fileList, String patientUsername, String doctorUsername, String keyStorePassword, DataOutputStream dataOutputStream){
        try{
            //verify if the keystore exists
            if (!verifyKeystoreExistence(patientUsername)){
                return;
            }

            //get the doctor's public key
            PublicKey publicKey = getDoctorPublicKey(patientUsername, doctorUsername, keyStorePassword);

            //send name of patient to server. so we can associate a directory to the patient
            dataOutputStream.writeUTF(patientUsername);
            dataOutputStream.flush(); //guarantee that the data that may be in the buffer is sent to the server
 

            //encrypt and send the files and the secret key
            for (String filename : fileList){
                if (verifyFileExistence(filename)){
                     //generate AES key
                    SecretKey key = generateAESKey();

                    //encrypt the file and send it to the server
                    encryptFileAndSend(filename, key, socket, dataOutputStream);

                    //encrypt and send the secret key
                    encryptAndSendSecretKey(key, publicKey, socket, filename, dataOutputStream);
                }
            }
            //send message to server that we have finished sending files
            dataOutputStream.writeUTF("FIM DO ENVIO DE FICHEIROS");
            dataOutputStream.flush(); //guarantee that the data that may be in the buffer is sent to the server
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Erro ao enviar ficheiros.");
        }
    }
    //method to do everything regarding the option -sa
    public static void sa(List<String> filenames, Socket socket, String patientUsername, String doctorUsername, String keyStorePassword) {
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
    
            PrivateKey privateKey = getPrivateKey(patientUsername, keyStorePassword, patientUsername);
            if (privateKey == null) {
                System.err.println("Falha ao recuperar a chave privada.");
                return;
            }
    
            for (String filename : filenames) {
                File file = new File(filename);
                if (!file.exists()) {
                    System.out.println("Erro: O arquivo " + filename + " não existe localmente.");
                    continue;
                }
    
                // Assinar o arquivo
                byte[] signature = signFile(file, privateKey);
                if (signature == null) {
                    System.err.println("Falha ao assinar o arquivo.");
                    continue;
                }
    
                // Enviar nome do arquivo assinado
                dos.writeUTF(file.getName() + ".assinado");
    
                // Enviar arquivo
                sendFile(file, dos);
    
                // Enviar nome da assinatura
                dos.writeUTF(file.getName() + ".assinatura." + doctorUsername);
    
                // Enviar assinatura
                dos.writeInt(signature.length);
                dos.write(signature);
    
                // Esperar resposta do servidor para cada arquivo e assinatura enviados
                String response = dis.readUTF();
                System.out.println(response);
            }
    
            // Indicar ao servidor que a transmissão dos arquivos acabou
            dos.writeUTF("FIM_DO_ENVIO_DE_FICHEIROS");
            dos.flush();
    
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erro durante o envio de arquivos -sa.");
        }
    }
    


private static void sendFile(File file, DataOutputStream dos) throws IOException {
    // Enviar tamanho do arquivo
    long fileSize = file.length();
    dos.writeLong(fileSize);

    // Enviar arquivo
    FileInputStream fis = new FileInputStream(file);
    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = fis.read(buffer)) != -1) {
        dos.write(buffer, 0, bytesRead);
    }
    fis.close();
}

private static byte[] signFile(File file, PrivateKey privateKey) {
    try {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        byte[] fileData = Files.readAllBytes(file.toPath());
        privateSignature.update(fileData);
        return privateSignature.sign();
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | IOException e) {
        e.printStackTrace();
        return null;
    }
}


    // connects to server and calls functions sc, sa, se, g (string mode)
    public static void connectAndSend(String serverAddress, int serverPort, String mode, String patientUsername, String doctorUsername, String keyStorePassword, List<String> fileList){
        try{
         // Establish connection to the server
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Conectado ao servidor em: " + serverAddress + ":" + serverPort); 

            //case sc, sa, se, g (create one function for each and then call the functions here)
            try (DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
                switch (mode) {
                    case "sc":
                        dataOutputStream.writeUTF("sc");
                        dataOutputStream.flush();
                        sc(socket, fileList, patientUsername, doctorUsername, keyStorePassword, dataOutputStream);
                        //close dataOutputStream
                        dataOutputStream.close();
                        break;
                    case "sa":
                        dataOutputStream.writeUTF("sa");
                        dataOutputStream.flush();
                        sa(fileList, socket, patientUsername, doctorUsername, keyStorePassword);
                        //close dataOutputStream
                        dataOutputStream.close();
                        break;
                    default:
                        System.out.println("Invalid mode");
                        break;
                }
            } catch (IOException e) {
                // Handle exceptions related to I/O operations
                e.printStackTrace();
            }

            // Close the socket
            socket.close();
            System.out.println("Desconectado do servidor.");

        } catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }

}


//comando para compilar: javac mySNS.java
//comando para correr(sc): java mySNS -a 127.0.0.1:23456 -m silva -u maria -sc exame1.png relatorio1.pdf
//comando para correr (sa): java mySNS -a 127.0.0.1:23456 -m silva -u maria -sa exame1.png relatorio1.pdf
