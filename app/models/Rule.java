package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="rule")
public class Rule extends Model {
	private static final long serialVersionUID = -1276946796999822969L;
	
	@Id
	public Long id;
	
	public String name;
	public double precision;
	public double coverage;
	
	@ManyToOne
	public Job job;
}
