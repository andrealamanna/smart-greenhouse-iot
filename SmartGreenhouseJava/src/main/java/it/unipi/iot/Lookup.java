package it.unipi.iot;

import java.net.InetAddress;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class Lookup extends CoapResource {

	public Lookup(String name) {
		super(name);
	}

	public void handleGET(CoapExchange exchange) {

		InetAddress inetAddress = exchange.getSourceAddress();
		
		String zoneName;
		
		try {
			zoneName = exchange.getQueryParameter("zone");
		} catch (Exception e) {
			System.out.println("Bad query parameter format");
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}
		if (zoneName == null || zoneName.isEmpty()) {
			System.out.println("Bad query parameter format");
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}
		
		Zone zone = MainApp.zones.get(zoneName);
		
		if (zone == null || zone.getActuators().size() == 0) {
			System.out.println("Zone not found!");
			exchange.respond(ResponseCode.NOT_FOUND);
			return;
		}
	
		exchange.respond(ResponseCode.VALID);
		
		String actuators = "actuators=";
		
		for (String actuator: zone.getActuators().keySet()) {
			actuators+=actuator+",";
		}
		actuators=actuators.substring(0, actuators.length()-1);
		
		System.out.println(actuators);
		
		CoapClient client = new CoapClient("coap://[" + inetAddress.getHostAddress() + "]/humidity");
		CoapResponse response = client.put(actuators, MediaTypeRegistry.TEXT_PLAIN);		
		
		if (!response.isSuccess()) {
			System.out.println("Error sending actuators list!");
		}
		else {
			System.out.println("Actuators list added");
		}
	}
	
	public void handlePOST(CoapExchange exchange) {
		
		String zoneName = new String(exchange.getRequestText());
		System.out.println(exchange.getRequestPayload().toString());
		zoneName = zoneName.split("zone=")[1];
		
		if (zoneName == null || zoneName.isEmpty()) {
			System.out.println("Bad query parameter format");
			exchange.respond(ResponseCode.BAD_REQUEST);
			return;
		}
		
		Zone zone = MainApp.zones.get(zoneName);
		
		if (zone == null || zone.getActuators().size() == 0) {
			System.out.println("Zone not found!");
			exchange.respond(ResponseCode.NOT_FOUND);
			return;
		}
		
		Response clientResponse = new Response(ResponseCode.CONTENT);
		clientResponse.setPayload("Checking for actuator error");
		exchange.respond(clientResponse);

		CoapClient client = new CoapClient(zone.getActuators().keySet().iterator().next());
		
		int expTime = 1;
		boolean active = false;
		int retry = 3;
		
		while(!active && retry>0) {
			try {
				Thread.sleep((int) (expTime * 100 * Math.random())); //waiting before ping to actuator
				System.out.println("Try to ping actuator "+client.getURI());
				active = client.ping();
				expTime*=2;
				retry--;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		if (!active || retry == 0) {
			for (String nodeAddress: zone.getHumiditySensors().keySet()) {
				CoapClient sensorClient = new CoapClient(nodeAddress);
				CoapResponse sensoreResponse = sensorClient.put("actuators=None", MediaTypeRegistry.TEXT_PLAIN);		
				
				if (!sensoreResponse.isSuccess()) {
					System.out.println("Error removing actuators list!");
				}
				else {
					System.out.println("Actuators list removed");
				}
			}
		}
	}

}