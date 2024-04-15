import java.io.BufferedInputStream;
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
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
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
                Key myPrivateKey = kstore.getKey("manel", "1234567890".toCharArray()); // chave PRIVADA
                PrivateKey pk = (PrivateKey) myPrivateKey;

                //ler a key: Verificar se a keyfile existe...
                    //byte[] keyEncoded = new byte[2048];
                FileInputStream keyFileInputStream = new FileInputStream(nomeChave);
                byte[] wrappedKey = keyFileInputStream.readAllBytes();
                System.out.println("KeyEncodedRSA: " + wrappedKey.toString());
                keyFileInputStream.close();
                
                //Fazer o unwrap da key:
                Cipher c = Cipher.getInstance("RSA");
                c.init(Cipher.UNWRAP_MODE, pk); //unwrap cm a private key do utente
                Key keyUnwraped = c.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
                byte[] keyEncoded2 = keyUnwraped.getEncoded();
                SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded2, "AES");


                Cipher c2 = Cipher.getInstance("AES");
                c2.init(Cipher.DECRYPT_MODE, keySpec2);

                //Decifrar:
                CipherInputStream cis = new CipherInputStream(fis, c2);
                byte[] b = new byte[64];  //aumentar este tamanho com testes maiores !!!
                int i = cis.read(b);
                while(i != -1){
                    fos.write(b,0,i);
                    i = cis.read(b);
                }

                cis.close();
                fos.close();
                fis.close();
            }else{
                System.out.println("keystore." + ute + " não existe na diretoria");
            }

        }catch (FileNotFoundException e){
            System.out.println("Ficheiro " + nome + "não existe na diretoria");
        }
    }

    public static void assinaFicheiro (String nome, String med) throws Exception{ //Verificar se o fich existe
        FileInputStream fis = new FileInputStream(nome);
        FileOutputStream fos = new FileOutputStream(nome + ".assinatura." + med);
        System.out.println(med);
        //Buscar a chave privada do medico:
        String keystoreFileName = "keystore." + med;
        File keystoreFile = new File(keystoreFileName);
        if (keystoreFile.exists()){
            FileInputStream kfile = new FileInputStream("keystore." + med);
            KeyStore kstore = KeyStore.getInstance("PKCS12");
            kstore.load(kfile, "1234567890".toCharArray()); // Passwrd
            Key myPrivateKey = kstore.getKey("joao", "1234567890".toCharArray()); // chave PRIVADA
            PrivateKey pk = (PrivateKey) myPrivateKey;

            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(pk);

            //Fazer os updates com a signature
            byte[] b = new byte[16];
            int bytesRead;
            while ((bytesRead = fis.read(b)) != -1) {
                s.update(b, 0, bytesRead);
            }
            fis.close();
            //Escrevemos a signature para o fos:
            fos.write(s.sign());

        }else{
            System.out.println("Keystore não existe na diretoria");
        }

        fos.close();
    }

    public static boolean verificaAssinatura(String nome, String med) throws Exception {
        boolean verify = false;
        System.out.println(med);
        System.out.println(nome);
        // verifica se a assin existe 
        String signatureFileName = nome + ".assinatura." + med;
        File signatureFile = new File(signatureFileName);
        System.out.println(signatureFileName);
        if (!signatureFile.exists()) {
            System.out.println("Ficheiro assinado não existe na diretoria, " + signatureFileName);
            return false;
        }
    
        // le o ficheiro original 
        FileInputStream fis = new FileInputStream(nome);
    
        // le o ficheiro assinado
        FileInputStream sigFis = new FileInputStream(signatureFileName);
        byte[] signature = sigFis.readAllBytes();
        sigFis.close();
    
        // buscar o cert da keystore
        String keystoreFileName = "keystore." + med;
        File keystoreFile = new File(keystoreFileName);
        if (keystoreFile.exists()) {
            FileInputStream kfile = new FileInputStream(keystoreFileName);
            KeyStore kstore = KeyStore.getInstance("PKCS12");
            kstore.load(kfile, "1234567890".toCharArray()); // Password
    
            // vai buscar o cert usando o alias do medico
            Certificate cert = kstore.getCertificate(med);
            if (cert != null) {
                PublicKey publicKey = cert.getPublicKey();
    
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
    
                // le e faz o update da file
                byte[] buffer = new byte[8192]; // 8 KB buffer
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    sig.update(buffer, 0, bytesRead);
                }
                fis.close();
    
                // verifica a signature
                verify = sig.verify(signature);
                System.out.println("Resultado da verificação: " + verify);
            } else {
                System.out.println("Certificado não encontrado para o alias: " + med);
            }
        } else {
            System.out.println("Keystore não existe: " + keystoreFileName);
        }
    
        return verify;
    }

    public static void seguro(String nome, String med, String ute) throws Exception{
        //Cifra ficheiros com o .seguro 
        //Asina ficheiros com o .assinado."""med"""

        cifraFicheiro(nome, med, ute);
        assinaFicheiro(nome, med);
        //Rename a file.cifrado para file.seguro e verifica que file.cifrado existe
        File cifradoAntigo = new File(nome+".cifrado");
        File cifradoNovo = new File (nome+".seguro");
        if (cifradoAntigo.exists()){
            cifradoAntigo.renameTo(cifradoNovo);
            System.out.println(nome+".seguro criado com sucesso!");
        }else{
            System.out.println(nome+".cifrado não existe na diretoria, não é possivel criar o ficheiro:" + nome+".seguro");
        }
        File assinado = new File(nome+".assinado."+med);
        if (assinado.exists()){
            System.out.println(nome+".assinado."+med +"criado com sucesso!");
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

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        if (op.equals("-sc")){
            System.out.println("Opção -sc Escolhida");
            //Iteramos sobre as files
            if (files.size() > 0){
                for (int f = 0; f < files.size(); f ++){
                    //ciframos o fichero:
                    cifraFicheiro(files.get(f), userMed, userUte);

                    //Enviar op, med e ute ao servidor:
                    out.writeObject(op);
                    out.writeObject(userMed);
                    out.writeObject(userUte);

                    //Verificar que file.cifrado e file.chave_secreta existem:
                    File cifrado = new File(files.get(f)+".cifrado");
                    File chave = new File(files.get(f)+".chave_secreta");
                    if (cifrado.exists() && chave.exists()){
                        //mandar file_size e file_name:
                        out.writeObject(cifrado.length());
                        out.writeObject(cifrado.getName());

                        //mandar file.cifrado:
                        FileInputStream cfile = new FileInputStream(cifrado);
                        BufferedInputStream fileC = new BufferedInputStream(cfile);

                        byte[] buffer = new byte[1024];
                        int t = 0;
                        while ((t = fileC.read(buffer, 0, 1024)) > 0) {
                            out.write(buffer, 0, t);
                        }

                        fileC.close();
                        cfile.close();

                        //mandar file.chave_secreta:
                        //mandar size e nome da file.chave_secreta:
                        out.writeObject(chave.length());
                        out.writeObject(chave.getName());

                        System.out.println("A Enviar: " + chave.getName() + ", com a size: " + chave.length());

                        //mandar file.chave_secreta:
                        FileInputStream keyFileInputStream = new FileInputStream(chave);

                        byte[] buf = new byte[(int) chave.length()];
                        keyFileInputStream.read(buf, 0, buf.length);
                        out.write(buf, 0, buf.length);

                        keyFileInputStream.close();
                    }else{
                        System.out.println(files.get(f)+".cifrado/.chave_secreta não existe na diretoria");
                        break; //? Aqui secalhar mandamos td a zeros?
                    }
                }
                out.writeObject("END");
                out.writeObject("Dr.END");
                out.writeObject("Mr.END");
            }else{
                System.out.println("Não existem ficheiros");
            }
        }
        
        else if (op.equals("-sa")){
            System.out.println("Opção -sa escolhida");
            if (files.size() > 0){
                for (int f = 0; f < files.size(); f ++){
                    assinaFicheiro(files.get(f), userMed);

                    //Enviar op, med e ute ao servidor:
                    out.writeObject(op);
                    out.writeObject(userMed);
                    out.writeObject(userUte);

                    //Verificar que file.assinado e file.og existem:
                    File assinado = new File(files.get(f)+".assinatura."+userMed);
                    File og = new File(files.get(f));
                    if (assinado.exists() && og.exists()){
                        //mandar assinado_size e assinado_name:
                        out.writeObject(assinado.length());
                        out.writeObject(assinado.getName());

                        //mandar file.assinado.userMed:
                        FileInputStream afile = new FileInputStream(assinado);
                        BufferedInputStream fileA = new BufferedInputStream(afile);

                        byte[] buffer = new byte[1024];
                        int t = 0;
                        while ((t = fileA.read(buffer, 0, 1024)) > 0) {
                            out.write(buffer, 0, t);
                        }

                        fileA.close();
                        afile.close();

                        //mandar original file:
                        //mandar o size e o nome:
                        out.writeObject(og.length());
                        out.writeObject(og.getName());

                        //mandar original file:
                        FileInputStream ogfile = new FileInputStream(og);
                        BufferedInputStream fileOG = new BufferedInputStream(ogfile);

                        byte[] buffer1 = new byte[1024];
                        int i = 0;
                        while ((i = fileOG.read(buffer1, 0, 1024)) > 0) {
                            out.write(buffer1, 0, i);
                        }

                        fileOG.close();
                        ogfile.close();


                    } // se alguma file n existir
                }
                out.writeObject("END");
                out.writeObject("Dr.END");
                out.writeObject("Mr.END");

            }else{
                System.out.println("Não existem ficheiros");
            }
            
        }
        else if (op.equals("-se")){
            System.out.println("Opção -se escolhida");
            if (files.size() > 0){
                for (int f = 0; f < files.size(); f ++){
                    seguro(files.get(f), userMed, userUte);

                    //Enviar op, med e ute ao servidor:
                    out.writeObject(op);
                    out.writeObject(userMed);
                    out.writeObject(userUte);

                    //Mandar file.seguro, file.chave_secreta, file.assinatura.userMed e og file
                    //Verificar que todas existem na nossa dir:
                    File seguro = new File(files.get(f)+".seguro");
                    File chave = new File(files.get(f)+".chave_secreta");
                    File assinatura = new File(files.get(f)+".assinatura."+userMed);
                    File og = new File(files.get(f));
                    if (seguro.exists() && chave.exists() && assinatura.exists() && og.exists()){
                        //enviar seguro size e seguro nome:
                        out.writeObject(seguro.length());
                        out.writeObject(seguro.getName());

                        //mandar file seguro:
                        FileInputStream sfile = new FileInputStream(seguro);
                        BufferedInputStream fileS = new BufferedInputStream(sfile);

                        byte[] buffer = new byte[1024];
                        int t = 0;
                        while ((t = fileS.read(buffer, 0, 1024)) > 0) {
                            out.write(buffer, 0, t);
                        }

                        fileS.close();
                        sfile.close();

                        //mandar chave size e chave nome:
                        out.writeObject(chave.length());
                        out.writeObject(chave.getName());

                        //mandar file chave:
                        FileInputStream keyFileInputStream = new FileInputStream(chave);

                        byte[] buf = new byte[(int) chave.length()];
                        keyFileInputStream.read(buf, 0, buf.length);
                        out.write(buf, 0, buf.length);

                        keyFileInputStream.close();

                        //mandar assinado size e nome:
                        out.writeObject(assinatura.length());
                        out.writeObject(assinatura.getName());

                        //mandar file.assinatura.userMed:
                        FileInputStream assfile = new FileInputStream(assinatura);
                        BufferedInputStream fileAss = new BufferedInputStream(assfile);

                        byte[] buffer3 = new byte[1024];
                        int r = 0;
                        while ((r = fileAss.read(buffer3, 0, 1024)) > 0) {
                            out.write(buffer3, 0, r);
                        }

                        fileAss.close();
                        assfile.close();

                        //mandar og size e nome:
                        out.writeObject(og.length());
                        out.writeObject(og.getName());

                        //mandar original file:
                        FileInputStream ogfile = new FileInputStream(og);
                        BufferedInputStream fileOG = new BufferedInputStream(ogfile);

                        byte[] buffer4 = new byte[1024];
                        int o = 0;
                        while ((o = fileOG.read(buffer4, 0, 1024)) > 0) {
                            out.write(buffer4, 0, o);
                        }

                        fileOG.close();
                        ogfile.close();


                    }//if alguma das files não existe na dir ´
                    System.out.println("Alguma file na op -sa não existe na dir");
                }
                out.writeObject("END");
                out.writeObject("Dr.END");
                out.writeObject("Mr.END");
            }else{
                System.out.println("Não existem ficheiros");
            }

        }
        else if (op.equals("-g")){

            //Recebe ficheiros 

            System.out.println("Opção -g escolhida");
            if (files.size() > 0){
                for (int f = 0; f < files.size(); f ++){
                    //decifraFicheiro(files.get(f) +".cifrado", userUte, files.get(f)+".chave_secreta");
                    if (verificaAssinatura(files.get(f), userMed)){
                        System.out.println("Assinatura verificada com sucesso, foi o medico que assinou!");
                    }else{
                        System.out.println("Assinatura não verificada, ficheiro comprometido!");
                    }
                }

            }else{
                System.out.println("Não existem ficheiros");
            }

            
        }
    }  
}
