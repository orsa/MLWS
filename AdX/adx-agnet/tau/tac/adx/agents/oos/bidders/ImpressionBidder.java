package tau.tac.adx.agents.oos.bidders;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import tau.tac.adx.props.AdxBidBundle;
import tau.tac.adx.props.AdxQuery;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.agents.oos.CampaignData;
import tau.tac.adx.agents.oos.SampleAdNetwork;
import edu.umich.eecs.tac.props.Ad;

public class ImpressionBidder {
	
	private final Logger log = Logger
			.getLogger(ImpressionBidder.class.getName());
	
	public void bidForImpression(Map<Integer, CampaignData> myCampaigns, AdxBidBundle bidBundle, int day, AdxQuery[] queries) {
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

					for (MarketSegment marketSegment : segmentsList) {
						if (campaign.getTargetSegment() == marketSegment) {
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
						}
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

}
