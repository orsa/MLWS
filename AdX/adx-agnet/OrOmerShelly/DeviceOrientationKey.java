package OrOmerShelly;

import tau.tac.adx.devices.Device;

public class DeviceOrientationKey {
	
	private String publisherName;
	private Device device;
	
	public DeviceOrientationKey(String publisherName,
			Device device) {
		this.publisherName = publisherName;
		this.device = device;
	}
	
	public String getPublisherName() {
		return publisherName;
	}
	
	public void setPublisherName(String publisherName) {
		this.publisherName = publisherName;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}
	

}