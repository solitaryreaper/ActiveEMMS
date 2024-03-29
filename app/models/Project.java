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
	
	@OneToMany(mappedBy="project")
	public List<Job> jobs;
	
	public Project(String name, String description)
	{
		this.name = name;
		this.description = description;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Project [name=").append(name).append(", description=")
				.append(description).append("]");
		return builder.toString();
	}


	/**
     * Generic query helper for entity Project with id Long
     */
    public static Finder<Long,Project> find = new Finder<Long,Project>(Long.class, Project.class);	
}
