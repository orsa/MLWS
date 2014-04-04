package OrOmerShelly;

public class PublisherStats {

	private int popularity;
	private int videoOrientation;
	private int textOrientation;
	
	// TODO: check if orientation could be null?
	public PublisherStats(int popularity, Integer videoOrientation, Integer textOrientation) {
		this.popularity = popularity;
		this.videoOrientation = videoOrientation;
		this.textOrientation = textOrientation;
	}
	
	public int getPopularity() {
		return popularity;
	}
	
	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}
	
	public int getVideoOrientation() {
		return videoOrientation;
	}
	
	public void setVideoOrientation(int videoOrientation) {
		this.videoOrientation = videoOrientation;
	}
	
	public int getTextOrientation() {
		return textOrientation;
	}
	
	public void setTextOrientation(int textOrientation) {
		this.textOrientation = textOrientation;
	}	
}
