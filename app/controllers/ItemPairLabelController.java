package controllers;

import static play.data.Form.form;

import java.util.List;

import models.Constants;
import models.ItemPairGoldData;
import models.Job;
import models.service.CacheService;
import models.service.DBService;

import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;
import com.walmartlabs.productgenome.rulegenerator.model.rule.Rule;

import play.Logger;
import play.cache.Cache;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.itempair_label;

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
    		Learner learner = CacheService.getMatcher();
    		List<Rule> rules = null; // TODO
    		return ok(rules.toString());
    	}
    	
//		Job job = (Job) Cache.get(Constants.CACHE_JOB);
//		Long jobId = job.id;
		Long jobId = 1L;
		List<String> attributes = CacheService.getDatasetAttributes();
		ItemPair pair = null;
		boolean isTrainPhase = CacheService.isTrainPhase();
		if(isTrainPhase) {
			// Retrieve the unlabelled itempair with most entropy for labelling
			pair = DBService.getRandomItemPairToLabel(jobId);
			//pair = DBService.getBestItemPairToLabel();
    	}
    	else {
    		// Retrieve any random unlabelled itempair for labelling
    		pair = DBService.getRandomItemPairToLabel(jobId);
    	}
    	
    	// Ask user to label the current itempair
    	return ok(itempair_label.render(attributes, pair, isTrainPhase));
	}
	
	public static Result saveItemPairLabel()
	{
    	DynamicForm dynamicForm = form().bindFromRequest();
    	Logger.info("Params : " + dynamicForm.data().toString());
    	
    	String item1Id = dynamicForm.get(Constants.PARAM_ITEM1_ID);
    	String item2Id = dynamicForm.get(Constants.PARAM_ITEM2_ID);
    	MatchStatus matchStatus = MatchStatus.getMatchStatus(Constants.PARAM_MATCH_STATUS);
    	
    	Logger.info(item1Id + ", " + item2Id + ", " + matchStatus.toString());
    	// Save this labelled data in database for building subsequent matching models.
    	ItemPairGoldData goldData = new ItemPairGoldData(item1Id, item2Id, matchStatus);
    	//Job job = (Job) Cache.get(Constants.CACHE_JOB);
    	//goldData.job = job;
    	if(!CacheService.isTrainPhase()) {
    		goldData.isLabelledInTrainPhase = false;
    	}
    	
    	Logger.info("Saving gold data item pair ..");
    	goldData.save();
    	
    	// Once this itempair has been labelled, continue with labelling other itempairs.
    	return labelItemPair();
	}
}
