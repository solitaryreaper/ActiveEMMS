package controllers;

import static play.data.Form.form;

import java.util.List;

import models.Constants;
import models.ItemPairGoldData;
import models.Job;
import models.service.CacheService;
import models.service.DBService;

import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.algos.RandomForestLearner;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;
import com.walmartlabs.productgenome.rulegenerator.model.rule.Rule;

import play.Logger;
import play.cache.Cache;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.itempair_label;
import weka.classifiers.trees.RandomForest;

/**
 * Controller class that handles labelling of itempair in an active learning setting.
 * 
 * @author excelsior
 *
 */
public class ItemPairLabelController extends Controller
{
	public static Result labelItemPair()
	{
    	// If training phase is done, display the rules learnt
		if(CacheService.isTrainingPhaseDone()) {
    		Logger.info("Training Phase completed ..");
    		RandomForestLearner learner = (RandomForestLearner)CacheService.getMatcher();
    		List<String> rules = learner.getRules();
    		return ok(rules.toString());
    	}
    	
		Job job = (Job) Cache.get(Constants.CACHE_JOB);
		Long jobId = job.id;
		//Long jobId = 3L;
		List<String> attributes = CacheService.getDatasetAttributes();
		ItemPair pair = null;
		boolean isTrainPhase = CacheService.isTrainPhase();
		if(isTrainPhase) {
			// Retrieve the unlabelled itempair with most entropy for labelling
			//pair = DBService.getRandomItemPairToLabel(jobId);
			pair = DBService.getBestItemPairToLabel(jobId);
    	}
    	else {
    		// Retrieve any random unlabelled itempair for labelling
    		pair = DBService.getRandomItemPairToLabel(jobId);
    	}
    	
		int numItemPairsLabelled = (Integer) Cache.get(Constants.CACHE_ITEMPAIRS_LABELLED);
    	// Ask user to label the current itempair
    	return ok(itempair_label.render(attributes, pair, isTrainPhase, numItemPairsLabelled));
	}
	
	public static Result saveItemPairLabel()
	{
    	DynamicForm dynamicForm = form().bindFromRequest();
    	Logger.info("Params : " + dynamicForm.data().toString());
    	
    	String item1Id = dynamicForm.get(Constants.PARAM_ITEM1_ID);
    	String item2Id = dynamicForm.get(Constants.PARAM_ITEM2_ID);
    	MatchStatus matchStatus = 
			MatchStatus.getMatchStatus(dynamicForm.get(Constants.PARAM_MATCH_STATUS));
    	
    	Logger.info(item1Id + ", " + item2Id + ", " + matchStatus.toString());
    	
    	// Save this labelled data in database for building subsequent matching models.
    	ItemPairGoldData goldData = new ItemPairGoldData(item1Id, item2Id, matchStatus);
    	Job job = (Job) Cache.get(Constants.CACHE_JOB);
    	goldData.job = job;
    	if(!CacheService.isTrainPhase()) {
    		goldData.isLabelledInTrainPhase = false;
    	}
    	
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
