import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class mySNS2 {

    public static void cifraFicheiro (String nome, String med, String ute) throws Exception{
        //(nome = nome do Ficheiro)
        try{
            FileInputStream fis = new FileInputStream(nome);
            FileOutputStream fos = new FileOutputStream(nome + ".cifrado");

            //Criar chave AES para cifrar o ficheiro:
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            SecretKey key = kg.generateKey();
            // Instanciar o Cipher para encryptar
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);

            //Encriptar ficheiro:
            CipherOutputStream cos = new CipherOutputStream(fos, c);
            byte[] b = new byte[16];
                int x = fis.read(b);
                while (x != -1) {
                    cos.write(b, 0, x);
                    x = fis.read(b);
                }
                cos.close();
            
            //buscar o cert do utente na keystore do medico:
            FileInputStream kfile = new FileInputStream("keystore." + med);
            KeyStore kstore = KeyStore.getInstance("PKCS12");
            kstore.load(kfile, "1234567890".toCharArray()); // Passwrd
            Certificate cert = kstore.getCertificate(ute); // chave publica do utente na keystore do medico

            //Instancira um segundo Cipher para encriptar a chave AES com RSA:
            Cipher c2 = Cipher.getInstance("RSA");
            c2.init(Cipher.WRAP_MODE, cert);
            //Wrap da key:
            byte[] keyWrapped = c2.wrap(key); // wrap da AES key
            FileOutputStream kos = new FileOutputStream(nome + ".chave_secreta");
            kos.write(keyWrapped);
            kos.close();

            fos.close();
            fis.close();

        }catch (FileNotFoundException e){
            System.out.println("Ficheiro " + nome + "não existe na diretoria");
            e.printStackTrace();
        }
    }

    public static void decifraFicheiro (String nome, String ute, String nomeChave) throws Exception{
        //(nome = nome do ficheiro cifrado)
        //decifra com a chave privada do utente
        try{
            FileInputStream fis = new FileInputStream(nome);
            FileOutputStream fos = new FileOutputStream(nome + ".decifrado");

            //buscar a chave privada do utente a sua keystore:
            String keystoreFileName = "keystore." + ute;
            File keystoreFile = new File(keystoreFileName);
            if (keystoreFile.exists()){ //se a keystore existir:
                FileInputStream kfile = new FileInputStream("keystore." + ute);
                KeyStore kstore = KeyStore.getInstance("PKCS12");
                kstore.load(kfile, "1234567890".toCharArray()); // Passwrd
                Key myPrivateKey = kstore.getKey("manuel", "1234567890".toCharArray()); // chave PRIVADA
                PrivateKey pk = (PrivateKey) myPrivateKey;

                //ler a key: Verificar se a keyfile existe...
                byte[] keyEncoded = new byte[128/8];
                FileInputStream kfile1 = new FileInputStream(nomeChave);
                kfile1.read(keyEncoded);
                kfile1.close();
                
                //Fazer o unwrap da key:
                Cipher c = Cipher.getInstance("RSA");
                c.init(Cipher.UNWRAP_MODE, pk); //unwrap cm a private key do utente
                Key keyUnwraped = c.unwrap(keyEncoded, "AES", Cipher.SECRET_KEY);

                





                    

                
            }else{
                System.out.println("keystore." + ute + " não existe na diretoria");
            }



        }catch (FileNotFoundException e){
            System.out.println("Ficheiro " + nome + "não existe na diretoria");
        }
    }




    
    public static void main (String[] args) throws Exception{
        String[] serverAdress; 
        String userMed;
        String userUte;
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<String> files = new ArrayList();

        String op = "NoOp";

        //Parse os Agrs:
        //Guarda o serverAdress:
        serverAdress = args[1].split(":");
        //guardar userMedico, userUtente e op:
        userMed = args[3];
        userUte = args[5];
        op = args[6];
        //Guarda o nome dos ficheiros
        for(int i = 7; i < args.length; i++){
            files.add(args[i]);
        }

        //criação do socket:
        Socket socket = new Socket(serverAdress[0], (Integer.parseInt(serverAdress[1])));


        System.out.println((serverAdress[0] + ", " + serverAdress[1]));
        System.out.println(userMed);
        System.out.println(userUte);
        System.out.println(op);
        System.out.println(files);



        if (op.equals("-sc")){
            System.out.println("Opção -sc Escolhida");
            //Iteramos sobre as files
            if (files.size() > 0){
                for (int f = 0; f < files.size(); f ++){
                    cifraFicheiro(files.get(f), userMed, userUte);
                }

                //Envia ficheiro cifrado e a chave wrapped:

            }else{
                System.out.println("Não existem ficheiros");
            }
        }
    }  
}