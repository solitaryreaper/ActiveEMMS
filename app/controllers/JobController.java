package controllers;

import static play.data.Form.form;

import java.io.File;
import java.util.List;

import models.Constants;
import models.Job;
import models.Project;
import models.service.DBService;
import play.Logger;
import play.cache.Cache;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import views.html.job_matching_results;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.walmartlabs.productgenome.rulegenerator.EMMSWorkflowDriver;
import com.walmartlabs.productgenome.rulegenerator.config.JobMetadata;
import com.walmartlabs.productgenome.rulegenerator.model.analysis.DatasetEvaluationSummary;
import com.walmartlabs.productgenome.rulegenerator.model.analysis.JobEvaluationSummary;
import com.walmartlabs.productgenome.rulegenerator.model.analysis.RuleEvaluationSummary;
import com.walmartlabs.productgenome.rulegenerator.model.data.Dataset;
import com.walmartlabs.productgenome.rulegenerator.model.data.DatasetNormalizerMeta;
import com.walmartlabs.productgenome.rulegenerator.utils.parser.DataParser;
import com.walmartlabs.productgenome.rulegenerator.utils.parser.ItemDataParser;
import com.walmartlabs.productgenome.rulegenerator.utils.parser.ItemPairDataParser;

/**
 * Controller class to process the current entity matching job.
 * 
 * @author excelsior
 *
 */
public class JobController extends Controller {

	/**
	 * Job submission for passive learner implies using all training data to generate rules.
	 * For active learner, job submission triggers a loop where most informative examples are 
	 * successively labelled and then used for generation of matching rules.
	 */
    public static Result submitJob()
    {
    	DynamicForm dynamicForm = form().bindFromRequest();
    	Logger.info("Job for parameters : " + dynamicForm.data().toString());
    	
    	boolean isActiveLearner = dynamicForm.get(Constants.PARAM_LEARNING_METHOD).equals(Constants.ACTIVE_LEARNER);
    	boolean isItemPairFormat = dynamicForm.get(Constants.PARAM_DATA_FORMAT).equals(Constants.ITEM_PAIR_FILE_FORMAT);

    	JobMetadata jobMeta = getJobMetadata(dynamicForm);
    	Job job = new Job(jobMeta.getJobName(), jobMeta.getDescription(), jobMeta.getDatasetName());
    	job.project = (Project) Cache.get(Constants.CACHE_PROJECT);
    	job.save();
    	Cache.set(Constants.CACHE_JOB, job);
    	
    	if(isActiveLearner) {
    		return invokeActiveLearner(jobMeta, isItemPairFormat);
    	}
    	else {
    		return invokePassiveLearner(jobMeta);
    	}
    }
    
    private static JobMetadata getJobMetadata(DynamicForm dynamicForm)
    {
    	boolean isItemPairFormat = dynamicForm.get(Constants.PARAM_DATA_FORMAT).equals(Constants.ITEM_PAIR_FILE_FORMAT);

    	JobMetadata jobMeta = new JobMetadata();
    	jobMeta.setJobName(dynamicForm.get(Constants.PARAM_JOB_NAME));
    	jobMeta.setDescription(dynamicForm.get(Constants.PARAM_JOB_DESCRIPTION));
    	jobMeta.setDatasetName(dynamicForm.get(Constants.PARAM_DATASET_NAME));
    	jobMeta.setAttributesToEvaluate(dynamicForm.get(Constants.PARAM_ATTRIBUTES_TO_EVALUATE));
    	
    	MultipartFormData body = request().body().asMultipartFormData();
    	if(!isItemPairFormat) {
    		FilePart srcFilePath = body.getFile(Constants.PARAM_SOURCE_FILE_PATH);
    		FilePart tgtFilePath = body.getFile(Constants.PARAM_TARGET_FILE_PATH);

        	jobMeta.setSourceFile(srcFilePath.getFile().getAbsolutePath());
        	jobMeta.setTargetFile(tgtFilePath.getFile().getAbsolutePath());
    	}
    	else {
    		FilePart itemPairFilePath = body.getFile(Constants.PARAM_ITEM_PAIR_FILE_PATH);
    		jobMeta.setItemPairFile(itemPairFilePath.getFile().getAbsolutePath());
    	}
    	
		FilePart goldFilePath = body.getFile(Constants.PARAM_GOLD_FILE_PATH);
    	jobMeta.setGoldFile(goldFilePath.getFile().getAbsolutePath());

    	jobMeta.setDesiredPrecision(dynamicForm.get(Constants.PARAM_PRECISION));
    	jobMeta.setDesiredCoverage(dynamicForm.get(Constants.PARAM_COVERAGE));
    	
    	return jobMeta;
    }
    
    /**
     * Invokes an active learning loop to get the data successively labelled and generate matching
     * rules using this labelled data.
     */
    private static Result invokeActiveLearner(JobMetadata jobMeta, boolean isItemPairFormat)
    {
    	Logger.info("Starting active learning ..");
    	Dataset dataset = null;
    	DataParser parser = null;
    	
		BiMap<String, String> schemaMap = getSchemaMap(jobMeta.getAttributesToEvaluate());
		DatasetNormalizerMeta normalizerMeta = new DatasetNormalizerMeta(schemaMap);
		
		String datasetName = jobMeta.getDatasetName();
		File goldFile = new File(jobMeta.getGoldFile());
    	if(isItemPairFormat) {
    		File itemPairFile = new File(jobMeta.getItemPairFile());
    		
			parser = new ItemPairDataParser();
			dataset = parser.parseData(datasetName, itemPairFile, goldFile, normalizerMeta);
    	}
    	else {
			File srcFile = new File(jobMeta.getSourceFile());
			File tgtFile = new File(jobMeta.getTargetFile());
			
			parser = new ItemDataParser();
			dataset = parser.parseData(datasetName, srcFile, tgtFile, goldFile, normalizerMeta);    		
    	}
    	
    	Cache.set(Constants.CACHE_DATASET_ATTRIBUTES, dataset.getAttributes());
    	
    	Logger.info("Found " + dataset.getItemPairs().size() + " itempairs ..");    	
    	DBService.loadDataset(dataset);

    	return ItemPairLabelController.labelItemPair();
    }
    
	private static BiMap<String, String> getSchemaMap(List<String> attributesToEvaluate)
	{
		BiMap<String, String> schemaMap = HashBiMap.create();
		for(String attribute : attributesToEvaluate) {
			schemaMap.put(attribute, attribute);
		}
		
		return schemaMap;
	}
	
    /**
     * Invokes passive rule learning algorithm on the train data and generates rules satisfying
     * the precision and recall constraints.
     */
    private static Result invokePassiveLearner(JobMetadata jobMeta)
    {
    	EMMSWorkflowDriver jobDriver = new EMMSWorkflowDriver();
    	JobEvaluationSummary matchRunResults = jobDriver.runEntityMatching(jobMeta);
    	DatasetEvaluationSummary trainPhaseSummary = matchRunResults.getTrainPhaseSumary();
    	DatasetEvaluationSummary tunePhaseSummary = matchRunResults.getTunePhaseSummary();
    	DatasetEvaluationSummary testPhaseSummary = matchRunResults.getTestPhaseSummary();
    	
    	List<RuleEvaluationSummary> topNRules = testPhaseSummary.getRankedRuleSummaries(testPhaseSummary.getRuleSummary());
    	
    	return ok(job_matching_results.render(trainPhaseSummary, tunePhaseSummary, testPhaseSummary, topNRules));    	
    }
}
