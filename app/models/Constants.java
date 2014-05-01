package models;

public class Constants {
	
	// Form parameters
	public static final String PARAM_JOB_NAME = "job_name" ;
	public static final String PARAM_DATASET_NAME = "dataset_name" ;
	public static final String PARAM_ATTRIBUTES_TO_EVALUATE = "attributes_to_evaluate" ;
	public static final String PARAM_LEARNER = "learner";
	public static final String PARAM_SOURCE_FILE_PATH = "source_data_file_path";
	public static final String PARAM_TARGET_FILE_PATH = "target_data_file_path";
	public static final String PARAM_ITEM_PAIR_FILE_PATH = "item_pair_file_path";
	public static final String PARAM_GOLD_FILE_PATH = "gold_data_file_path";
	public static final String PARAM_PRECISION = "precision";
	public static final String PARAM_COVERAGE = "coverage";
	public static final String PARAM_LEARNING_METHOD = "learning_type";
	
	public static final String ACTIVE_LEARNER = "active";
	public static final String PASSIVE_LEARNER = "passive";
	
	public static final String PARAM_DATA_FORMAT = "dataset_format";
	public static final String ITEM_FILE_FORMAT = "item_file_format";
	public static final String ITEM_PAIR_FILE_FORMAT = "itempair_file_format";
	
	public static final String DEFAULT_USER = "test_user";
	public static final int DEFAULT_USER_ID = 1;
	
	public static final String DB_PROJECT_TABLE = "emms.project";
	public static final String DB_JOB_TABLE = "emms.job";
	public static final String DB_USER_TABLE = "emms.user";
	public static final String DB_PROJECT_TO_JOBS_TABLE = "emms.project_to_jobs_map";
}
