package OrOmerShelly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.analysis.NewtonSolver;

import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.report.adn.MarketSegment;

public class UserAnalyzer {

	private final static int NUMBER_OF_MARKET_SEGMENTS = 12;
	private final static int USER_POPULATION_SIZE = 10000;
	private Map<String, PublisherStats> publishersStats;
		
	// Private static member - Map publisher name + audience orientation to probabilities
	private static HashMap<AudienceOrientationKey, Double> audienceOrientationMap = getAudienceOrientation();
	private static HashMap<DeviceOrientationKey, Double> deviceOrientationMap = getDeviceOrientation();
	private static HashMap<String, Double> popularityMap = getPopularity();
	
	
	private static HashMap<DeviceOrientationKey, Double> getDeviceOrientation() {
		HashMap<DeviceOrientationKey, Double> map = new HashMap<DeviceOrientationKey, Double>();
		
		map.put(new DeviceOrientationKey("Yahoo", Device.mobile), 26.0);
		map.put(new DeviceOrientationKey("Yahoo", Device.pc), 100 - 26.0);
		
		map.put(new DeviceOrientationKey("CNN", Device.mobile), 24.0);
		map.put(new DeviceOrientationKey("CNN", Device.pc), 100 - 24.0);
		
		map.put(new DeviceOrientationKey("NY Times", Device.mobile), 23.0);
		map.put(new DeviceOrientationKey("NY Times", Device.pc), 100 - 23.0);
		
		map.put(new DeviceOrientationKey("Hfngtn", Device.mobile), 22.0);
		map.put(new DeviceOrientationKey("Hfngtn", Device.pc), 100 - 22.0);
		
		map.put(new DeviceOrientationKey("MSN", Device.mobile), 25.0);
		map.put(new DeviceOrientationKey("MSN", Device.pc), 100 - 25.0);
		
		map.put(new DeviceOrientationKey("Fox", Device.mobile), 24.0);
		map.put(new DeviceOrientationKey("Fox", Device.pc), 100 - 24.0);
		
		map.put(new DeviceOrientationKey("Amazon", Device.mobile), 21.0);
		map.put(new DeviceOrientationKey("Amazon", Device.pc), 100 - 21.0);
		
		map.put(new DeviceOrientationKey("Ebay", Device.mobile), 22.0);
		map.put(new DeviceOrientationKey("Ebay", Device.pc), 100 - 22.0);
		
		map.put(new DeviceOrientationKey("Wal-Mart", Device.mobile), 18.0);
		map.put(new DeviceOrientationKey("Wal-Mart", Device.pc), 100 - 18.0);
		
		map.put(new DeviceOrientationKey("Target", Device.mobile), 19.0);
		map.put(new DeviceOrientationKey("Target", Device.pc), 100 - 19.0);
		
		map.put(new DeviceOrientationKey("BestBuy", Device.mobile), 20.0);
		map.put(new DeviceOrientationKey("BestBuy", Device.pc), 100 - 20.0);
		
		map.put(new DeviceOrientationKey("Sears", Device.mobile), 19.0);
		map.put(new DeviceOrientationKey("Sears", Device.pc), 100 - 19.0);
		
		map.put(new DeviceOrientationKey("WebMD", Device.mobile), 24.0);
		map.put(new DeviceOrientationKey("WebMD", Device.pc), 100 - 24.0);
		
		map.put(new DeviceOrientationKey("EHow", Device.mobile), 28.0);
		map.put(new DeviceOrientationKey("EHow", Device.pc), 100 - 28.0);
		
		map.put(new DeviceOrientationKey("Ask", Device.mobile), 28.0);
		map.put(new DeviceOrientationKey("Ask", Device.pc), 100 - 28.0);
		
		map.put(new DeviceOrientationKey("TripAdvisor", Device.mobile), 30.0);
		map.put(new DeviceOrientationKey("TripAdvisor", Device.pc), 100 - 30.0);
		
		map.put(new DeviceOrientationKey("CNet", Device.mobile), 27.0);
		map.put(new DeviceOrientationKey("CNet", Device.pc), 100 - 27.0);
		
		map.put(new DeviceOrientationKey("Weather", Device.mobile), 31.0);
		map.put(new DeviceOrientationKey("Weather", Device.pc), 100 - 31.0);
				
		return map;
	}

