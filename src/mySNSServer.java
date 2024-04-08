import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class mySNSServer {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java mySNSServer <porta>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor à escuta na porta " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                    handleClientConnection(clientSocket);
                } catch (IOException e) {
                    System.err.println("Erro ao lidar com o cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        try {
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            OutputStream outputStream = clientSocket.getOutputStream();

            // receive the mode of operation
            String mode = dataInputStream.readUTF();
            System.out.println("Modo de operação: " + mode);

            switch (mode){
                case "sc":
                        // Lê o nome do paciente
                    String patientName = dataInputStream.readUTF();
                    System.out.println("A receber dados para o/a paciente: " + patientName);

                    // checks if directory exists and creates it if it doesn't
                    File patientDirectory = checkOrCreateDirectory(patientName);

                    while (true) {
                        String command = dataInputStream.readUTF();
                        System.out.println("Comando recebido: " + command);
                        
                        if ("FIM DO ENVIO DE FICHEIROS".equals(command)) {
                            System.out.println("Todos os ficheiros e chaves foram recebidos.");
                            break;
                        } else if (command.startsWith("inicio do envio do ficheiro")) {
                            System.out.println("entrou inicio do envio do ficheiro");
                            String[] parts = command.split(":");
                            String filename = parts[1].trim();
                            System.out.println("filename: " + filename);
                            saveEncryptedFile(dataInputStream, patientDirectory, filename, new DataOutputStream(outputStream));
                        } else if (command.startsWith("inicio do envio da chave secreta encriptada")) {
                            System.out.println("entrou inicio do envio da chave secreta encriptada");
                            String[] parts = command.split(":");
                            String filename = parts[1].trim();
                            saveEncryptedKey(dataInputStream, patientDirectory, filename, new DataOutputStream(outputStream));
                        }
                    }
                    break;

                case "sa":
                    System.out.println("entrou no sa");

                case "se":
                    System.out.println("Entrou na op -se");

                case "g":
                    System.out.println("Entrou na op -g");
                }

    
        } catch (IOException e) {
            System.err.println("Erro ao processar a conexão do cliente: " + e.getMessage());
        }
    }


    private static void saveEncryptedFile(DataInputStream dataInputStream, File patientDirectory, String filename, DataOutputStream dataOutputStream) throws IOException {
        File newFile = new File(patientDirectory, filename + ".cifrado");
        System.out.println("Verificando existência do arquivo cifrado: " + newFile.getName());
        
        // Verifica se o arquivo já existe no diretório
        if (newFile.exists()) {
            System.out.println("Ficheiro já existe no servidor: " + newFile.getName());
            
            // Envia mensagem ao cliente informando que o arquivo já existe
            dataOutputStream.writeUTF("Ficheiro já existe no servidor: " + newFile.getName());
            dataOutputStream.flush();
            
            // Recebe os bytes do arquivo para limpar o buffer, mas não faz nada com esses bytes
            // Lê o tamanho do arquivo que será recebido
            int length = dataInputStream.readInt();

            // Lê exatamente fileSize bytes, que é o arquivo completo
            // Cria um buffer do tamanho exato do arquivo a ser lido
            byte[] buffer = new byte[length];
            
            // Lê o arquivo inteiro de uma vez
            dataInputStream.readFully(buffer, 0, length);

        } else {
            System.out.println("A guardar novo ficheiro cifrado: " + newFile.getName());
            
            // Recebe e guarda o ficheiro cifrado
            try (FileOutputStream fos = new FileOutputStream(newFile)) {
                int length = dataInputStream.readInt(); // Lê o tamanho do arquivo como um inteiro
                System.out.println("tamanho do ficheiro: " + length + " bytes");
            
                // Cria um buffer do tamanho exato do arquivo a ser lido
                byte[] buffer = new byte[length];
            
                // Lê o arquivo inteiro de uma vez
                dataInputStream.readFully(buffer, 0, length);
            
                // Escreve o arquivo inteiro para o sistema de arquivos
                fos.write(buffer);
            }

            //send message to client that file was saved
            dataOutputStream.writeUTF("Ficheiro cifrado salvo com sucesso: " + newFile.getName());
        }
    }



    private static void saveEncryptedKey(DataInputStream dataInputStream, File patientDirectory, String filename, DataOutputStream dataOutputStream) throws IOException {
        // Constrói o nome do arquivo onde a chave criptografada será salva
        String encryptedKeyName = filename + ".chave_secreta." + patientDirectory.getName();
        File encryptedKeyFile = new File(patientDirectory, encryptedKeyName);
    
        System.out.println("Verificando existência da chave criptografada: " + encryptedKeyFile.getName());
    
        // Verifica se o arquivo da chave já existe no diretório
        if (encryptedKeyFile.exists()) {
            System.out.println("Chave já existe no servidor: " + encryptedKeyFile.getName());
            
            // Envia mensagem ao cliente informando que a chave já existe
            dataOutputStream.writeUTF("Chave já existe no servidor: " + encryptedKeyFile.getName());
            dataOutputStream.flush();
            
            // Recebe o tamanho da chave criptografada que será recebida
            int length = dataInputStream.readInt();
    
            // Consome os bytes da chave para limpar o buffer, mas não salva, pois já existe
            byte[] buffer = new byte[length];
            dataInputStream.readFully(buffer, 0, length);
    
        } else {
            System.out.println("A guardar nova chave cifrada: " + encryptedKeyFile.getName());
    
            // Recebe e guarda a chave cifrada
            try (FileOutputStream fos = new FileOutputStream(encryptedKeyFile)) {
                int length = dataInputStream.readInt(); // Lê o tamanho da chave criptografada como um inteiro
                System.out.println("Tamanho da chave: " + length + " bytes");
            
                // Cria um buffer do tamanho exato da chave a ser lida
                byte[] buffer = new byte[length];
            
                // Lê a chave inteira de uma vez
                dataInputStream.readFully(buffer, 0, length);
            
                // Escreve a chave inteira no sistema de arquivos
                fos.write(buffer);
            }
    
            // Envia mensagem ao cliente informando que a chave foi salva
            dataOutputStream.writeUTF("Chave cifrada guardada com sucesso: " + encryptedKeyFile.getName());
        }
    }
    

    //checks if directory exists and creates it if it doesn't
    //returns the directory
    public static File checkOrCreateDirectory(String patientName) {
        File directory = new File(patientName);
        
        if (directory.exists()) {
            System.out.println("O diretório com o nome '" + patientName + "' já existe.");
        } else {
            boolean wasCreated = directory.mkdirs();
            if (wasCreated) {
                System.out.println("O diretório '" + patientName + "' foi criado com sucesso.");
            } else {
                System.out.println("Não foi possível criar o diretório '" + patientName + "'.");
            }
        }
        
        return directory;
    }
    
}


//comando para compilar: javac mySNSServer.java
//comando para executar: java mySNSServer 23456