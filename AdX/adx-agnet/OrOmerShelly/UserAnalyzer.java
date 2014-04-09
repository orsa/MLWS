package OrOmerShelly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.report.adn.MarketSegment;

public class UserAnalyzer {

	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats;
	private double ucsLevel;
	
	private MarketSegment predictedAge;
	private MarketSegment prdictedGender;
	private MarketSegment predictedIncome;
		
	public List<Set<MarketSegment>> predictMarketSegment(String publisherName) {
		predictAge(publisherName);
		predictGender(publisherName);
		predictIncome(publisherName);
		
		List<Set<MarketSegment>> predictedMarketSegments = new ArrayList<Set<MarketSegment>>();
		predictedMarketSegments.add(MarketSegment.compundMarketSegment(predictedAge, prdictedGender));
		predictedMarketSegments.add(MarketSegment.compundMarketSegment(predictedAge, predictedIncome));
		predictedMarketSegments.add(MarketSegment.compundMarketSegment(prdictedGender, predictedIncome));
		
		return predictedMarketSegments;		
	}

	private void predictIncome(String publisherName) {
		// TODO Auto-generated method stub
		
	}

	private void predictGender(String publisherName) {
		// TODO Auto-generated method stub
		
	}

	private void predictAge(String publisherName) {
		// TODO Auto-generated method stub
		
	}

	public List<Pair<ImpressionParameters, Double>> calcMarketSegmentsDistribution(String publisherName) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		
		
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	// plan - we will rank all market segments - from the most to less probable, and use this to
	// prioritize and to get our bids.
	// to do that, we will predict market segments for all publishers.
}
