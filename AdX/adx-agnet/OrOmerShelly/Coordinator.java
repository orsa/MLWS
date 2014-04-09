package OrOmerShelly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import OrOmerShelly.oos.bidders.CampaignBidder;
import OrOmerShelly.oos.bidders.ImpressionBidder;

import edu.umich.eecs.tac.props.BankStatus;

import se.sics.tasim.props.SimulationStatus;
import tau.tac.adx.ads.properties.AdType;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.devices.Device;
import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.props.PublisherCatalog;
import tau.tac.adx.props.PublisherCatalogEntry;
import tau.tac.adx.report.adn.AdNetworkReport;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.AdNetBidMessage;
import tau.tac.adx.report.demand.AdNetworkDailyNotification;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.CampaignReport;
import tau.tac.adx.report.demand.CampaignReportKey;
import tau.tac.adx.report.demand.InitialCampaignMessage;
import tau.tac.adx.report.publisher.AdxPublisherReport;
import tau.tac.adx.report.publisher.AdxPublisherReportEntry;

public class Coordinator {
	private final Logger log = Logger
			.getLogger(Coordinator.class.getName());

	private static Coordinator instance = null;
	
	public OOSAgent messenger;
	public ImpressionBidder impressionBidder = ImpressionBidder.getInstance();
	public CampaignBidder campaignBidder = CampaignBidder.getInstance(); // The singleton instance 
	public UCSbidder ucsbidder=UCSbidder.getInstance();
	
	/**
	 * Messages received:
	 * 
	 * We keep all the {@link CampaignReport campaign reports} 
	 * delivered to the agent. We also keep the initialization 
	 * messages {@link PublisherCatalog} and
	 * {@link InitialCampaignMessage} and the most recent messages and reports
	 * {@link CampaignOpportunityMessage}, {@link CampaignReport}, and
	 * {@link AdNetworkDailyNotification}.
	 */
	private final Queue<CampaignReport> campaignReports = new LinkedList<CampaignReport>();
	private PublisherCatalog publisherCatalog;
	private InitialCampaignMessage initialCampaignMessage;
	private AdNetworkDailyNotification adNetworkDailyNotification;
	private final Queue<AdNetworkDailyNotification> adNetworkDailyNotificationsHistory; // TODO: split ucs level history
	private final int FIRST_DAY_OF_BID_BUNDLE_CALCULATION = 0;
	private final int FIRST_DAY_OF_AD_NETWORK_DAILY_NOTIFICATION = 1;
	private final int FIRST_DAY_OF_PUBLISHERS_REPORT = 1;
	/*
	 * we maintain a list of queries - each characterized by the web site (the
	 * publisher), the device type, the ad type, and the user market segment
	 */
	private AdxQuery[] queries;
	
	/**
	 * Information regarding the latest campaign opportunity announced
	 */
	private CampaignData pendingCampaign;
	
	/**
	 * We maintain a collection (mapped by the campaign id) of the campaigns won
	 * by our agent.
	 */
	private Map<Integer, CampaignData> myCampaigns; // TODO: also campaigns that already ended? YES!!!
	// TODO: add a flag to campaigns - active or not
	
	/*
	 * the bidBundle to be sent daily to the AdX
	 */
	private AdxBidBundle bidBundle;
	private Queue<AdxBidBundle> bidBundleHistory;
	
	/*
	 * The current bid level for the user classification service
	 */
	double ucsBid;
	
	/*
	 * The targeted service level for the user classification service
	 */
	double ucsTargetLevel;
	
	double currentUcsLevel;
	Queue<Double> ucsLevelHistory;
	
	double qualityScore;
	Queue<Double> qualityScoreHistory;
	
	Map<String, PublisherStats> publisherDailyStats;
	Queue<Map<String, PublisherStats>> publisherStatsHistory;
	
	/*
	 * current day of simulation
	 */
	public int day;
	
	public double bankBalance;
	
	private Random randomGenerator;
	
