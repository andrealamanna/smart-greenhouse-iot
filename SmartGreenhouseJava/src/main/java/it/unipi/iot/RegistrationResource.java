package it.unipi.iot;

import java.net.InetAddress;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class RegistrationResource extends CoapResource {

	public RegistrationResource(String name) {
		super(name);
	}
	
	public void handlePOST(CoapExchange exchange) {

		Response clientResponse = new Response(ResponseCode.CONTENT);
		clientResponse.setPayload( "Welcome" );
		exchange.respond(clientResponse);
		
		//wait before Resource Discovery
		/*try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
		
		InetAddress inetAddress = exchange.getSourceAddress();
		System.out.println(inetAddress.toString());
		CoapClient client = new CoapClient("coap://[" + inetAddress.getHostAddress() + "]:5683/.well-known/core");
		CoapResponse response = null;
		int expTime = 1;
		
		while(response == null || !response.isSuccess()) {
			try {
				Thread.sleep((int) (expTime * 100 * Math.random())); //waiting before Resource Discovery
				response = client.get();
				expTime*=2;
			}
			catch(Exception e) {
				response=null;
				e.printStackTrace();
			}
		}
		
		String responseText = response.getResponseText();
		if(responseText.contains("</lamp>"))
			responseText=responseText.substring(0,responseText.indexOf("</lamp>"));
		System.out.println(responseText);
		String resource=responseText.split("<")[2].split(">")[0];
		String attributes=responseText.substring(responseText.lastIndexOf(">")+1);
		String attRt="";
		String attIf="";
		boolean obs = false;
		
		for (String attribute : attributes.split(";")) {
			if (attribute.contains("rt"))
				attRt=attribute.substring(attribute.indexOf("rt")+3);
			if (attribute.contains("if"))
				attIf=attribute.substring(attribute.indexOf("if")+3);
			if (attribute.contains("obs"))
				obs=true;
		}
		
		System.out.println("Registration resource:" +resource);

		if (attIf.equals("\"sensor\"")) {
			if (resource.contains("temperature")) {
				TemperatureResource temperatureResource = new TemperatureResource(resource, inetAddress.getHostAddress());
				if (!MainApp.temperatureResources.contains(temperatureResource)) {
					MainApp.temperatureResources.add(temperatureResource);
					if (obs)
						observeTemperature(temperatureResource);
				}
				LampResource lampResource = new LampResource("/lamp", inetAddress.getHostAddress());
				if (!MainApp.lampResources.contains(lampResource)) {
					MainApp.lampResources.add(lampResource);
				}
			}
			if (resource.contains("humidity")) {
				HumidityResource humidityResource = new HumidityResource(resource, inetAddress.getHostAddress());
				if (!MainApp.humidityResources.contains(humidityResource)) {
					MainApp.humidityResources.add(humidityResource);
					if (obs)
						observeHumidity(humidityResource);
				}
			}
		}
		if (attIf.equals("\"actuator\"")) {
			if (resource.contains("irrigation")) {
				IrrigationResource irrigationResource = new IrrigationResource(resource, inetAddress.getHostAddress());
				if (!MainApp.irrigationResources.contains(irrigationResource)) {
					MainApp.irrigationResources.add(irrigationResource);
					if (obs)
						observeIrrigation(irrigationResource);
				}	
			}
			if (resource.contains("lamp")) {
				LampResource lampResource = new LampResource(resource, inetAddress.getHostAddress());
				if (!MainApp.lampResources.contains(lampResource)) {
					MainApp.lampResources.add(lampResource);
				}
				TemperatureResource temperatureResource = new TemperatureResource("/temperature", inetAddress.getHostAddress());
				if (!MainApp.temperatureResources.contains(temperatureResource)) {
					MainApp.temperatureResources.add(temperatureResource);
						observeTemperature(temperatureResource);
				}
			}
		}
		
	}

	private static void observeHumidity(HumidityResource humidityResource) {
		MainApp.coapObserverClients.add(new CoapObserverClient(humidityResource));
		MainApp.coapObserverClients.get(MainApp.coapObserverClients.size() - 1).startObserving();
	}

	private static void observeTemperature(TemperatureResource temperatureResource) {
		MainApp.coapObserverClients.add(new CoapObserverClient(temperatureResource));
		MainApp.coapObserverClients.get(MainApp.coapObserverClients.size() - 1).startObserving();
	}
	
	private static void observeIrrigation(IrrigationResource irrigationResource) {
		//TODO method for future developments
		throw new UnsupportedOperationException("Obs actuator not implemented, yet!");
	}
}