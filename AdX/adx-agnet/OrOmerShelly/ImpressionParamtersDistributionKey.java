package OrOmerShelly;

public class ImpressionParamtersDistributionKey {

	private ImpressionParameters impParams;
	private Double	weight;
		
	public ImpressionParamtersDistributionKey(ImpressionParameters impParams,
			Double weight) {
		super();
		this.impParams = impParams;
		this.weight = weight;
	}

	public ImpressionParameters getImpParams() {
		return impParams;
	}
	
	public void setImpParams(ImpressionParameters impParams) {
		this.impParams = impParams;
	}
	
	public Double getWeight() {
		return weight;
	}
	
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	
	
	
}