	/**
	 * On day 0, a campaign (the "initial campaign") is allocated to each
	 * competing agent. The campaign starts on day 1. The address of the
	 * server's AdxAgent (to which bid bundles are sent) and DemandAgent (to
	 * which bids regarding campaign opportunities may be sent in subsequent
	 * days) are also reported in the initial campaign message
	 */
	public void handleInitialCampaignMessage(
			InitialCampaignMessage campaignMessage) {
		log.info(campaignMessage.toString());

		day = 0;

		initialCampaignMessage = campaignMessage;
		messenger.demandAgentAddress = campaignMessage.getDemandAgentAddress();
		messenger.adxAgentAddress = campaignMessage.getAdxAgentAddress();

		CampaignData campaignData = new CampaignData(initialCampaignMessage);
		campaignData.setBudget(initialCampaignMessage.getReachImps() / 1000.0); // TODO: Shelly - rethink on this calculation

		/*
		 * The initial campaign is already allocated to our agent so we add it
		 * to our allocated-campaigns list.
		 */
		log.info("Day " + day + ": Allocated campaign - " + campaignData);
		getMyCampaigns().put(initialCampaignMessage.getId(), campaignData);
	}

	
	/**
	 * Process the reported set of publishers
	 * 
	 * @param publisherCatalog
	 */
	public void handlePublisherCatalog(PublisherCatalog publisherCatalog) {
		this.publisherCatalog = publisherCatalog;
		generateAdxQuerySpace();
	}

	/**
	 * A user visit to a publisher's web-site results in an impression
	 * opportunity (a query) that is characterized by the the publisher, the
	 * market segment the user may belongs to, the device used (mobile or
	 * desktop) and the ad type (text or video).
	 * 
	 * An array of all possible queries is generated here, based on the
	 * publisher names reported at game initialization in the publishers catalog
	 * message
	 */
	private void generateAdxQuerySpace() {
		if (publisherCatalog != null && queries == null) {
			Set<AdxQuery> querySet = new HashSet<AdxQuery>();

			/*
			 * for each web site (publisher) we generate all possible variations
			 * of device type, ad type, and user market segment
			 */
			for (PublisherCatalogEntry publisherCatalogEntry : publisherCatalog) {
				String publishersName = publisherCatalogEntry
						.getPublisherName();
				for (MarketSegment userSegment : MarketSegment.values()) {
					Set<MarketSegment> singleMarketSegment = new HashSet<MarketSegment>();
					singleMarketSegment.add(userSegment);

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.text));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.mobile, AdType.video));

