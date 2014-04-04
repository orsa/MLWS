package tau.tac.adx.agents;

import java.util.Set;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.report.adn.MarketSegment;

public class ImpressionParameters {

	private Set<MarketSegment> marketSegments;
	private Device device;
	private AdType adType;
	
	
	
	public Set<MarketSegment> getMarketSegments() {
		return marketSegments;
	}
	
	public void setMarketSegments(Set<MarketSegment> marketSegments) {
		this.marketSegments = marketSegments;
	}
	
	public Device getDevice() {
		return device;
	}
	
	public void setDevice(Device device) {
		this.device = device;
	}
	
	public AdType getAdType() {
		return adType;
	}
	
	public void setAdType(AdType adType) {
		this.adType = adType;
	}
	
}
