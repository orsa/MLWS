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
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.SparseInstance;
import weka.core.Instances;
import edu.umich.eecs.tac.props.Ad;

public class ImpressionBidder {

	private static ImpressionBidder instance = null;

	private final Logger log = Logger
			.getLogger(ImpressionBidder.class.getName());

	private final static double CPM = 1000.0;



	private static final int REMAINING_BUDGET_ATTR_INDEX = 5;
	private static final int PRIORITY_ATTR_INDEX = 6;

	private UserAnalyzer userAnalyzer;
	private PublisherCatalog publisherCatalog;
	private Map<String, PublisherStats> publishersStats;
	private List<CampaignData> myActiveCampaigns; // mutable!!!
	@SuppressWarnings("unused")
	private double bankBalance;
	
	private AdxBidBundle bidBundle;
	private AdxBidBundle previousBidBundle;
	
	private Instance lastInstance = null; // Doesn't contain campaign information and bid
	private Instances dataset = null; // On the first day this is a example dataset which is based on budget/reach impressions.
	private Classifier classifier; // Must be an updateable classifier
	
	private int day; // TODO: check exactly which day should be given: current day or day bidding for (that is currentDay + 1)?
	
	public void init(Classifier newClassifier, CampaignData currentCampaign, int day) throws Exception { // Called one time during the game or when a classifier changes
		this.day = day;
		classifier = newClassifier;
		dataset = getDefaultDataset(currentCampaign);
		trainClassifier();
		
	}
	
	// TODO: set daily limits for campaign and overall - do not want to get a minus in bankBalance?
	
	private Instances getDefaultDataset(CampaignData currentCampaign) {
		double budget = currentCampaign.getBudget();
		double priority = getCampaignPriority(currentCampaign);
		
		double[] attributes = new double[2];
		attributes[0] = budget;
		attributes[1] = priority;
		
		int[] indicesToFill = new int[2];
		indicesToFill[0] = 5;
		indicesToFill[1] = 6;
		
		Instance defaultInstance = new SparseInstance(1, attributes, indicesToFill, 1); 
		
		double avgCmpRevenuePerImp = budget / currentCampaign.getReachImps();
		defaultInstance.setClassValue(avgCmpRevenuePerImp / priority);
		
		FastVector attributeNames = new FastVector();
		attributeNames.insertElementAt(new Attribute("popularity"), 0);
		attributeNames.insertElementAt(new Attribute("adType"), 1);
		attributeNames.insertElementAt(new Attribute("adTypeOrientation"), 2);
		attributeNames.insertElementAt(new Attribute("weightMarketSegment"), 3);
		attributeNames.insertElementAt(new Attribute("device"), 4);
		attributeNames.insertElementAt(new Attribute("remainingBudget"), 5);
		attributeNames.insertElementAt(new Attribute("priority"), 6);
		attributeNames.insertElementAt(new Attribute("bid"), 7);
		
		Instances defaultInstances = new Instances("NAMEOFRELATION", 
				attributeNames,
				Integer.MAX_VALUE); // Test this... Weka3.7 is more intuitive using a list and not a FastVector
		
		defaultInstances.add(defaultInstance);
		
		return defaultInstances;
		
	}

