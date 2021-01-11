package it.unipi.iot;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapServer;

public class Server extends CoapServer {

	static {
		CaliforniumLogger.disableLogging();
	}
	
	public void startServer() {
		System.out.println("Server started...");
		this.add(new RegistrationResource("registration"));
		this.add(new Lookup("lookup"));
		this.start();
	}
}
