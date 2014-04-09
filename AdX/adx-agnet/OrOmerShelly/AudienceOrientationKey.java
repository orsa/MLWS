package OrOmerShelly;

import tau.tac.adx.report.adn.MarketSegment;

public class AudienceOrientationKey {
	
	private String publisherName;
	private MarketSegment marketSegment;
	
	public AudienceOrientationKey(String publisherName,
			MarketSegment marketSegment) {
		super();
		this.publisherName = publisherName;
		this.marketSegment = marketSegment;
	}
	
	public String getPublisherName() {
		return publisherName;
	}
	
	public void setPublisherName(String publisherName) {
		this.publisherName = publisherName;
	}
	
	public MarketSegment getMarketSegment() {
		return marketSegment;
	}
	
	public void setMarketSegment(MarketSegment marketSegment) {
		this.marketSegment = marketSegment;
	}
}