package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import play.db.ebean.Model;

@Entity
@Table(name="job")
public class Job extends Model {

	private static final long serialVersionUID = 1503735325440578992L;

	private enum JobStatus
	{
		INITIALIZED("initialized"),
		FAILED("failed"),
		PARTIAL_SUCCESS("partial_success"),
		SUCCESS("success");
		
		private String jobStatus;
		
		private JobStatus(String status)
		{
			this.jobStatus = status;
		}
		
		public String getJobStatus()
		{
			return this.jobStatus;
		}
		
		public JobStatus getStatusEnum(String status)
		{
			JobStatus result = null;
			for(JobStatus statusEnum : JobStatus.values()) {
				if(statusEnum.getJobStatus().equals(status)) {
					result = statusEnum;
					break;
				}
			}
			
			return result;
		}
	}
	
	@Id
	public Long id;
	
	public String name;
	public String description;
	public String datasetName;
	
	@ManyToOne
	public Project project;
	
	@OneToMany(mappedBy="job")
	public List<Rule> rules;
	@OneToMany(mappedBy="job")
	public List<ItemPairGoldData> itemPairGoldData;
	@OneToMany(mappedBy="job")
	public List<ItemData> itemData;
	
	public JobStatus status = JobStatus.INITIALIZED;
	
	
}
