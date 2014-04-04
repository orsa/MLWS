package OrOmerShelly.oos.bidders;

import tau.tac.adx.agents.CampaignData;
import java.util.List;
import java.util.Map;

import OrOmerShelly.Coordinator;



	



public class UCSbidder {
	/*
	 * Things to consider:
	 * 1.  I had to add setters and getters for MyCampaigns.
	 * 2. I have 5 states representing situations before the bid and 5 after it.
	 * perhaps we should make a transition from each pre-bid state to only 3 post-bid states (instead of all 5)?
	 * 3. Obviously - change the range and prices, maybe even alpha and learningRate
	 * 4. Perhaps we want to pay a bit more wisely on the first 5-6 days? (maybe a strategy we have?)
	 * 5. Still need to calculate the reinforcements
	 */
	final int stateA = 0;
	final int stateB = 1;
	final int stateC = 2;
	final int stateD = 3;
	final int stateE = 4;
	final int stateF = 5;
	final int stateG = 6;
	final int stateH = 7;
	final int stateI = 8;
	final int stateJ = 9;
	final int stateK = 10;
	final int statesCount=11;
	final int possibleActions=5;
	final double range0=1; // range for the bid
	final double range1=2;
	final double range2=3;
	final double range3=4;
	final double finalRange0=1; // range for the reinforcement
	final double finalRange1=2;
	final double finalRange2=3;
	final double finalRange3=4;
	final double[] prices={0,1,2,3,4};
	 double alpha=0.1;
	 double gamma=0.9;
	 int state;
	 int nextstate;
	 int action;
	 int[] actionsFromA = new int[] { stateB, stateC, stateD, stateE, stateF };
	 int[] actionsFromB = new int[] { stateG, stateH, stateI, stateJ, stateK };
	 int[] actionsFromC = new int[] { stateG, stateH, stateI, stateJ, stateK };
	 int[] actionsFromD = new int[] { stateG, stateH, stateI, stateJ, stateK };
	 int[] actionsFromE = new int[] { stateG, stateH, stateI, stateJ, stateK };
	 int[] actionsFromF = new int[] { stateG, stateH, stateI, stateJ, stateK };
	 int[][] actions = new int[][] { actionsFromA, actionsFromB, actionsFromC,
			actionsFromD, actionsFromE, actionsFromF };
	int[][] R = new int[statesCount][statesCount]; // reward lookup
	 double[][] Q = new double[statesCount][possibleActions]; // Q learning
	static List<Double> amountpaid; // the ith entry represents the amount paid on the ith day.
	static List<Integer> placeReached; // the ith entry represents the UCS place we got on the ith day.
	private static UCSbidder instance = null;

	protected UCSbidder() {
	      // Exists only to defeat instantiation.
	}

	public static UCSbidder getInstance() {

		if(instance == null) {
			instance = new UCSbidder();
	    }

		return instance;
	}

	public double getBid(Coordinator co){
		/*
		 * That's the point where we do the core of the Sadna.
		 * I think I'll use Q-learning (http://en.wikipedia.org/wiki/Q-learning),
		 * unless somebody has a better idea!
		 */



		return bid(co);
	}
	double bid(Coordinator co) {
		/*
         1. Set parameter , and environment reward matrix R 
         2. Initialize matrix Q as zero matrix 
         3. For each episode: Select random initial state 
            Do while not reach goal state o 
                Select one among all possible actions for the current state o 
                Using this possible action, consider to go to the next state o 
                Get maximum Q value of this next state based on all possible actions o 
                Compute o Set the next state as the current state
		 */

		// For each episode
		//for (int i = 0; i < 1000; i++) { // train episodes
		// Select random initial state
		//int state = rand.nextInt(statesCount);
		int state=findState(co);
		int action=(int) Math.round(maxQ(state)[1]);
		this.action=action;
		this.state=state;
		return prices[action];

		/*while (state != stateC) // goal state
		{
			// Select one among all possible actions for the current state
			int[] actionsFromState = actions[state];

			// Selection strategy is random in this example
			int index = rand.nextInt(actionsFromState.length);
			int action = actionsFromState[index];

			// Action outcome is set to deterministic in this example
			// Transition probability is 1
			int nextState = action; // data structure

			// Using this possible action, consider to go to the next state
			double q = Q(state, action);
			double maxQ = maxQ(nextState);
			int r = R(state, action);

			double value = q + alpha * (r + gamma * maxQ - q);
			setQ(state, action, value);

			// Set the next state as the current state
			state = nextState;
		} */
	}


	/*
	 * A method to update the bidder for every campaign bidding result , in order to 
	 * store a history.
	 */
	double Q(int s, int a) {
		return Q[s][a];
	}
	public void updateUCS(Map<Integer, CampaignData> myCampaigns, double amountPaidYesterday, int place, int currentDay ) {
		/*
		 * perhaps amountPaidYesterday will not be a parameter,
		 *  but will be passed somewhere else? 
		 */
		double reinforecement=findReinforcement();
		int nextState=-1;
		
		double q = Q(state, action);
		double maxQ = maxQ(nextState)[0];
		double value = q + alpha * (reinforecement + gamma * maxQ - q);
		setQ(state, action, value);
		//int r = R(state, action);

	}
	public  double[] maxQ(int s) {
		double[] result=new double[2];
		int[] actionsFromState = actions[s];
		double maxValue = Double.MIN_VALUE;
		for (int i = 0; i < actionsFromState.length; i++) {
			int nextState = actionsFromState[i];
			double value = Q[s][nextState];

			if (value > maxValue)
				result[0]=value;
				result[1]=i;
				maxValue = value;
		}
		return result;
	}
	int findState(Coordinator co) {
		double accume=0.0;
		for (int i=0; i<co.getMyCampaigns().size() ; i++) { // I had to add setters and getters for MyCampaigns.
			accume+=((1.0)*(co.getMyCampaigns().get(i).impsTogo()))/(co.getMyCampaigns().get(i).getReachImps());
		}
		if (accume<range0) return 1;
		if (accume<range1) return 2;
		if (accume<range2) return 3;
		if (accume<range3) return 4;
		return 5;
		
	}
	
	double findReinforcement() {
		return 0;
	}
	
	int findFinalState(double reinforcement) {
		if (reinforcement<finalRange0) return stateG;
		if (reinforcement<finalRange1) return stateH;
		if (reinforcement<finalRange2) return stateI;
		if (reinforcement<finalRange3) return stateJ;
		return stateJ;
	}
	
	void setQ(int s, int a, double value) {
		Q[s][a] = value;
	}


}
