import java.io.BufferedOutputStream;
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
 
		@SuppressWarnings("unchecked")
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String userMed;
				String userUte;

				String op;

				@SuppressWarnings("rawtypes")
				List<String> files = new ArrayList();
				
				try {
					userMed = (String)inStream.readObject();
					userUte = (String)inStream.readObject();
					op = (String)inStream.readObject();
					files = (ArrayList<String>)inStream.readObject();
					System.out.println(userMed);
					System.out.println(userUte);
					System.out.println(op);
					System.out.println(files);
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
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

				outStream.close();
				inStream.close();
 			
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

