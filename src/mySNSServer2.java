import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class mySNSServer2 {
    
    public static void main (String[] args){
        System.out.println("servidor: main");
		mySNSServer2 server = new mySNSServer2();
		server.startServer();
    }

    public void startServer (){
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(23456); //Porta de Escuta
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();  //Soc aceita a ligaзгo do cliente 
				ServerThread newServerThread = new ServerThread(inSoc); //criaзгo de uma thread para responder ao cliente 
				newServerThread.start(); //come?ar a thread 
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
		//sSoc.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}
 
		public void run(){
			try {

				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				while (true){
					
					String userMed = "";
					String userUte = "";

					String op = "";
					
					try {
						op = (String)inStream.readObject();
						userMed = (String)inStream.readObject();
						userUte = (String)inStream.readObject();
						System.out.println("Medico: " + userMed);
						System.out.println("Utente: " + userUte);
						System.out.println("op: " + op);
					}catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}

					if(op.equals("END")){
						break;
					}

					File dir = new File(userUte);
					if (!dir.exists()){
						// Create the directory and all parent directories if they don't exist
						boolean created = dir.mkdirs();
						if (created) {
							System.out.println("Directory created successfully.");
						} else {
							System.out.println("Failed to create directory.");
						}
					} else {
						System.out.println("Directory already exists.");
					}

					if (op.equals("-sc")){
						//TO-DO: Verificar se a file.cifrado e a file.chave_secreta ja existem na diretoria de userUte.
						//Receber ficheiro cifrado:
						//Receber o size do ficheiro. cifrado e nome do ficheiro.cifrado:
						Long cifradoSize = 0L;
						String cifradoNome = "";
						try{
							cifradoSize = (Long)inStream.readObject();
							cifradoNome = (String)inStream.readObject();
						}catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						System.out.println("Recebido: " + op+ ", " + cifradoNome + ", size: " + cifradoSize);
						
						//Receber file.cifrado:
						FileOutputStream outFileStream = new FileOutputStream(userUte + "/" + cifradoNome);
						BufferedOutputStream outFile = new BufferedOutputStream(outFileStream);

						int file_s = cifradoSize.intValue();
						byte[] buffer = new byte[1024];
						int bytesRead;
						while (file_s > 0) {
							bytesRead = inStream.read(buffer, 0, Math.min(buffer.length, file_s));
							outFile.write(buffer, 0, bytesRead);
							file_s -= bytesRead;
						}
						outFile.flush();
						outFile.close();
						outFileStream.close();

						//Receber chave wrapped:
						//Receber o size do ficheiro.chave_secreta e o nome do ficheiro.chave_secreta:
						Long chaveSize = 0l;
						String chaveNome = "";
						try{
							chaveSize = (Long)inStream.readObject();
							chaveNome = (String)inStream.readObject();
						}catch (ClassNotFoundException e){
							e.printStackTrace();
						}
						System.out.println("Recebido: " + op + ", " + chaveNome + ", size: " + chaveSize);

						//Receber ficheiro.chave_secreta:
						byte[] keyFileBytes = new byte[1024];
						FileOutputStream keyFileOutputStream = new FileOutputStream(userUte+"/" + chaveNome);
						int keyFileLength;
						while ((keyFileLength = inStream.read(keyFileBytes)) > 0) {
							keyFileOutputStream.write(keyFileBytes, 0, keyFileLength);
						}
						keyFileOutputStream.close();
			
						outStream.flush();


					}
					else if (op.equals("-sa")){
						//Receber ficheiro assinado:
						//Receber size e nome:
						Long assinadoSize = 0L;
						String assinadoNome = "";

						try{
							assinadoSize = (Long)inStream.readObject();
							assinadoNome = (String)inStream.readObject();
						}catch(ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						System.out.println("Recebido: " + op + ", " + assinadoNome + ", size: " + assinadoSize);

						//Receber file.asinado.userMed:
						FileOutputStream outFileStream = new FileOutputStream(userUte + "/" + assinadoNome);
						BufferedOutputStream outFile = new BufferedOutputStream(outFileStream);

						int file_s = assinadoSize.intValue();
						byte[] buffer = new byte[1024];
						int bytesRead;
						while (file_s > 0) {
							bytesRead = inStream.read(buffer, 0, Math.min(buffer.length, file_s));
							outFile.write(buffer, 0, bytesRead);
							file_s -= bytesRead;
						}
						outFile.flush();
						outFile.close();
						outFileStream.close();

						//Receber file original:
						//Receber size e nome da og:
						Long ogSize = 0L;
						String ogNome = "";

						try{
							ogSize = (Long)inStream.readObject();
							ogNome = (String)inStream.readObject();
						}catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						System.out.println("Recebido: " + op + ", " + ogNome + ", size: " + ogSize);

						//Receber og file:
						FileOutputStream outFileStream2 = new FileOutputStream(userUte + "/" + ogNome);
						BufferedOutputStream outFile2 = new BufferedOutputStream(outFileStream2);

						int file_s2 = ogSize.intValue();
						byte[] buffer2 = new byte[1024];
						int bytesRead2;
						while (file_s2 > 0) {
							bytesRead2 = inStream.read(buffer2, 0, Math.min(buffer2.length, file_s2));
							outFile2.write(buffer2, 0, bytesRead2);
							file_s2 -= bytesRead2;
						}
						outFile2.flush();
						outFile2.close();
						outFileStream2.close();

					
					}
					else if (op.equals("-se")){
						//Receber file.seguro, file.chave_secreta, file.assinatura.userMed e original file:
						//Receber file.seguro size e nome:
						


					}
					else if (op.equals("-g")){
						System.out.println("op -g");
					}

					else if (op.equals("")){
						break;
					}
				
					// //TODO: refazer
					// //este codigo apenas exemplifica a comunicacao entre o cliente e o servidor
					// //nao faz qualquer tipo de autenticacao
					// if (user.length() != 0){
					// 	outStream.writeObject( (Boolean) true);
					// }
					// else {
					// 	outStream.writeObject( (Boolean) false);
					// }
					
					// try {
					// 	long fSize = (long)inStream.readObject();
					// 	//Receber a file 
					// 	System.out.println(fSize);
					// 	int file_s = Long.valueOf(fSize).intValue();
					// 	FileOutputStream fos = new FileOutputStream("ad2324-projeto1_v1.pdf");
					// 	BufferedOutputStream outFile = new BufferedOutputStream(fos);

					// 	byte[] buffer = new byte[1024];
					// 	int bytesLidos;

					// 	while (file_s > 0){
					// 		bytesLidos = inStream.read(buffer, 0, Math.min(buffer.length, file_s));

					// 		fos.write(buffer,0,bytesLidos);
					// 		file_s -= bytesLidos;
					// 	}
						
					// 	//Receber teste extra 
					// 	String test = (String)inStream.readObject();
					// 	System.out.println(test);

					// 	fos.close();

					// }catch (ClassNotFoundException e1) {
					// 	e1.printStackTrace();
					// }


				}

				outStream.close();
				inStream.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

