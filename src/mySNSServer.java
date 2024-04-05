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

            // Lê o nome do paciente
            String patientName = dataInputStream.readUTF();
            System.out.println("Recebendo dados para o paciente: " + patientName);

            // checks if directory exists and creates it if it doesn't
            File patientDirectory = checkOrCreateDirectory(patientName);
            System.out.println("diretorio criado e tudo bem");
            
            while (true) {
                String command = dataInputStream.readUTF();
                
                if ("FIM DO ENVIO DE FICHEIROS".equals(command)) {
                    System.out.println("Todos os ficheiros e chaves foram recebidos.");
                    break;
                } else if (command.startsWith("inicio do envio do ficheiro")) {
                    String filename = command.split(":")[1].trim();
                    saveEncryptedFile(dataInputStream, patientDirectory, filename);
                } else if (command.startsWith("inicio do envio da chave secreta encriptada")) {
                    String filename = command.split(":")[1].trim();
                    saveEncryptedKey(dataInputStream, patientDirectory, filename);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar a conexão do cliente: " + e.getMessage());
        }
    }

    private static void saveEncryptedFile(DataInputStream dataInputStream, File patientDirectory, String filename) throws IOException {
        // Aqui, você precisará definir a lógica para salvar o arquivo cifrado.
        // Por exemplo, você pode primeiro ler o tamanho do arquivo, seguido dos dados do arquivo.
        System.out.println("Salvando arquivo cifrado: " + filename);
    }

    private static void saveEncryptedKey(DataInputStream dataInputStream, File patientDirectory, String filename) throws IOException {
        // Aqui, você definiria a lógica para salvar a chave cifrada, possivelmente começando por ler o tamanho da chave.
        System.out.println("Salvando chave cifrada para: " + filename);
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
