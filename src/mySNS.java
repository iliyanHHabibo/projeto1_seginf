import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyStore;
import java.security.cert.Certificate;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

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
        if (mySNS.scFiles != null) {
            mode = "sc";
            connectAndSend(mySNS.serverAddress, mySNS.serverPort, mode, mySNS.patientUsername, mySNS.doctorUsername, "123456", mySNS.scFiles);
        }

       //else if para os outros modos (sa, se, g)
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
    public static void encryptFileAndSend (String filename, SecretKey key, Socket socket){
        try{
            // Initialize the cipher for encryption with the generated key
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);

            // Open input and output streams for reading from and writing to files
            FileInputStream fis = new FileInputStream(filename); // Open input stream to read from "a.txt" file
            
            //get the OutputStream of the Socket to send data to the server
            OutputStream socketOut = socket.getOutputStream();

            //create a CipherOutputStream based on the OutputStream of the socket
            //to encrypt the data before sending it
            CipherOutputStream cos = new CipherOutputStream(socketOut, c);

            //tell the server that we have begun sending the file
            socketOut.write("inicio do envio do ficheiro".getBytes());

            //send to the server the filename
            socketOut.write(filename.getBytes());

            byte[] buffer = new byte[4096]; 
            int bytesRead;

            //read the file and write the encrypted data directly to the Socket's OutputStream
            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }

            //if we close the CipherOutputStream, we close the socket output stream as well
            //cos.close();
            fis.close();

            //tell the server that we have finished sending the file
            socketOut.write("fim do envio do ficheiro".getBytes());

            System.out.println("Ficheiro " + filename + " enviado com sucesso.");

        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Erro ao encriptar o ficheiro.");
        }
    }

    public static void encryptAndSendSecretKey(SecretKey key, PublicKey publicKey, Socket socket){
        try{
            // Initialize a Cipher for RSA encryption
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // Encrypt the AES key with the RSA public key
            byte[] encryptedKey = cipher.doFinal(key.getEncoded());

            //tell the server that we are sending the encrypted key
            socket.getOutputStream().write("inicio do envio da chave secreta encriptada".getBytes());

            //write the encrypted key to the socket's OutputStream
            socket.getOutputStream().write(encryptedKey);

            //tell the server that we have finished sending the encrypted key
            socket.getOutputStream().write("fim do envio da chave secreta encriptada".getBytes());

            System.out.println("Chave secreta encriptada enviada com sucesso.");

        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Erro ao encriptar a chave secreta.");
        }
    }
    

    //method to do everything regarding the option -sc
    public static void sc (Socket socket, List<String> fileList, String patientUsername, String doctorUsername, String keyStorePassword){
        try{
            //verify if the keystore exists
            if (!verifyKeystoreExistence(patientUsername)){
                return;
            }

            //get the doctor's public key
            PublicKey publicKey = getDoctorPublicKey(patientUsername, doctorUsername, keyStorePassword);

            //send name of patient to server. so we can associate a directory to the patient
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeUTF(patientUsername);
            dataOutputStream.flush(); //guarantee that the data that may be in the buffer is sent to the server
 

            //encrypt and send the files and the secret key
            for (String filename : fileList){
                if (verifyFileExistence(filename)){
                     //generate AES key
                    SecretKey key = generateAESKey();

                    //encrypt the file and send it to the server
                    encryptFileAndSend(filename, key, socket);

                    //encrypt and send the secret key
                    encryptAndSendSecretKey(key, publicKey, socket);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Erro ao enviar ficheiros.");
        }
    }

    // connects to server and calls functions sc, sa, se, g (string mode)
    public static void connectAndSend(String serverAddress, int serverPort, String mode, String patientUsername, String doctorUsername, String keyStorePassword, List<String> fileList){
        try{
         // Establish connection to the server
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Conectado ao servidor em: " + serverAddress + ":" + serverPort); 

            //case sc, sa, se, g (create one function for each and then call the functions here)
            switch (mode){
                case "sc":
                    sc(socket, fileList, patientUsername, doctorUsername, keyStorePassword);
                    break;
                //case sa:
                //case se:
                //case g:
            }
            
            //send message to server to indicate that we have finished sending files
            OutputStream socketOut = socket.getOutputStream();
            socketOut.write("FIM DO ENVIO DE FICHEIROS".getBytes());


            //CLIENT CANT CLOSE THE SOCKET BECAUSE THE SERVER WILL BE SENDING A RESPONSE TO THE REQUEST
            //THE RESPONSE COULD BE AN ERROR MESSAGE OR A SUCCESS MESSAGE
            InputStream socketIn = socket.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead = socketIn.read(buffer);
            String response = new String(buffer, 0, bytesRead);
            System.out.println(response);

            // Close the socket
            socket.close();
            System.out.println("Desconectado do servidor.");

        } catch (Exception e){
            System.out.println("Error: " + e.getMessage());
        }
    }



}
