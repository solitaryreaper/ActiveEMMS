package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="item_data")
public class ItemData extends Model {

	private static final long serialVersionUID = 2573816915313200503L;
	
	@Id
	public Long id;
	
	public int datasourceId;
	public String itemId;
	public String attribute;
	public String value;
	
	@ManyToOne
	public Job job;

	public ItemData(int dataSourceId, String itemId, String attribute, String value) {
		super();
		this.datasourceId = dataSourceId;
		this.itemId = itemId;
		this.attribute = attribute;
		this.value = value;
	}

	/**
     * Generic query helper for entity Itemdata with id Long
     */
    public static Finder<Long,ItemData> find = new Finder<Long,ItemData>(Long.class, ItemData.class);	
}
