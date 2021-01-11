package it.unipi.iot;

import java.util.ArrayList;

public class TemperatureResource extends Resource {
	private ArrayList<String> temperatureValues = new ArrayList<String>();

	public TemperatureResource(String path, String address) {
		super(path, address);
	}

	public ArrayList<String> getTemperatureValues() {
		return this.temperatureValues;
	}

	public void setTemperatureValues(ArrayList<String> list) {
		int valuesLimit = 5;
		if (list.size() > valuesLimit)
			list.remove(0);
		this.temperatureValues = list;
	}
}
