package controllers;

import static play.data.Form.form;

import java.util.ArrayList;
import java.util.List;

import models.Constants;
import models.ItemPairGoldData;
import models.Job;
import models.service.CacheService;
import models.service.DBService;
import models.utils.DBUtils;
import play.Logger;
import play.cache.Cache;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.itempair_label;
import views.html.rules_display;
import weka.core.Attribute;
import weka.core.Instances;

import com.walmartlabs.productgenome.rulegenerator.algos.RandomForestLearner;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;
import com.walmartlabs.productgenome.rulegenerator.model.rule.Rule;

/**
 * Controller class that handles labelling of itempair in an active learning setting.
 * 
 * @author excelsior
 *
 */
public class ItemPairLabelController extends Controller
{
	/**
	 * Displays an appropriate itempair for the crowd to label to generate matching rules .. 
	 */
	@SuppressWarnings("unchecked")
	public static Result labelItemPair()
	{
    	// If training phase is done, display the rules learnt
		if(CacheService.isTrainingPhaseDone()) {
			Job job = (Job) Cache.get(Constants.CACHE_JOB);
    		Logger.info("Training Phase completed for job " + job.id);
    		
    		ArrayList<Attribute> features = (ArrayList<Attribute>) Cache.get(Constants.CACHE_DATASET_FEATURES);    		
    		Instances labelledData = DBUtils.getLabelledInstances(job.id, features);
    		Logger.info("Found " + labelledData.numInstances() + " labelled instances ..");
    		DBService.learnMatcher(labelledData);
    		
    		RandomForestLearner learner = (RandomForestLearner)CacheService.getMatcher();
    		List<Rule> rules = learner.getMatchingRules();
    		
    		Logger.info("Showing the learnt rules " + rules.size() + " ..");
    		for(Rule rule : rules) {
    			Logger.info(rule.toString());
    		}
    		return ok(rules_display.render(rules));
    	}
    	
		Job job = (Job) Cache.get(Constants.CACHE_JOB);
		Long jobId = job.id;
		List<String> attributes = CacheService.getDatasetAttributes();
		ItemPair pair = null;
		boolean isTrainPhase = CacheService.isTrainPhase();
		if(isTrainPhase) {
			// Retrieve the unlabelled itempair with most entropy for labelling
			pair = DBService.getRandomItemPairToLabel(jobId);
			//pair = DBService.getBestItemPairToLabel(jobId);
    	}
    	else {
    		// Retrieve any random unlabelled itempair for labelling
    		pair = DBService.getRandomItemPairToLabel(jobId);
    	}
    	
		int numItemPairsLabelled = 1;//(Integer) Cache.get(Constants.CACHE_ITEMPAIRS_LABELLED);
		String datasetName = "Restaurant";
		
    	// Ask user to label the current itempair
    	return ok(itempair_label.render(datasetName, attributes, pair, isTrainPhase, numItemPairsLabelled));
	}
	
	/**
	 * Saves the itempair labelled by the crowd in active learning setting .. 
	 */
	public static Result saveItemPairLabel()
	{
    	DynamicForm dynamicForm = form().bindFromRequest();
    	Logger.info("Params : " + dynamicForm.data().toString());
    	
    	String item1Id = dynamicForm.get(Constants.PARAM_ITEM1_ID);
    	String item2Id = dynamicForm.get(Constants.PARAM_ITEM2_ID);
    	MatchStatus matchStatus = 
			MatchStatus.getMatchStatus(dynamicForm.get(Constants.PARAM_MATCH_STATUS));
    	
    	// Save this labelled data in database for building subsequent matching models.
    	ItemPairGoldData goldData = new ItemPairGoldData(item1Id, item2Id, matchStatus);
    	Job job = (Job) Cache.get(Constants.CACHE_JOB);
    	goldData.job = job;
    	if(!CacheService.isTrainPhase()) {
    		goldData.isLabelledInTrainPhase = false;
    	}

    	Logger.info(item1Id + ", " + item2Id + ", " + matchStatus.toString() + ", " + goldData.itemPairId);
    	Logger.info("Saving gold data item pair ..");
    	goldData.save();

    	int numItemPairsLabelled = (Integer) Cache.get(Constants.CACHE_ITEMPAIRS_LABELLED) + 1;
    	Cache.set(Constants.CACHE_ITEMPAIRS_LABELLED, numItemPairsLabelled);
    	if(numItemPairsLabelled % Constants.NUM_ITEMPAIRS_PER_ITERATION == 0) {
    		int iterationCount = (Integer)Cache.get(Constants.CACHE_ITERATION_COUNTER) + 1;
    		Cache.set(Constants.CACHE_ITERATION_COUNTER, iterationCount);
    	}
    	
    	// Once this itempair has been labelled, continue with labelling other itempairs.
    	return labelItemPair();
	}
}
