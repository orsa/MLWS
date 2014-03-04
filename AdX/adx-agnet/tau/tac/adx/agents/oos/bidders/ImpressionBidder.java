package tau.tac.adx.agents.oos.bidders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

import org.apache.commons.lang3.tuple.Pair;

import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.agents.oos.CampaignData;
import tau.tac.adx.agents.oos.ImpressionParameters;
import tau.tac.adx.agents.oos.PublisherStats;
import tau.tac.adx.agents.oos.UserAnalyzer;
import tau.tac.adx.devices.Device;
import edu.umich.eecs.tac.props.Ad;

public class ImpressionBidder {

	private static ImpressionBidder instance = null;

	private final Logger log = Logger
			.getLogger(ImpressionBidder.class.getName());


	private UserAnalyzer userAnalyzer;
	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats;
	private List<CampaignData> myActiveCampaigns;
	@SuppressWarnings("unused")
	private double bankBalance;
	
	private AdxBidBundle bidBundle;
	
	private Instance lastInstance;
	private Dataset dataset;
	private Classifier classifier;
	
	// TODO: set daily limits for campaign and overall - do not want to get a minus in bankBalance?
	public void fillBidBundle(int day) {

		bidBundle = new AdxBidBundle();
		
		for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog.getPublishers()) {
			String publisherName = publisherCatalogEntry.getPublisherName();
			List<Pair<ImpressionParameters,Double>> marketSegmentsDistribution = userAnalyzer.calcMarketSegmentsDistribution(publisherName);

			Collections.sort(marketSegmentsDistribution, 
					new Comparator<Pair<ImpressionParameters,Double>>() {
				@Override
				public int compare(Pair<ImpressionParameters, Double> o1,
						Pair<ImpressionParameters, Double> o2) {
					return o1.getRight().compareTo(o2.getRight());
				}
			});


			Map<Integer,Integer> campaignWeightVector = new HashMap<Integer,Integer>();
			double bid = 0.0;
			for (Pair<ImpressionParameters, Double> marketSegmentWithWeight : marketSegmentsDistribution) {
				ImpressionParameters impParams = marketSegmentWithWeight.getLeft();
				
				// Initial bid calculates what does the impression worth
				bid = initialBid(impParams, publisherName, marketSegmentWithWeight.getRight());
				
				// Now we will calculate what does the impression worth *for us*.
				List<CampaignData> relevantCampaigns = filterCampaigns(impParams);
				if (exists(relevantCampaigns)) {
					prioritizeCampaigns(relevantCampaigns);
					bid = calcBid();
				} else {
					if (isUnknown(impParams)) {
						getUrgentCampaigns();
						bid = calcBidForUnknown();
					}
				}

				addToBidBundle(publisherName, impParams, bid, campaignWeightVector);
			}

		}
	}

	private double initialBid(ImpressionParameters impParams, String publisherName, Double weight) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		int adTypeOrientation = getAdTypeOrientation(impParams, publisherStats);
		double[] serializedValues = serializeValues(publisherStats.getPopularity(),
										impParams.getAdType(),
										adTypeOrientation,
										weight,
										impParams.getDevice());
		Instance testInstance = new DenseInstance(serializedValues); 
		lastInstance = testInstance;
		
		return (double)classifier.classify(testInstance);

	}

	private double[] serializeValues(int popularity, AdType adType,
			int adTypeOrientation, Double weight,
			Device device) {
		double[] values = new double[5];
		values[0] = (double)popularity;
		values[1] = (double)adType.ordinal();
		values[2] = (double)adTypeOrientation;
		values[3] = weight != null ? (double)weight : 0;
		values[3] = (double)device.ordinal();
		return values;
	}

	private int getAdTypeOrientation(ImpressionParameters impParams,
			PublisherStats publisherStats) {
		switch(impParams.getAdType()) {
			case text: return publisherStats.getTextOrientation();
			case video: return publisherStats.getVideoOrientation();
			default: log.warning("No ad type provided for impression"); return 0;
		}
	}

	private void addToBidBundle(String publisherName, ImpressionParameters impParams, double bid, Map<Integer, Integer> campaignWeightVector) {
		for (Integer campaignId : campaignWeightVector.keySet()) {
			for (MarketSegment marketSegment : impParams.getMarketSegments()) {
				AdxQuery query = new AdxQuery(publisherName, marketSegment, impParams.getDevice(), impParams.getAdType());
				bidBundle.addQuery(query, bid, new Ad(null), campaignId, campaignWeightVector.get(campaignId));
			}
		}
		
	}

	private double calcBidForUnknown() {
		return 0;
		// TODO Auto-generated method stub
		
	}

	private void getUrgentCampaigns() {
		// TODO Auto-generated method stub
		
	}

	private boolean isUnknown(ImpressionParameters impParams) {
		return impParams.getMarketSegments().isEmpty();
	}

	private double calcBid() {
		return 0;
		// TODO Auto-generated method stub
		
	}

	private void prioritizeCampaigns(List<CampaignData> relevantCampaigns) {
		// TODO Auto-generated method stub
		
	}

	private List<CampaignData> filterCampaigns(ImpressionParameters impParams) {
		List<CampaignData> filteredCampaigns = new ArrayList<CampaignData>();
		AdType impressionAdType = impParams.getAdType();
		Device impressionDevice =  impParams.getDevice();
		Set<MarketSegment> impressionMarketSegments = impParams.getMarketSegments();
		
		for (CampaignData campaign : myActiveCampaigns) {
			boolean addedCampaign = false;
			Set<MarketSegment> marketSegments = campaign.getTargetSegment();
			AdxQuery[] relevantQueries = campaign.getCampaignQueries();
			for (AdxQuery query : relevantQueries) {
				AdType relevantAd = query.getAdType();
				Device relevantDevice = query.getDevice();
				
				if (relevantAd == impressionAdType && relevantDevice == impressionDevice) {
					for (MarketSegment impressionMarketSegment : impressionMarketSegments) {
						if (marketSegments.contains(impressionMarketSegment)) {
							filteredCampaigns.add(campaign);
							addedCampaign = true;
							break;
						}
					}
				}
				
				if (addedCampaign) {
					break;
				}
			}
		}
		
		return filteredCampaigns;
	}

	private boolean exists(List<CampaignData> campaigns) {
		if (campaigns == null || campaigns.size() == 0) {
			return false;
		}

		return true;	
	}
	
	
	
	
	public void trainClassifier() {
		dataset.add(lastInstance);
		classifier.buildClassifier(dataset);
	}
	
	

	public void bidForImpression(Map<Integer, CampaignData> myCampaigns, AdxBidBundle bidBundle, int day, AdxQuery[] queries) {
		@SuppressWarnings("unused")
		int entrySum = 0;

		/*
		 * 
		 */
		for (CampaignData campaign : myCampaigns.values()) {

			int dayBiddingFor = day + 1;

			/* A fixed random bid, for all queries of the campaign */
			/*
			 * Note: bidding per 1000 imps (CPM) - no more than average budget
			 * revenue per imp
			 */

			Random rnd = new Random(); // TODO: Shelly; he we set the bids for impressions
			double avgCmpRevenuePerImp = campaign.getBudget() / campaign.getReachImps();
			double rbid = 1000.0 * rnd.nextDouble() * avgCmpRevenuePerImp;

			/*
			 * add bid entries w.r.t. each active campaign with remaining
			 * contracted impressions.
			 * 
			 * for now, a single entry per active campaign is added for queries
			 * of matching target segment.
			 */

			if ((dayBiddingFor >= campaign.getDayStart())
					&& (dayBiddingFor <= campaign.getDayEnd())
					&& (campaign.impsTogo() >= 0)) {

				int entCount = 0;
				for (int i = 0; i < queries.length; i++) {

					Set<MarketSegment> segmentsList = queries[i]
							.getMarketSegments();

					for (@SuppressWarnings("unused") MarketSegment marketSegment : segmentsList) {
						// TODO: this is very different from the git repository!!!
						//if (campaign.getTargetSegment() == marketSegment) {
						/*
						 * among matching entries with the same campaign id,
						 * the AdX randomly chooses an entry according to
						 * the designated weight. by setting a constant
						 * weight 1, we create a uniform probability over
						 * active campaigns
						 */
						++entCount;
						bidBundle.addQuery(queries[i], rbid, new Ad(null),
								campaign.getId(), 1);
						//}
					}

					if (segmentsList.size() == 0) {
						++entCount;
						bidBundle.addQuery(queries[i], rbid, new Ad(null),
								campaign.getId(), 1);
					}
				}
				double impressionLimit = 0.5 * campaign.impsTogo();
				double budgetLimit = 0.5 * Math.max(0, campaign.getBudget()
						- campaign.getStats().getCost());
				bidBundle.setCampaignDailyLimit(campaign.getId(),
						(int) impressionLimit, budgetLimit);
				entrySum += entCount;
				log.info("Day " + day + ": Updated " + entCount
						+ " Bid Bundle entries for Campaign id " + campaign.getId());
			}
		}
	}



	/* Infrastructure */
	protected ImpressionBidder() {	}

	public static ImpressionBidder getInstance() {
		if (instance == null) {
			instance = new ImpressionBidder();
		}

		return instance;
	}

	public PublisherCatalog getPublisherCatalog() {
		return publisherCatalog;
	}

	public void setPublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
	}

	public List<CampaignData> getMyActiveCampaigns() {
		return myActiveCampaigns;
	}

	public void setMyActiveCampaigns(List<CampaignData> myActiveCampaigns) {
		this.myActiveCampaigns = myActiveCampaigns;
	}

	public AdxBidBundle getBidBundle() {
		return bidBundle;
	}

	public void setBidBundle(AdxBidBundle bidBundle) {
		this.bidBundle = bidBundle;
	}
		

}
