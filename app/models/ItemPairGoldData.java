package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;

import play.db.ebean.Model;

@Entity
@Table(name="itempair_gold_data")
public class ItemPairGoldData extends Model {

	private static final long serialVersionUID = -7782083996595353299L;
	
	@Id
	public Long id;
	
	public int itemPairId;
	
	public String item1Id;
	public String item2Id;
	
	public MatchStatus matchStatus;
	public boolean isLabelledInTrainPhase = true;
	
	public ItemPairGoldData(int itemPairId, String item1Id, String item2Id, MatchStatus matchStatus) 
	{
		super();
		this.itemPairId = itemPairId;
		this.item1Id = item1Id;
		this.item2Id = item2Id;
		this.matchStatus = matchStatus;
	}
	
	public ItemPairGoldData(String item1Id, String item2Id, MatchStatus matchStatus) 
	{
		super();
		this.itemPairId = getItemPairId();
		this.item1Id = item1Id;
		this.item2Id = item2Id;
		this.matchStatus = matchStatus;
	}	
	
	@ManyToOne
	public Job job;

	/**
     * Generic query helper for entity Project with id Long
     */
    public static Finder<Long,ItemPairGoldData> find = new Finder<Long,ItemPairGoldData>(Long.class, ItemPairGoldData.class);
    
    public int getItemPairId()
    {
		String id = item1Id + "#" + item2Id;
		return id.replace(" ", "").trim().hashCode();
    }
}
