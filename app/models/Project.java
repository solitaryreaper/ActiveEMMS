package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="project")
public class Project extends Model {

	private static final long serialVersionUID = -7232015476850510328L;

	@Id
	public Long id;
	
	public String name;
	public String description;
	
	@OneToMany(mappedBy="ptoject")
	public List<Job> jobs;
}
