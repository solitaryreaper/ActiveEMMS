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
	
	public String itemId;
	public String attribute;
	public String value;
	
	@ManyToOne
	public Job job;

}
