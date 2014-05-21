package models;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="feature_data")
public class FeatureData extends Model
{
	private static final long serialVersionUID = 2573816915313200503L;
	
	@Id
	public Long id;
	
	public int itemPairId;
	
	public String featureName;
	public double featureValue;
	
	@ManyToOne
	public Job job;
	
	public FeatureData(int itemPairId, String featureName, double featureValue)
	{
		this.itemPairId = itemPairId;
		this.featureName = featureName;
		this.featureValue = featureValue;
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FeatureData [id=").append(id).append(", itemPairId=")
				.append(itemPairId).append(", featureName=")
				.append(featureName).append(", featureValue=")
				.append(featureValue).append(", job=").append(job).append("]");
		return builder.toString();
	}

	public static Finder<Long,FeatureData> find = new Finder<Long,FeatureData>(Long.class, FeatureData.class);
}
