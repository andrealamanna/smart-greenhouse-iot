package it.unipi.iot;

import java.util.LinkedHashMap;

public class Zone{
	
	private String name;
	private LinkedHashMap<String,Integer> humiditySensors = new LinkedHashMap<String,Integer>();
	private LinkedHashMap<String,Integer> temperatureSensors = new LinkedHashMap<String,Integer>();
	private LinkedHashMap<String,Integer> actuators = new LinkedHashMap<String,Integer>();
	
	public Zone(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public LinkedHashMap<String,Integer> getHumiditySensors() {
		return humiditySensors;
	}

	public void setHumiditySensors(LinkedHashMap<String,Integer> humiditySensors) {
		this.humiditySensors = humiditySensors;
	}
	
	public LinkedHashMap<String,Integer> getTemperatureSensors() {
		return temperatureSensors;
	}

	public void setTemperatureSensors(LinkedHashMap<String,Integer> tempatureSensors) {
		this.temperatureSensors = tempatureSensors;
	}
	
	public LinkedHashMap<String,Integer> getActuators() {
		return actuators;
	}

	public void setActuators(LinkedHashMap<String,Integer> actuators) {
		this.actuators = actuators;
	}
}
