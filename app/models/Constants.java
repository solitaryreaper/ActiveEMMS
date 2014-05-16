package models;

public class Constants {
	
	// Form parameters
	public static final String PARAM_JOB_NAME = "job_name" ;
	public static final String PARAM_JOB_DESCRIPTION = "job_desc" ;
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
	
	public static final String PARAM_ITEM1_ID = "item1Id";
	public static final String PARAM_ITEM2_ID = "item2Id";
	public static final String PARAM_MATCH_STATUS = "match_status";
	
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
	
	public static final String CACHE_PROJECT = "project";
	public static final String CACHE_JOB = "job";
	public static final String CACHE_DATASET_ATTRIBUTES = "attributes";
	public static final String CACHE_DATASET_FEATURES = "features";
	public static final String CACHE_MATCHER = "matcher";
	public static final String CACHE_ITERATION_COUNTER = "iteration";
	public static final String CACHE_BEST_ITEMPAIRS = "best_itempairs";
	public static final String CACHE_ITEMPAIRS_LABELLED = "labelled_examples";
	
	public static final int NUM_TRAIN_ITERATIONS = 1;
	public static final int NUM_ITEMPAIRS_PER_ITERATION = 10;
	
	public static final int DATA_SOURCE1_ID = 1;
	public static final int DATA_SOURCE2_ID = 2;
	
	public static final int WEKA_INSTANCE_ITEMPAIR_ATTRIBUTE_ID = 0;
}
