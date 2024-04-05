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
            System.out.println("Servidor à escuta em " + port);

            // loop to accept multiple clients
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                    // Processar dados recebidos do cliente
                    receiveDataFromClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Erro na conexão com o cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
        
    }

    private static void receiveDataFromClient(Socket clientSocket) {
        try {
            InputStream input = clientSocket.getInputStream();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String fileName = reader.readLine();

            if (fileName != null) {
                // Construir o caminho do arquivo com base no nome do arquivo recebido
                String encryptedFileName = fileName + ".cifrado";
                String encryptedKeyFileName = fileName + ".chave_secreta.maria";

                System.out.println("Recebendo arquivo: " + fileName);

                // Save encrypted file
                //saveReceivedFile(input, encryptedFileName);

                // Exemplo de como salvar a chave cifrada (deverá adaptar conforme a lógica de recebimento da chave)
                //saveReceivedFile(input, encryptedKeyFileName);
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar dados do cliente: " + e.getMessage());
        }
    }

}
