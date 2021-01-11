package it.unipi.iot;

public class LampResource extends Resource {
	private Boolean state;

	public LampResource(String path, String address) {
		super(path, address);
		state = false;
	}

	public Boolean getState() {
		return state;
	}

	public void setState(Boolean state) {
		this.state = state;
	}
}