	// TODO: Optimisation: sort myActiveCampaigns in Coordinator so prioritizing will be more efficient! 
	public void fillBidBundle() throws Exception {

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
				
				// Define an instance for classification - filter relevant data according to stage
				generateFirstInstance(impParams, publisherName, marketSegmentWithWeight.getRight());
				
				// Initial bid calculates what does the impression worth
				try {
					bid = initialBid();
				} catch (Exception e) {
					log.severe("Failed to classifiy initial bid. Returning last bid");
					bid = getLastBidAveraged(impParams, publisherName);
				}
				
				// Now we will calculate what does the impression worth *for us*.
				List<CampaignData> relevantCampaigns = filterCampaigns(impParams);
				if (exists(relevantCampaigns)) {
					prioritizeCampaigns(relevantCampaigns);
					bid = calcBid(bid, relevantCampaigns);
				} else {
					if (isUnknown(impParams)) {
						List<CampaignData> urgentCampaigns = getUrgentCampaigns();
						bid = calcBidForUnknown(urgentCampaigns);
					}
				}

				addToBidBundle(publisherName, impParams, CPM*bid, campaignWeightVector); // Question: do we bid per impression or per 1000 impressions?
			}

		}
	}
	
	// TODO: sort out attributes and such
	@SuppressWarnings("unused")
	private Instance generateFullInstance(ImpressionParameters impParams,
			String publisherName, Double weight, double remainingBudget, double priority, double bid) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		int adTypeOrientation = getAdTypeOrientation(impParams, publisherStats);
		
		double[] serializedValues = serializeValues(publisherStats.getPopularity(),
				impParams.getAdType(),
				adTypeOrientation,
				weight,
				impParams.getDevice(),
				remainingBudget,
				priority, 
				bid);
		
		return new SparseInstance(1, serializedValues);  
	}

	private void generateFirstInstance(ImpressionParameters impParams,
			String publisherName, Double weight) {
		PublisherStats publisherStats = publishersStats.get(publisherName);
		int adTypeOrientation = getAdTypeOrientation(impParams, publisherStats);
		
		double[] serializedValues = serializeValues(publisherStats.getPopularity(),
				impParams.getAdType(),
				adTypeOrientation,
				weight,
				impParams.getDevice(),
				0, 0, 0);
		
		lastInstance = new SparseInstance(1, serializedValues);  
	}

	private double initialBid() throws Exception {
		int[] indicesToFill = new int[5];
		indicesToFill[0] = 0;
		indicesToFill[1] = 1;
		indicesToFill[2] = 2;
		indicesToFill[3] = 3;
		indicesToFill[4] = 4;
		
		Instance initialBidInstance = new SparseInstance(1, filter(lastInstance.toDoubleArray()), indicesToFill, 1);
		
		return (double)classifier.classifyInstance(initialBidInstance);
	}

	private double[] filter(double[] allAttributes) {
		double[] values = new double[5];
		values[0] = allAttributes[0];
		values[1] = allAttributes[1];
		values[2] = allAttributes[2];
		values[3] = allAttributes[3];
		values[4] = allAttributes[4];
		return values;
	}

	private double getLastBidAveraged(ImpressionParameters impParams,
			String publisherName) {
		int count = impParams.getMarketSegments().size();
		double sum = 0;
		Device device = impParams.getDevice();
		AdType adType = impParams.getAdType();
		
		for (MarketSegment marketSegment : impParams.getMarketSegments()) {
			sum += previousBidBundle.getBid(new AdxQuery(publisherName, marketSegment, device, adType));	
		}
		
		return sum / count;
		
	}

	private double[] serializeValues(int popularity, AdType adType,
			int adTypeOrientation, Double weight,
			Device device, double remainingBudget, double priority, double bid) {
		double[] values = new double[8];
		values[0] = (double)popularity;
		values[1] = (double)adType.ordinal();
		values[2] = (double)adTypeOrientation;
		values[3] = weight != null ? (double)weight : 0;
		values[4] = (double)device.ordinal();
		values[5] = remainingBudget;
		values[6] = priority;
		values[7] = bid;
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

	private double calcBidForUnknown(List<CampaignData> urgentCampaigns) {
		return 0;
		// TODO Auto-generated method stub
		
	}

	private List<CampaignData> getUrgentCampaigns() {
		prioritizeCampaigns(myActiveCampaigns);
		return myActiveCampaigns;
	}

	private boolean isUnknown(ImpressionParameters impParams) {
		return impParams.getMarketSegments().isEmpty();
	}

	private double calcBid(double currentBid, List<CampaignData> relevantCampaigns) throws Exception {
		// TODO: Generate a bid for each of the campaign based on the campaign data and the initial bid
		List<Double> bidsForRelevantCampaigns = new ArrayList<Double>(relevantCampaigns.size());
		for (CampaignData relevantCampaign : relevantCampaigns) {
			Instance campaignInstnace = enrichInstance(relevantCampaign.getBudget(), // TODO Remaining budget, not whole budget
					getCampaignPriority(relevantCampaign));
			
			bidsForRelevantCampaigns.add(classifier.classifyInstance(campaignInstnace));			
		}
		// TODO: average all bids using weighted average with the priorities OR take the max bid
		return averageBids(bidsForRelevantCampaigns, relevantCampaigns);
	}
	
	private double averageBids(List<Double> bidsForRelevantCampaigns,
			List<CampaignData> relevantCampaigns) throws Exception {
		int sum = 0;
		int sumPriorities = 0;
		if (bidsForRelevantCampaigns.size() != relevantCampaigns.size()) {
			log.severe("The sizes of the list of bids for relevant campaigns and the size of the list of the relevant campaigns should be equal!");
			throw new Exception("Not enough/Too many Bids for relevant campaigns");
		}
		
		for (int i = 0 ; i < bidsForRelevantCampaigns.size() ; i++) {
			double bid = bidsForRelevantCampaigns.get(i);
			double priority = getCampaignPriority(relevantCampaigns.get(i));
			
			sum += bid*priority;
			sumPriorities += priority;
		}
		
		return sum/sumPriorities;
	}

	private Instance enrichInstance(double remainingBudget, double priority) {
		Instance enriched = new SparseInstance(lastInstance); 
		// TODO: Create a data structure for attributes and sort out their types. I can make them nominal (like device and adtype) and numeric (like bid and priority)
		// TODO that way I won't need to serialize the values...
		enriched.setValue(REMAINING_BUDGET_ATTR_INDEX, remainingBudget);
		enriched.setValue(PRIORITY_ATTR_INDEX, priority);
		
		return enriched;
	}

	private double getCampaignPriority(CampaignData campaign) { // TODO: check that can't divide by zero
		return (double)campaign.impsTogo() / ((double)(campaign.getDayEnd() - day));
	}

	private void prioritizeCampaigns(List<CampaignData> relevantCampaigns) {
		Collections.sort(relevantCampaigns, new Comparator<CampaignData>() {
			@Override
			public int compare(CampaignData campaign1, CampaignData campaign2) {
				Double priorityCampaign1 = getCampaignPriority(campaign1);
				Double priorityCampaign2 = getCampaignPriority(campaign2);
				return priorityCampaign1.compareTo(priorityCampaign2);
			}
		});		
	}

	private List<CampaignData> filterCampaigns(ImpressionParameters impParams) {
		List<CampaignData> filteredCampaigns = new ArrayList<CampaignData>();
		AdType impressionAdType = impParams.getAdType();
		Device impressionDevice =  impParams.getDevice();
		Set<MarketSegment> impressionMarketSegments = impParams.getMarketSegments();
		
		for (CampaignData campaign : myActiveCampaigns) {
			boolean addedCampaign = false;
			
			// Step1: is the campaign fulfilled?
			if (isFulfilled(campaign)) {
				continue;
			}
			
			// Step2: does the campaign fit the impressions characteristics and market segment?
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

	private boolean isFulfilled(CampaignData campaign) {
		return campaign.impsTogo() == 0;
	}

	private boolean exists(List<CampaignData> campaigns) {
		if (campaigns == null || campaigns.size() == 0) {
			return false;
		}

		return true;	
	}
	
	
	public void trainClassifier() throws Exception { // TODO: train on all instances given, update with the "winning" bid
		if (dataset == null) {
			log.severe("Can't train classifier if dataset wasn't loaded. Make sure to init the ImpressionBidder properly");
		}
		
		if (lastInstance != null) {
			dataset.add(lastInstance);
		}
		
		classifier.buildClassifier(dataset);
	}
	
	public void updateClassifier() throws Exception {
		if (classifier instanceof UpdateableClassifier)
			((UpdateableClassifier) classifier).updateClassifier(lastInstance);
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

	public AdxBidBundle getPreviousBidBundle() {
		return previousBidBundle;
	}

	public void setPreviousBidBundle(AdxBidBundle previousBidBundle) {
		this.previousBidBundle = previousBidBundle;
	}

	public void updateDay(int day) {
		this.day = day;
	}
	
	
		

}