					querySet.add(new AdxQuery(publishersName,
							singleMarketSegment, Device.pc, AdType.video));

				}
				
				/**
				 * An empty segments set is used to indicate the "UNKNOWN" segment
				 * such queries are matched when the UCS fails to recover the user's
				 * segments.
				 */
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.mobile,
						AdType.text));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.video));
				querySet.add(new AdxQuery(publishersName,
						new HashSet<MarketSegment>(), Device.pc, AdType.text));
			}
			queries = new AdxQuery[querySet.size()];
			querySet.toArray(queries);
		}
	}
	
	
	/**
	 * On day n ( > 0) a campaign opportunity is announced to the competing
	 * agents. The campaign starts on day n + 2 or later and the agents may send
	 * (on day n) related bids (attempting to win the campaign). The allocation
	 * (the winner) is announced to the competing agents during day n + 1.
	 */
	public void handleICampaignOpportunityMessage(
			CampaignOpportunityMessage com) {

		day = com.getDay();

		pendingCampaign = new CampaignData(com);
		log.info("Day " + day + ": Campaign opportunity - " + pendingCampaign);

		/*
		 * The campaign requires com.getReachImps() impressions. The competing
		 * Ad Networks bid for the total campaign Budget (that is, the ad
		 * network that offers the lowest budget gets the campaign allocated).
		 * The advertiser is willing to pay the AdNetwork at most 1$ CPM,
		 * therefore the total number of impressions may be treated as a reserve
		 * (upper bound) price for the auction.
		 */
//		long cmpBid = 1 + Math.abs((randomGenerator.nextLong())
//				% (com.getReachImps()));
//
//		double cmpBidUnits = cmpBid / 1000.0;
		
		double cmpBidUnits = campaignBidder.getBid(pendingCampaign);  // TODO: Or; here we determine the bid.

		log.info("Day " + day + ": Campaign total budget bid: " + cmpBidUnits);

		/*
		 * Adjust ucs bid s.t. target level is achieved. Note: The bid for the
		 * user classification service is piggybacked
		 */

		if (adNetworkDailyNotification != null) {
			double ucsLevel = adNetworkDailyNotification.getServiceLevel();
			double prevUcsBid = ucsBid;

			/* UCS Bid should not exceed 0.2 */
			ucsBid = ucsbidder.getBid(this);

			log.info("Day " + day + ": Adjusting ucs bid: was " + prevUcsBid
					+ " level reported: " + ucsLevel + " target: "
					+ ucsTargetLevel + " adjusted: " + ucsBid);
		} else {
			log.info("Day " + day + ": Initial ucs bid is " + ucsBid);
		}

		/* Note: Campaign bid is in millis */
		AdNetBidMessage bids = new AdNetBidMessage(ucsBid, pendingCampaign.getId(),  // here we send a bid for campaign oppor. and ucs combined.
				(long)cmpBidUnits);
		messenger.sendMessageOnBidsAndUcs(bids);
	}
	
	
	/** Updates the bank balance **/
	public void handleBankStatus(BankStatus content) {
		log.info("Day " + day + " :" + content.toString());
		bankBalance = content.getAccountBalance();
	}
	
	
	/**
	 * On day n ( > 0), the result of the UserClassificationService and Campaign
	 * auctions (for which the competing agents sent bids during day n -1) are
	 * reported. The reported Campaign starts in day n+1 or later and the user
	 * classification service level is applicable starting from day n+1.
	 */
	public void handleAdNetworkDailyNotification(
			AdNetworkDailyNotification notificationMessage) {

		if (day > FIRST_DAY_OF_AD_NETWORK_DAILY_NOTIFICATION) {
			adNetworkDailyNotificationsHistory.add(adNetworkDailyNotification); // Add last notification to history
			qualityScoreHistory.add(qualityScore);
			ucsLevelHistory.add(currentUcsLevel);
		}
		
		adNetworkDailyNotification = notificationMessage;

		campaignBidder.updateCampaignes(notificationMessage.getCampaignId(),
										notificationMessage.getWinner(),
										notificationMessage.getPrice()
										); // [orsa:] collect data for ML
		
		log.info("Day " + day + ": Daily notification for campaign "
				+ adNetworkDailyNotification.getCampaignId());

		String campaignAllocatedTo = " allocated to "
				+ notificationMessage.getWinner();

		if ((pendingCampaign.getId() == adNetworkDailyNotification.getCampaignId())
				&& (notificationMessage.getCost() != 0)) {

			/* add campaign to list of won campaigns */
			pendingCampaign.setBudget(notificationMessage.getCost());

			getMyCampaigns().put(pendingCampaign.getId(), pendingCampaign);

			campaignAllocatedTo = " WON at cost "
					+ notificationMessage.getCost();
			
			campaignBidder.updateWonPendingCampaign();
		}
		
		qualityScore = notificationMessage.getQualityScore();
		
		currentUcsLevel = notificationMessage.getServiceLevel();

		log.info("Day " + day + ": " + campaignAllocatedTo
				+ ". UCS Level set to " + notificationMessage.getServiceLevel()
				+ " at price " + notificationMessage.getPrice()
				+ " Quality Score is: " + notificationMessage.getQualityScore());
	}
	
	
	/**
	 * The SimulationStatus message received on day n indicates that the
	 * calculation time is up and the agent is requested to send its bid bundle
	 * to the AdX.
	 */
	public void handleSimulationStatus(SimulationStatus simulationStatus) {
		log.info("Day " + day + " : Simulation Status Received");
		calculateAndSendBidBundle();
		log.info("Day " + day + " ended. Starting next day");
		++day;
	}

	private void calculateAndSendBidBundle() {
		if (day > FIRST_DAY_OF_BID_BUNDLE_CALCULATION) {
			bidBundleHistory.add(bidBundle);
		}
		
		impressionBidder.updateDay(day);
		impressionBidder.setPreviousBidBundle(bidBundle);
		try {
			impressionBidder.fillBidBundle();
			bidBundle = impressionBidder.getBidBundle();
		} catch (Exception e) {
			// TODO What to do?
			bidBundle = null;
		}
		
		
		if (bidBundle != null) {
			log.info("Day " + day + ": Sending BidBundle");
			messenger.sendBidAndAds(bidBundle);
		} else {
			log.warning("BidBundle is null!");
		}
	}
	
	
	/**
	 * Campaigns performance w.r.t. each allocated campaign
	 */
	public void handleCampaignReport(CampaignReport campaignReport) {

		campaignReports.add(campaignReport);

		/*
		 * for each campaign, the accumulated statistics from day 1 up to day
		 * n-1 are reported
		 */
		for (CampaignReportKey campaignKey : campaignReport.keys()) {
			int cmpId = campaignKey.getCampaignId();
			CampaignStats cstats = campaignReport.getCampaignReportEntry(
					campaignKey).getCampaignStats();
			getMyCampaigns().get(cmpId).setStats(cstats);

			log.info("Day " + day + ": Updating campaign " + cmpId + " stats: "
					+ cstats.getTargetedImps() + " tgtImps "
					+ cstats.getOtherImps() + " nonTgtImps. Cost of imps is "
					+ cstats.getCost());
		}
	}

	
	/**
	 * Users and Publishers statistics: popularity and ad type orientation
	 */
	public void handleAdxPublisherReport(AdxPublisherReport adxPublisherReport) {
		if (day > FIRST_DAY_OF_PUBLISHERS_REPORT) {
			publisherStatsHistory.add(publisherDailyStats);
		}
		
		publisherDailyStats = new HashMap<String, PublisherStats>();
		
		log.info("Publishers Report: ");
		for (PublisherCatalogEntry publisherKey : adxPublisherReport.keys()) {
			AdxPublisherReportEntry entry = adxPublisherReport
					.getEntry(publisherKey);
			log.info(entry.toString());
			PublisherStats publisherStats = fetchPublisherStats(entry);
			publisherDailyStats.put(publisherKey.getPublisherName(), publisherStats);
		}
	}
	
	private PublisherStats fetchPublisherStats(AdxPublisherReportEntry entry) {
		return new PublisherStats(entry.getPopularity(),
									entry.getAdTypeOrientation().get(AdType.video),
									entry.getAdTypeOrientation().get(AdType.text));
	}


	/**
	 * 
	 * @param AdNetworkReport
	 */
	public void handleAdNetworkReport(AdNetworkReport adnetReport) {
		// TODO: Shelly - read class notes - why commented out? 
		// This is a map per AdNetQuery of wins/costs/bids result for impression auction - go over this and use it
		ucsbidder.updateUCS(adnetReport, this, this.getMyCampaigns());
		log.info("Day "+ day + " : AdNetworkReport = " + adnetReport);
		//adnetReport.keys().iterator().next().
		/*
		 for (AdNetworkKey adnetKey : adnetReport.keys()) {
		 
			double rnd = Math.random();
			if (rnd > 0.95) {
				AdNetworkReportEntry entry = adnetReport
						.getAdNetworkReportEntry(adnetKey);
				log.info(adnetKey + " " + entry);
			}
		}
        */
		
		for (AdNetworkKey adnetKey : adnetReport.keys()) {
			AdNetworkReportEntry adnetEntry = adnetReport.getAdNetworkReportEntry(adnetKey);
			// TODO: get for a certain campaign id all wins and losses and calculate revenues.
			// if won, update instance with the second prize paid.
		}
		
		// TODO impressionBidder.completeLastIntsancesAndUpdate();

	}
	

	public void initSimulation() {
		randomGenerator = new Random();
		day = 0;
		bidBundle = new AdxBidBundle();
		ucsTargetLevel = 0.5 + (randomGenerator.nextInt(5) + 1) / 10.0;
		
		/* initial bid between 0.1 and 0.2 */
		ucsBid = 0.1 + 0.1*randomGenerator.nextDouble(); //TODO: Omer; check this out - the ucs bit for the first day.
		
		setMyCampaigns(new HashMap<Integer, CampaignData>());
	}

	public void finiSimulation() {
		campaignReports.clear();
		bidBundle = null;
	}
	
	
	/* Infrastructure */	
	public Coordinator(OOSAgent sampleAdNetwork) {
		this.messenger = sampleAdNetwork;
		this.adNetworkDailyNotificationsHistory = new LinkedList<AdNetworkDailyNotification>();
		this.bidBundleHistory = new LinkedList<AdxBidBundle>();
	}

	public static Coordinator getInstance(OOSAgent oosAgent) {
		if (instance == null) {
			instance = new Coordinator(oosAgent);
		}
		
		return instance;
	}


	public Map<Integer, CampaignData> getMyCampaigns() {
		return myCampaigns;
	}


	public void setMyCampaigns(Map<Integer, CampaignData> myCampaigns) {
		this.myCampaigns = myCampaigns;
	}

}
