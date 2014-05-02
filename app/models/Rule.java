package models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.ebean.Model;
import play.db.ebean.Model.Finder;

@Entity
@Table(name="rule")
public class Rule extends Model {
	private static final long serialVersionUID = -1276946796999822969L;
	
	@Id
	public Long id;

	public String name;
	public double precisionMetric;
	public double coverageMetric;
	
	@ManyToOne
	public Job job;
	
	/**
     * Generic query helper for entity Project with id Long
     */
    public static Finder<Long,Rule> find = new Finder<Long,Rule>(Long.class, Rule.class);	
}
