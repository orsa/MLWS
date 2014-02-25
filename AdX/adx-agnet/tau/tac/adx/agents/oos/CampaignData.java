package tau.tac.adx.agents.oos;
import tau.tac.adx.demand.CampaignStats;
import tau.tac.adx.report.adn.MarketSegment;
import tau.tac.adx.report.demand.CampaignOpportunityMessage;
import tau.tac.adx.report.demand.InitialCampaignMessage;


public class CampaignData {
		/* campaign attributes as set by server */
		private Long reachImps;
		private long dayStart;
		private long dayEnd;
		private MarketSegment targetSegment;
		double videoCoef;
		double mobileCoef;
		private int id;

		/* campaign info as reported */
		private CampaignStats stats;
		private double budget;

		public CampaignData(InitialCampaignMessage icm) {
			setReachImps(icm.getReachImps());
			setDayStart(icm.getDayStart());
			setDayEnd(icm.getDayEnd());
			setTargetSegment(icm.getTargetSegment());
			videoCoef = icm.getVideoCoef();
			mobileCoef = icm.getMobileCoef();
			setId(icm.getId());

			setStats(new CampaignStats(0, 0, 0));
			setBudget(0.0);
		}

		public void setBudget(double d) {
			budget = d;
		}

		public CampaignData(CampaignOpportunityMessage com) {
			setDayStart(com.getDayStart());
			setDayEnd(com.getDayEnd());
			setId(com.getId());
			setReachImps(com.getReachImps());
			setTargetSegment(com.getTargetSegment());
			mobileCoef = com.getMobileCoef();
			videoCoef = com.getVideoCoef();
			setStats(new CampaignStats(0, 0, 0));
			setBudget(0.0);
		}

		@Override
		public String toString() {
			return "Campaign ID " + getId() + ": " + "day " + getDayStart() + " to "
					+ getDayEnd() + " " + getTargetSegment().name() + ", reach: "
					+ getReachImps() + " coefs: (v=" + videoCoef + ", m="
					+ mobileCoef + ")";
		}

		public int impsTogo() {
			return (int) Math.max(0, getReachImps() - getStats().getTargetedImps());
		}

		void setStats(CampaignStats s) {
			stats.setValues(s);
		}

		public double getBudget() {
			return budget;
		}

		public Long getReachImps() {
			return reachImps;
		}

		public void setReachImps(Long reachImps) {
			this.reachImps = reachImps;
		}

		public MarketSegment getTargetSegment() {
			return targetSegment;
		}

		public void setTargetSegment(MarketSegment targetSegment) {
			this.targetSegment = targetSegment;
		}

		public long getDayStart() {
			return dayStart;
		}

		public void setDayStart(long dayStart) {
			this.dayStart = dayStart;
		}

		public long getDayEnd() {
			return dayEnd;
		}

		public void setDayEnd(long dayEnd) {
			this.dayEnd = dayEnd;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public CampaignStats getStats() {
			return stats;
		}

	}
