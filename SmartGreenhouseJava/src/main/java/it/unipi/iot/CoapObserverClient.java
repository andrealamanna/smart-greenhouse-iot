package it.unipi.iot;

import java.util.ArrayList;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

public class CoapObserverClient extends CoapClient {
	private HumidityResource humidityResource;
	private TemperatureResource temperatureResource;
	CoapObserveRelation coapObserveRelation;

	public CoapObserverClient(HumidityResource humidityResource) {
		super(humidityResource.getResourceURI());
		this.humidityResource = humidityResource;
	}
	
	public CoapObserverClient(TemperatureResource temperatureResource) {
		super(temperatureResource.getResourceURI());
		this.temperatureResource = temperatureResource;
	}

	public void startObserving() {
		coapObserveRelation = this.observe(new CoapHandler () {
			public void onLoad(CoapResponse response) {
				try {
					String value;
					JSONObject jo = (JSONObject) JSONValue.parseWithException(response.getResponseText());
					if (jo.containsKey("humidity")) {
						value = jo.get("humidity").toString();
						ArrayList<String> resourceValues = humidityResource.getHumidityValues();
						resourceValues.add(value);
						MainApp.humidityResources.get(MainApp.humidityResources.indexOf(humidityResource))
								.setHumidityValues(resourceValues);
					}else if (jo.containsKey("temperature")) {
						value = jo.get("temperature").toString();
						ArrayList<String> resourceValues = temperatureResource.getTemperatureValues();
						resourceValues.add(value);
						MainApp.temperatureResources.get(MainApp.temperatureResources.indexOf(temperatureResource))
								.setTemperatureValues(resourceValues);
					}
					else {
						System.out.println("Sensor value not found.");
						return;
					}	
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
	
			public void onError() {
				System.out.println("Observing Error Detected.");
			}
		});
	}
}