	private static HashMap<String, Double> getPopularity() {
		HashMap<String, Double> map = new HashMap<String, Double>();
		map.put("Yahoo", 16.0);
		map.put("CNN", 2.2);
		map.put("NY Times", 3.1);
		map.put("Hfngtn", 8.1);
		map.put("MSN", 18.2);
		map.put("Fox", 3.1);
		map.put("Amazon", 12.8);
		map.put("Ebay", 8.5);
		map.put("Wal-Mart", 3.8);
		map.put("Target", 2.0);
		map.put("BestBuy", 1.6);
		map.put("Sears", 1.6);
		map.put("WebMD", 2.5);
		map.put("EHow", 2.5);
		map.put("Ask", 5.0);
		map.put("TripAdvisor", 1.6);
		map.put("CNet", 1.7);
		map.put("Weather", 5.8);
		return map;
	}

	private static HashMap<AudienceOrientationKey, Double> getAudienceOrientation() {
		List<AudienceOrientationRecord> orientationsCollection = new LinkedList<AudienceOrientationRecord>();
		// Assuming no below age 18 users, as they do not appear in User Popoulation Probabilities table. Therefore remaining part is given to "OLD" market segment.
		
		// Note: Calculations are "not pretty" to allow easy verification with the table.
		
		// Yahoo
		orientationsCollection.add(new AudienceOrientationRecord("Yahoo", MarketSegment.FEMALE, 50.4));
		orientationsCollection.add(new AudienceOrientationRecord("Yahoo", MarketSegment.MALE, 49.6));
		orientationsCollection.add(new AudienceOrientationRecord("Yahoo", MarketSegment.YOUNG, 12.2 + 17.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Yahoo", MarketSegment.OLD, 18.4 + 16.4 + (100 - (12.2 + 17.1 + 16.7) - (18.4 + 16.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Yahoo", MarketSegment.LOW_INCOME, 53 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("Yahoo", MarketSegment.HIGH_INCOME, 13 + (100 - 53 - 27 - 13)));
		
		// CNN
		orientationsCollection.add(new AudienceOrientationRecord("CNN", MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord("CNN", MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord("CNN", MarketSegment.YOUNG, 10.2 + 16.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("CNN", MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 16.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("CNN", MarketSegment.LOW_INCOME, 48 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("CNN", MarketSegment.HIGH_INCOME, 16 + (100 - 48 - 27 - 16)));		
		
		// NY Times
		orientationsCollection.add(new AudienceOrientationRecord("NY Times", MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord("NY Times", MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord("NY Times", MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("NY Times", MarketSegment.OLD, 19.4 + 17.4 + (100 - (9.2 + 15.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("NY Times", MarketSegment.LOW_INCOME, 47 + 26));
		orientationsCollection.add(new AudienceOrientationRecord("NY Times", MarketSegment.HIGH_INCOME, 17 + (100 - 47 - 26 - 17)));		
		
		// Hfngtn
		orientationsCollection.add(new AudienceOrientationRecord("Hfngtn", MarketSegment.FEMALE, 53.4));
		orientationsCollection.add(new AudienceOrientationRecord("Hfngtn", MarketSegment.MALE, 46.6));
		orientationsCollection.add(new AudienceOrientationRecord("Hfngtn", MarketSegment.YOUNG, 10.2 + 16.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Hfngtn", MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 16.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Hfngtn", MarketSegment.LOW_INCOME, 47 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("Hfngtn", MarketSegment.HIGH_INCOME, 17 + (100 - 47 - 27 - 17)));		
		
		
		// MSN
		orientationsCollection.add(new AudienceOrientationRecord("MSN", MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord("MSN", MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord("MSN", MarketSegment.YOUNG, 10.2 + 16.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("MSN", MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 16.1 + 16.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("MSN", MarketSegment.LOW_INCOME, 49 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("MSN", MarketSegment.HIGH_INCOME, 16 + (100 - 49 - 27 - 16)));		
		
		// Fox
		orientationsCollection.add(new AudienceOrientationRecord("Fox", MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord("Fox", MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord("Fox", MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Fox", MarketSegment.OLD, 19.4 + 18.4 + (100 - (9.2 + 15.1 + 16.7) - (19.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Fox", MarketSegment.LOW_INCOME, 46 + 26));
		orientationsCollection.add(new AudienceOrientationRecord("Fox", MarketSegment.HIGH_INCOME, 18 + (100 - 46 - 26 - 18)));		
		
		// Amazon
		orientationsCollection.add(new AudienceOrientationRecord("Amazon", MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord("Amazon", MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord("Amazon", MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Amazon", MarketSegment.OLD, 19.4 + 18.4 + (100 - (9.2 + 15.1 + 16.7) - (19.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Amazon", MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("Amazon", MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Ebay
		orientationsCollection.add(new AudienceOrientationRecord("Ebay", MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord("Ebay", MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord("Ebay", MarketSegment.YOUNG, 9.2 + 16.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord("Ebay", MarketSegment.OLD, 19.4 + 17.4 + (100 - (9.2 + 16.1 + 15.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Ebay", MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("Ebay", MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Wal-Mart
		orientationsCollection.add(new AudienceOrientationRecord("Wal-Mart", MarketSegment.FEMALE, 54.4));
		orientationsCollection.add(new AudienceOrientationRecord("Wal-Mart", MarketSegment.MALE, 45.6));
		orientationsCollection.add(new AudienceOrientationRecord("Wal-Mart", MarketSegment.YOUNG, 7.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Wal-Mart", MarketSegment.OLD, 20.4 + 18.4 + (100 - (7.2 + 15.1 + 16.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Wal-Mart", MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("Wal-Mart", MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Target
		orientationsCollection.add(new AudienceOrientationRecord("Target", MarketSegment.FEMALE, 54.4));
		orientationsCollection.add(new AudienceOrientationRecord("Target", MarketSegment.MALE, 45.6));
		orientationsCollection.add(new AudienceOrientationRecord("Target", MarketSegment.YOUNG, 9.2 + 17.1 + 17.7));
		orientationsCollection.add(new AudienceOrientationRecord("Target", MarketSegment.OLD, 18.4 + 17.4 + (100 - (9.2 + 17.1 + 17.7) - (18.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Target", MarketSegment.LOW_INCOME, 45 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("Target", MarketSegment.HIGH_INCOME, 19 + (100 - 45 - 27 - 19)));		
		
		// BestBuy
		orientationsCollection.add(new AudienceOrientationRecord("BestBuy", MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord("BestBuy", MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord("BestBuy", MarketSegment.YOUNG, 10.2 + 14.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("BestBuy", MarketSegment.OLD, 20.4 + 17.4 + (100 - (10.2 + 14.1 + 16.7) - (20.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("BestBuy", MarketSegment.LOW_INCOME, 46.5 + 26));
		orientationsCollection.add(new AudienceOrientationRecord("BestBuy", MarketSegment.HIGH_INCOME, 18 + (100 - 46.5 - 26 - 18)));		
		
		// Sears
		orientationsCollection.add(new AudienceOrientationRecord("Sears", MarketSegment.FEMALE, 53.4));
		orientationsCollection.add(new AudienceOrientationRecord("Sears", MarketSegment.MALE, 46.6));
		orientationsCollection.add(new AudienceOrientationRecord("Sears", MarketSegment.YOUNG, 9.2 + 12.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Sears", MarketSegment.OLD, 20.4 + 18.4 + (100 - (9.2 + 12.1 + 16.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Sears", MarketSegment.LOW_INCOME, 45 + 25));
		orientationsCollection.add(new AudienceOrientationRecord("Sears", MarketSegment.HIGH_INCOME, 20 + (100 - 45 - 25 - 20)));		
		
		// WebMD
		orientationsCollection.add(new AudienceOrientationRecord("WebMD", MarketSegment.FEMALE, 54.4));
		orientationsCollection.add(new AudienceOrientationRecord("WebMD", MarketSegment.MALE, 45.6));
		orientationsCollection.add(new AudienceOrientationRecord("WebMD", MarketSegment.YOUNG, 9.2 + 15.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord("WebMD", MarketSegment.OLD, 19.4 + 18.4 + (100 - (9.2 + 15.1 + 15.7) - (19.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("WebMD", MarketSegment.LOW_INCOME, 46 + 26.5));
		orientationsCollection.add(new AudienceOrientationRecord("WebMD", MarketSegment.HIGH_INCOME, 18.5 + (100 - 46 - 26.5 - 18.5)));		
		
		// EHow
		orientationsCollection.add(new AudienceOrientationRecord("EHow", MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord("EHow", MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord("EHow", MarketSegment.YOUNG, 10.2 + 15.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord("EHow", MarketSegment.OLD, 19.4 + 17.4 + (100 - (10.2 + 15.1 + 15.7) - (19.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("EHow", MarketSegment.LOW_INCOME, 50 + 27));
		orientationsCollection.add(new AudienceOrientationRecord("EHow", MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 27 - 15)));		
		
		// Ask
		orientationsCollection.add(new AudienceOrientationRecord("Ask", MarketSegment.FEMALE, 51.4));
		orientationsCollection.add(new AudienceOrientationRecord("Ask", MarketSegment.MALE, 48.6));
		orientationsCollection.add(new AudienceOrientationRecord("Ask", MarketSegment.YOUNG, 10.2 + 13.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord("Ask", MarketSegment.OLD, 20.4 + 18.4 + (100 - (10.2 + 13.1 + 15.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Ask", MarketSegment.LOW_INCOME, 50 + 28));
		orientationsCollection.add(new AudienceOrientationRecord("Ask", MarketSegment.HIGH_INCOME, 15 + (100 - 50 - 28 - 15)));		
		
		// TripAdvisor
		orientationsCollection.add(new AudienceOrientationRecord("TripAdvisor", MarketSegment.FEMALE, 53.4));
		orientationsCollection.add(new AudienceOrientationRecord("TripAdvisor", MarketSegment.MALE, 46.6));
		orientationsCollection.add(new AudienceOrientationRecord("TripAdvisor", MarketSegment.YOUNG, 8.2 + 16.1 + 17.7));
		orientationsCollection.add(new AudienceOrientationRecord("TripAdvisor", MarketSegment.OLD, 20.4 + 17.4 + (100 - (8.2 + 16.1 + 17.7) - (20.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("TripAdvisor", MarketSegment.LOW_INCOME, 46.5 + 26));
		orientationsCollection.add(new AudienceOrientationRecord("TripAdvisor", MarketSegment.HIGH_INCOME, 17.5 + (100 - 46.5 - 26 - 17.5)));		
		
		// CNet
		orientationsCollection.add(new AudienceOrientationRecord("CNet", MarketSegment.FEMALE, 49.4));
		orientationsCollection.add(new AudienceOrientationRecord("CNet", MarketSegment.MALE, 50.6));
		orientationsCollection.add(new AudienceOrientationRecord("CNet", MarketSegment.YOUNG, 12.2 + 15.1 + 15.7));
		orientationsCollection.add(new AudienceOrientationRecord("CNet", MarketSegment.OLD, 18.4 + 17.4 + (100 - (12.2 + 15.1 + 15.7) - (18.4 + 17.4))));
		orientationsCollection.add(new AudienceOrientationRecord("CNet", MarketSegment.LOW_INCOME, 48 + 26.5));
		orientationsCollection.add(new AudienceOrientationRecord("CNet", MarketSegment.HIGH_INCOME, 16.5 + (100 - 48 - 26.5 - 16.5)));	
		
		// Weather
		orientationsCollection.add(new AudienceOrientationRecord("Weather", MarketSegment.FEMALE, 52.4));
		orientationsCollection.add(new AudienceOrientationRecord("Weather", MarketSegment.MALE, 47.6));
		orientationsCollection.add(new AudienceOrientationRecord("Weather", MarketSegment.YOUNG, 9.2 + 15.1 + 16.7));
		orientationsCollection.add(new AudienceOrientationRecord("Weather", MarketSegment.OLD, 20.4 + 18.4 + (100 - (9.2 + 15.1 + 16.7) - (20.4 + 18.4))));
		orientationsCollection.add(new AudienceOrientationRecord("Weather", MarketSegment.LOW_INCOME, 45.5 + 26.5));
		orientationsCollection.add(new AudienceOrientationRecord("Weather", MarketSegment.HIGH_INCOME, 18.5 + (100 - 45.5 - 26.5 - 18.5)));		
		
		HashMap<AudienceOrientationKey, Double> map = new HashMap<AudienceOrientationKey, Double>();
		for (AudienceOrientationRecord orientationRecord : orientationsCollection) {
			map.put(orientationRecord.getKey(), orientationRecord.getOrientation());
		}
		
		return map;
	}

	public List<ImpressionParamtersDistributionKey> calcImpressionDistribution(String publisherName) {
		List<ImpressionParamtersDistributionKey> weights = new ArrayList<ImpressionParamtersDistributionKey>();
		PublisherStats publisherStats = publishersStats.get(publisherName);
		
		// Ad type orientation may be unknown. Therefore, if not stats are available for the publisher, average on what you already have.
		if (publisherStats == null) {
			publisherStats = getAveragedPublisherStatsForPublisher(publisherName);
		}
		
		// Calculate for all 12 market segments and include device and ad-type
		List<Set<MarketSegment>> allMarketSegments = MarketSegment.compundMarketSegments();
		for (Set<MarketSegment> marketSegment : allMarketSegments) {
			for (Device device : Device.values()) {
				for (AdType adType : AdType.values()) {
					ImpressionParameters impParams = new ImpressionParameters(marketSegment, device, adType);
					Double weight = 1.0;
					
					for (MarketSegment partialMarketSegmnet : marketSegment) {
						weight *= audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegmnet)) / 100; 
					}
					
					weight *= deviceOrientationMap.get(new DeviceOrientationKey(publisherName, device))  / 100;
					weight *= (adType == AdType.text ? publisherStats.getTextOrientation() : publisherStats.getVideoOrientation()) / publisherStats.getPopularity();
					weight *= popularityMap.get(publisherName) / 100;
										
					ImpressionParamtersDistributionKey key = new ImpressionParamtersDistributionKey(impParams, weight);
					weights.add(key);					
				}
			}
		}
		
		return weights;
	}

	public PublisherStats getAveragedPublisherStatsForPublisher(String publisherName) {
		double sumText = 0.0;
		double sumVideo = 0.0;
		double avgText = 0.0;
		double avgVideo = 0.0;
		double popularity = USER_POPULATION_SIZE * popularityMap.get(publisherName) / 100;
		
		int count = publishersStats.values().size();
		
		for (PublisherStats publisherStats : publishersStats.values()) {
			long publisherPopularity = publisherStats.getPopularity();
			sumText += (double)publisherStats.getTextOrientation() / publisherPopularity;
			sumVideo += (double)publisherStats.getVideoOrientation() / publisherPopularity;
		}
		
		avgText = sumText / count;
		avgVideo = sumVideo / count;
				
		return new PublisherStats(Math.round(popularity), Math.round(avgVideo * popularity), Math.round(avgText * popularity));
	}

	public double calcMedianOfMarketSegmentsWeights(String publisherName) {
		List<Double> weights = new ArrayList<Double>(NUMBER_OF_MARKET_SEGMENTS);
		
		List<Set<MarketSegment>> allMarketSegments = MarketSegment.compundMarketSegments();
		for (Set<MarketSegment> marketSegment : allMarketSegments) {
			double weight = 1.0;
			
			for (MarketSegment partialMarketSegmnet : marketSegment) {
				weight *= audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegmnet)) / 100; 
			}
			
			weights.add(weight);
		}
		
		return weights.get(NUMBER_OF_MARKET_SEGMENTS/2 - 1);		
	}

	public double getMarketSegmentWeight(Set<MarketSegment> targetSegment, String publisherName) {
		double weight = 1.0;
		
		for (MarketSegment partialMarketSegmnet : targetSegment) {
			weight *= audienceOrientationMap.get(new AudienceOrientationKey(publisherName, partialMarketSegmnet)) / 100; 
		}
		
		return weight;
	}
	
}
