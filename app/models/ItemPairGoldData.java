package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="itempair_gold_data")
public class ItemPairGoldData extends Model {

	private static final long serialVersionUID = -7782083996595353299L;
	
	@Id
	public Long id;
	
	public String item1Id;
	public String item2Id;
	
	@ManyToOne
	public Job job;

}
