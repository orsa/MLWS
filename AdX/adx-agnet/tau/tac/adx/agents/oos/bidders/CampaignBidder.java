package tau.tac.adx.agents.oos.bidders;

import tau.tac.adx.agents.CampaignData;

public class CampaignBidder {

	private static CampaignBidder instance = null;
	   
	/* input:
		Reach - higher target-reach means higher costly campaign.
		Duration - longer campaign means cheaper (since we have more time to achieve the goal).
		target audience.
		--
		UCS level (better level make UCS  results weightier)
		predicted impressions per target audience - more impressions means cheaper bid.	
	   ----
	*/
	protected CampaignBidder() {
	      // Exists only to defeat instantiation.
	}
	   
	public static CampaignBidder getInstance() {
		
		if(instance == null) {
			instance = new CampaignBidder();
	    }
	    
		return instance;
	}
	
	public double getBid(tau.tac.adx.agents.oos.CampaignData newCampaign){
	
		/*
		 * here we do a magic.
		 * use a smart assessment algorithm to generate the next bid. 
		 * 
		 * [Note: machine learning algorithem might not fit here, since it takes a lot of time (game days) to
		 * figure out whether a particular bid was payed-off (winning the campaignis NOT the only target) 
		 * 
		 * we shall try:
		 * BID = (alpha)*[(Reach)/(Duration)] * 1/(1 + targetAudienceScore)
		 *  plus some more parameters and normalizations.
		 */
		
		
		
		return 0;
	}
	
	
	/*
	 * A method to update the bidder for every campaign bidding result , in order to 
	 * store a history.
	 */
	public void updateCampaignes(int campaignId, String winner, double price ) {
		
	// todo: save the data - maybe also process some calculations.
		
	}

}
