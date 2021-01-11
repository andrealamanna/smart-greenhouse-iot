package it.unipi.iot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class MainApp {

	public static ArrayList<CoapObserverClient> coapObserverClients = new ArrayList<CoapObserverClient>();
	public static ArrayList<TemperatureResource> temperatureResources = new ArrayList<TemperatureResource>();
	public static ArrayList<HumidityResource> humidityResources = new ArrayList<HumidityResource>();
	public static ArrayList<IrrigationResource> irrigationResources = new ArrayList<IrrigationResource>();
	public static ArrayList<LampResource> lampResources = new ArrayList<LampResource>();
	public static LinkedHashMap<String,Zone> zones = new LinkedHashMap<String,Zone>();
	
	
	public static void main(String[] args) throws IOException, InterruptedException {

		runServer();
		showOperations();

		while (true) {
			try {
				Integer selectedOperation = insertInputLine();

				switch (selectedOperation) {
				case 0:
					showResources();
					break;
				case 1:
					showResourcesInformation();
					break;
				case 2:
					showSingleResourceInformation();
					break;
				case 3:
					showLastSensorValue();
					break;
				case 4:
					updateHumidity();
					break;
				case 5:
					updateTemperature();
					break;
				case 6:
					changeIrrigationState("ON", true);
					break;
				case 7:
					changeIrrigationState("OFF", false);
					break;
				case 8:
					changeLampState("ON", true);
					break;
				case 9:
					changeLampState("OFF", false);
					break;
				case 10:
					addToZone();
					break;
				case 11:
					removeFromZone();
					break;
				case 12:
					showZone();
					break;	
				case 13:
					showZoneInformations();
					break;
				case 14:
					showZoneList();
					break;
				case 15:
					deleteZone();
					break;
				case 16:
					System.exit(0);
					break;
				default:
					showOperations();
					break;
				}

			} catch (Exception e) {
				System.out.println("Invalid input. Try Again\n");
				showOperations();
				e.printStackTrace();
			}
		}
	}

	public static void runServer() {
		new Thread() {
			public void run() {
				Server server = new Server();
				server.startServer();
			}
		}.start();
	}

	public static Integer insertInputLine() {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		try {
			String line = bufferedReader.readLine();
			Integer value = -1;
			if (isNumeric(line))
				value = Integer.parseInt(line);
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public static String readInputLine() {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String line="";
		try {
			line = bufferedReader.readLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return line;
	}

	public static boolean isNumeric(String strNum) {
		if (strNum == null)
			return false;
		try {
			@SuppressWarnings("unused")
			Integer number = Integer.parseInt(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static void showResources() {
		System.out.println("Resources List:");
		int i = 0;
		for (HumidityResource resource : humidityResources) {
			System.out.println(
					+i + "\tHumidity Resource: " + resource.getAddress() + " " + resource.getPath());			
			i++;
		}
		System.out.println();
		i = 0;
		for (TemperatureResource resource : temperatureResources) {
			System.out.println(
					+i + "\tTemperature Resource: " + resource.getAddress() + " " + resource.getPath());			
			i++;
		}
		System.out.println();
		i = 0;
		for (IrrigationResource resource : irrigationResources) {
			System.out.println(
					+i + "\tIrrigation Resource: " + resource.getAddress() + " " + resource.getPath());			
			i++;
		}
		System.out.println();
		i = 0;
		for (LampResource resource : lampResources) {
			System.out.println(
					+i + "\tLamp Resource: " + resource.getAddress() + " " + resource.getPath());			
			i++;
		}
	}

	public static void showResourcesInformation() {
		System.out.println("Resources Information: \n");
		int i=0;
		for (HumidityResource resource : humidityResources) {
			System.out.println(i + "\t" + resource.getAddress() + " " + resource.getPath()+ "\n");
			for (String value : resource.getHumidityValues())
				System.out.println("\t\tValue: " + value);
		}
		i=0;
		for (TemperatureResource resource : temperatureResources) {
			System.out.println(i + "\t" + resource.getAddress() + " " + resource.getPath()+ "\n");
			for (String value : resource.getTemperatureValues())
				System.out.println("\t\tValue: " + value);
		}
		i=0;
		for (LampResource resource : lampResources) {
			String stateValue = resource.getState() ? "ON" : "OFF";
			System.out.println(i + "\t" + resource.getAddress() + " " + resource.getPath()
			+ "\n\t\tState: " + stateValue + "\n");
		}
		i=0;
		for (IrrigationResource resource : irrigationResources) {
			String stateValue = resource.getState() ? "ON" : "OFF";
			System.out.println(i + "\t" + resource.getAddress() + " " + resource.getPath()
			+ "\n\t\tState: " + stateValue + "\n");
		}
	}

	public static void changeIrrigationState(String state, Boolean value) {
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		if (irrigationResources == null || irrigationResources.size()==0) {
			System.out.println("Irrigation resources not registered yet!");
			return;
		}
		IrrigationResource resource = irrigationResources.get(id);
		if (resource == null) {
			System.out.println("Irrigation resources not found");
			return;
		}
		
		CoapClient client = new CoapClient(resource.getResourceURI());
		CoapResponse response = client.put("state=" + state, MediaTypeRegistry.TEXT_PLAIN);
		if (!response.isSuccess()) {
			System.out.println("Error: " + response.getCode().toString());
			return;
		}
		resource.setState(value);
		System.out.println("Irrigation state changed to: " + state);
	}
	
	public static void updateHumidity() {
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert min humidity value: ");
		int minValue = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert max humidity value: ");
		int maxValue = (int) insertInputLine();
		System.out.println();
		if (humidityResources == null || humidityResources.size()==0) {
			System.out.println("Humidity sensor not registered yet!");
			return;
		}
		HumidityResource resource = humidityResources.get(id);
		if (resource == null) {
			System.out.println("Humidity resources not found");
			return;
		}
		
		CoapClient client = new CoapClient(resource.getResourceURI());
		CoapResponse response = client.put("min_humidity=" + minValue+"&max_humidity="+maxValue, MediaTypeRegistry.TEXT_PLAIN);
		if (!response.isSuccess()) {
			System.out.println("Error: " + response.getCode().toString());
			return;
		}
		
		System.out.println("Humidity Values updated");
	}
	
	public static void updateTemperature() {
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert min temperature value: ");
		int minValue = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert max temperature value: ");
		int maxValue = (int) insertInputLine();
		System.out.println();
		if (temperatureResources == null || temperatureResources.size()==0) {
			System.out.println("Temperature sensor not registered yet!");
			return;
		}
		TemperatureResource resource = temperatureResources.get(id);
		if (resource == null) {
			System.out.println("Temperature resource not found");
			return;
		}
		
		CoapClient client = new CoapClient(resource.getResourceURI());
		CoapResponse response = client.put("min_temperature=" + minValue+"&max_temperature="+maxValue, MediaTypeRegistry.TEXT_PLAIN);
		if (!response.isSuccess()) {
			System.out.println("Error: " + response.getCode().toString());
			return;
		}
		
		System.out.println("Temperature Values updated");
	}
	
	public static void changeLampState(String state, Boolean value) {
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		if (lampResources == null || lampResources.size()==0) {
			System.out.println("Lamp resources not registered yet!");
			return;
		}
		LampResource resource = lampResources.get(id);
		if (resource == null) {
			System.out.println("Lamp resources not found");
			return;
		}
		
		CoapClient client = new CoapClient(resource.getResourceURI());
		CoapResponse response = client.put("state=" + state, MediaTypeRegistry.TEXT_PLAIN);
		if (!response.isSuccess()) {
			System.out.println("Error: " + response.getCode().toString());
			return;
		}
		resource.setState(value);
		System.out.println("Lamp state changed to: " + state);
	}

	public static void showLastSensorValue() {
		System.out.print("Insert node type (1 humidity, 2 temperature): ");
		int type = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		
		if (type == 1
		&& !humidityResources.isEmpty()
		&&	humidityResources.get(id) != null) {
			HumidityResource resource = humidityResources.get(id);
			System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath());
			System.out.println("\t\tValue: " + resource.getHumidityValues().get(resource.getHumidityValues().size()-1));
		}else if (type == 2
				&& !temperatureResources.isEmpty()
				&&	temperatureResources.get(id) != null) {
					TemperatureResource resource = temperatureResources.get(id);
					System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath());
					System.out.println("\t\tValue: " + resource.getTemperatureValues().get(resource.getTemperatureValues().size()-1));
		}else {
			System.out.println("Resource "+id+" with type "+type+" not found!");
		}
	}

	public static void showSingleResourceInformation() {
		System.out.print("Insert node type (1 humidity, 2 temperature, 3 irrigation, 4 lamp): ");
		int type = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		
		if (type == 1
		&& !humidityResources.isEmpty()
		&&	humidityResources.get(id) != null) {
			HumidityResource resource = humidityResources.get(id);
			System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath());
			for (String value : resource.getHumidityValues())
				System.out.println("\t\tValue: " + value);
		}else if (type == 2
				&& !temperatureResources.isEmpty()
				&&	temperatureResources.get(id) != null) {
					TemperatureResource resource = temperatureResources.get(id);
					System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath());
					for (String value : resource.getTemperatureValues())
						System.out.println("\t\tValue: " + value);
		}else if (type == 3
				&& !irrigationResources.isEmpty()
				&&	irrigationResources.get(id) != null) {
						IrrigationResource resource = irrigationResources.get(id);
						String stateValue = resource.getState() ? "ON" : "OFF";
						System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath()
						+ "\n\t\tState: " + stateValue + "\n");
		}else if (type == 4
				&& !lampResources.isEmpty()
				&&	lampResources.get(id) != null) {
						LampResource resource = lampResources.get(id);
						String stateValue = resource.getState() ? "ON" : "OFF";
						System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath()
						+ "\n\t\tState: " + stateValue + "\n");
		}else {
			System.out.println("Resource "+id+" with type "+type+" not found!");
		}
	}
	
	public static void addToZone() {
		System.out.print("Insert zone name: ");
		String zoneName = readInputLine();
		System.out.println();
		System.out.print("Insert node type (1 humidity, 2 temperature, 3 irrigation): ");
		int type = (int) insertInputLine();
		System.out.println();
		System.out.print("Insert node id: ");
		int id = (int) insertInputLine();
		System.out.println();
		if (!zones.containsKey(zoneName))
			zones.put(zoneName, new Zone(zoneName));
		if (type == 1
		&& !humidityResources.isEmpty()
		&&	humidityResources.get(id) != null) {
			String nodeAddress = humidityResources.get(id).getResourceURI();
			boolean added = updateZoneToNode(nodeAddress, zoneName);
			if (added) {
				LinkedHashMap<String,Integer> resources = zones.get(zoneName).getHumiditySensors();
				resources.put(nodeAddress,new Integer(id));
				zones.get(zoneName).setHumiditySensors(resources);
			}
		}else if (type == 2
		&& !temperatureResources.isEmpty()
		&&	temperatureResources.get(id) != null) {
			String nodeAddress = temperatureResources.get(id).getResourceURI();
			boolean added = updateZoneToNode(nodeAddress, zoneName);
			if (added){
				LinkedHashMap<String,Integer> resources = zones.get(zoneName).getTemperatureSensors();
				resources.put(nodeAddress,new Integer(id));
				zones.get(zoneName).setTemperatureSensors(resources);
			}
		}else if (type == 3
		&& !irrigationResources.isEmpty()
		&&	irrigationResources.get(id) != null) {
			String nodeAddress = irrigationResources.get(id).getResourceURI();
			boolean added = updateZoneToNode(nodeAddress, zoneName);
			if (added) {
				LinkedHashMap<String,Integer> resources = new LinkedHashMap<String, Integer>();
				resources.put(nodeAddress,new Integer(id));
				zones.get(zoneName).setActuators(resources);
			}
		}else {
			System.out.println("Resource "+id+" with type "+type+" not found!");
		}
	}
	
	public static void removeFromZone() {
		System.out.print("Insert zone name: ");
		String zoneName = readInputLine();
		System.out.println();
		if (zones.containsKey(zoneName)) {		
			System.out.print("Insert node type (1 humidity, 2 temperature, 3 irrigation): ");
			int type = (int) insertInputLine();
			System.out.println();
			System.out.print("Insert node id: ");
			int id = (int) insertInputLine();
			System.out.println();
			
			if (type == 1
			&& !humidityResources.isEmpty()
			&&	humidityResources.get(id) != null) {
				String nodeAddress = humidityResources.get(id).getResourceURI();
				boolean deleted = updateZoneToNode(nodeAddress, "None");
				if (deleted)
					zones.get(zoneName).getHumiditySensors().remove(nodeAddress);
			}else if (type == 2
			&& !temperatureResources.isEmpty()
			&&	temperatureResources.get(id) != null) {
				String nodeAddress = temperatureResources.get(id).getResourceURI();
				boolean deleted = updateZoneToNode(nodeAddress, "None");
				if (deleted)
					zones.get(zoneName).getTemperatureSensors().remove(nodeAddress);
			}else if (type == 3
			&& !irrigationResources.isEmpty()
			&&	irrigationResources.get(id) != null) {
				String nodeAddress = irrigationResources.get(id).getResourceURI();
				boolean deleted = updateZoneToNode(nodeAddress, "None");
				if (deleted)
					zones.get(zoneName).getActuators().remove(nodeAddress);
				boolean removed = false;
				for (String node : zones.get(zoneName).getHumiditySensors().keySet()) {
					try {
						Thread.sleep(15000);
						removed = removeActuatorsFromNode(node);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.out.println("Error in actuators removing");
					}
				}
			}else {
				System.out.println("Resource "+id+" with type "+type+" not found");
			}
		} else {
			System.out.println("Zone not found!");
		}
	}
	
	/*
	public static void updateZoneName() {
		System.out.print("Insert zone name: ");
		String zoneName = readInputLine();
		System.out.println();
		if (zones.containsKey(zoneName)) {
			System.out.print("Insert new name: ");
			String newZoneName = readInputLine();
			System.out.println();
			if (zones.containsKey(newZoneName))
				System.out.println("Zone name already exist, please change it!");
			else {
				Zone newZone = new Zone(newZoneName);
				newZone.getSensors().addAll(zones.get(zoneName).getSensors());
				newZone.getActuators().addAll(zones.get(zoneName).getActuators());
				zones.remove(zoneName);
				zones.put(newZoneName, newZone);
				System.out.println("Name updated!");
			}
		}
		else
			System.out.println("Zone not found!");
	}
	*/
	
	public static void deleteZone() {
		System.out.print("Insert zone name: ");
		String zoneName = readInputLine();
		System.out.println();
		boolean deleted = false;
		if (zones.containsKey(zoneName)) {
			Zone zone = MainApp.zones.get(zoneName);
			
			if (zone == null || zone.getActuators().size() == 0) {
				System.out.println("Zone not found!");
				return;
			}
		
			ArrayList<String> motes = new ArrayList<String>();
			motes.addAll(zones.get(zoneName).getHumiditySensors().keySet());
			motes.addAll(zones.get(zoneName).getTemperatureSensors().keySet());
			motes.addAll(zones.get(zoneName).getActuators().keySet());
			
			for(String nodeAddress : motes) {
				try {
					Thread.sleep(15000);
					deleted = updateZoneToNode(nodeAddress, "None");
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.out.println("Error in zone removing");
				} 
			}
			if(deleted)
				zones.remove(zoneName);
		}
		else
			System.out.println("Zone not found!");
	}

	private static boolean removeActuatorsFromNode(String nodeAddress) {
		CoapClient client = new CoapClient(nodeAddress);
		CoapResponse response = client.put("actuators=None", MediaTypeRegistry.TEXT_PLAIN);		
		
		if (!response.isSuccess()) {
			System.out.println("Error removing actuators list!");
			return false;
		}
		else {
			System.out.println("Actuators list removed");
			return true;
		}
	}
	
	private static boolean updateZoneToNode(String nodeAddress, String zoneName) {
		CoapClient client = new CoapClient(nodeAddress);
		CoapResponse response = client.put("zone="+zoneName, MediaTypeRegistry.TEXT_PLAIN);		
		
		if (!response.isSuccess()) {
			System.out.println("Error updating zone!");
			return false;
		}
		else {
			System.out.println("Zone updated");
			return true;
		}
	}
	
	public static void showZone() {
		System.out.print("Insert zone name: ");
		String zoneName = readInputLine();
		System.out.println();
		if (zones.containsKey(zoneName)) {
			System.out.println("Sensors: ");
			for (String uri : zones.get(zoneName).getHumiditySensors().keySet()) {
				System.out.println(uri);
			}
			for (String uri : zones.get(zoneName).getTemperatureSensors().keySet()) {
				System.out.println(uri);
			}
			System.out.println("Actuators: ");
			for (String uri : zones.get(zoneName).getActuators().keySet()) {
				System.out.println(uri);
			}
		}
		else
			System.out.println("Zone not found!");
	}
	
	public static void showZoneInformations() {
		System.out.print("Insert zone name: ");
		String zoneName = readInputLine();
		System.out.println();
		if (zones.containsKey(zoneName)) {
			Zone zone = zones.get(zoneName);
			System.out.println("Data for zone "+ zoneName);
			
			for (Integer id : zone.getHumiditySensors().values()) {
				if (!humidityResources.isEmpty()
				&&	humidityResources.get(id) != null) {
					HumidityResource resource = humidityResources.get(id);
					System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath());
					for (String value : resource.getHumidityValues())
						System.out.println("\t\tValue: " + value);
				}
			}
			for (Integer id : zone.getHumiditySensors().values()) {
				if (!temperatureResources.isEmpty()
				&&	temperatureResources.get(id) != null) {
					TemperatureResource resource = temperatureResources.get(id);
					System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath());
					for (String value : resource.getTemperatureValues())
						System.out.println("\t\tValue: " + value);
				}
			}
			for (Integer id : zone.getActuators().values()) {
				if (!irrigationResources.isEmpty()
				&&	irrigationResources.get(id) != null)  {
					IrrigationResource resource = irrigationResources.get(id);
					String stateValue = resource.getState() ? "ON" : "OFF";
					System.out.println(id + "\t" + resource.getAddress() + " " + resource.getPath()
					+ "\n\t\tState: " + stateValue + "\n");
				}
			}
		}
		else
			System.out.println("Zone not found!");
	}
	
	public static void showZoneList() {
		int i=0;
		for(String zoneName: zones.keySet()) {
			System.out.println("Zone "+ i + ":\t "+zoneName);
			i++;
		}
	}

	public static void showOperations() {
		System.out.println("Commands List:");
		System.out.println("0: show resources");
		System.out.println("1: show all resources information");
		System.out.println("2: show single resource");
		System.out.println("3: show last value registered from sensor");
		System.out.println("4: set min&max humidity");
		System.out.println("5: set min&max temperature");
		System.out.println("6: start irrigation");
		System.out.println("7: stop irrigation");
		System.out.println("8: start lamp");
		System.out.println("9: stop lamp");
		System.out.println("10: add to zone");
		System.out.println("11: remove from zone");
		System.out.println("12: show single zone");
		System.out.println("13: show zone informations");
		System.out.println("14: show zone list");
		System.out.println("15: delete zone");
		System.out.println("16: exit");
	}
}
