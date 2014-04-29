package controllers;

import static play.data.Form.form;

import java.util.List;

import models.Constants;

import com.walmartlabs.productgenome.rulegenerator.EMMSWorkflowDriver;
import com.walmartlabs.productgenome.rulegenerator.config.JobMetadata;
import com.walmartlabs.productgenome.rulegenerator.model.analysis.DatasetEvaluationSummary;
import com.walmartlabs.productgenome.rulegenerator.model.analysis.JobEvaluationSummary;
import com.walmartlabs.productgenome.rulegenerator.model.analysis.RuleEvaluationSummary;
import com.walmartlabs.productgenome.rulegenerator.model.data.Dataset;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import views.html.job_matching_results;

/**
 * Controller classe to process the current entity matching job.
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
    	DynamicForm form = form().bindFromRequest();
    	Logger.info("PARAMETERS : " + form.data().toString());
    	
    	boolean isActiveLearner = form.get(Constants.PARAM_LEARNING_METHOD).equals(Constants.ACTIVE_LEARNER);
    	boolean isItemPairFormat = form.get(Constants.PARAM_DATA_FORMAT).equals(Constants.ITEM_PAIR_FILE_FORMAT);
    	
    	JobMetadata jobMeta = new JobMetadata();
    	jobMeta.setDatasetName(form.get(Constants.PARAM_DATASET_NAME));
    	jobMeta.setAttributesToEvaluate(form.get(Constants.PARAM_ATTRIBUTES_TO_EVALUATE));
    	
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

    	jobMeta.setDesiredPrecision(form.get(Constants.PARAM_PRECISION));
    	jobMeta.setDesiredCoverage(form.get(Constants.PARAM_COVERAGE));
    	
    	if(isActiveLearner) {
    		return invokeActiveLearner(jobMeta, isItemPairFormat);
    	}
    	else {
    		return invokePassiveLearner(jobMeta);
    	}
    }
    
    /**
     * Invokes an active learning loop to get the data successively labelled and generate matching
     * rules using this labelled data.
     */
    public static Result invokeActiveLearner(JobMetadata jobMeta, boolean isItemPairFormat)
    {
    	Dataset dataset = null;
    	return ok();
    	/*
    	if(isItemPairFormat) {
    		
    	}
    	else {
    		
    	}
    	*/
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
