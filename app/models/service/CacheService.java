package models.service;

import java.util.ArrayList;
import java.util.List;

import models.Constants;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import weka.core.Attribute;

import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.model.rule.Rule;

/**
 * Service class to persist state and common objects across different HTTP requests.
 * 
 * @author excelsior
 *
 */
public class CacheService extends Controller
{
	@SuppressWarnings("unchecked")
	public static List<String> getDatasetAttributes()
	{
		return (List<String>) Cache.get(Constants.CACHE_DATASET_ATTRIBUTES);
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<Attribute> getDatasetFeatures()
	{
		return (ArrayList<Attribute>) Cache.get(Constants.CACHE_DATASET_FEATURES);
	}
	
	@SuppressWarnings("unchecked")
	public static List<Rule> getRules()
	{
		return (List<Rule>)Cache.get(Constants.CACHE_RULES);
	}
	
	@SuppressWarnings("unchecked")
	public static List<Attribute> getInstanceFeatures()
	{
		return (List<Attribute>) Cache.get(Constants.CACHE_DATASET_FEATURES);
	}
	
	public static void clearCache()
	{
		session().clear();
		Cache.remove(Constants.CACHE_PROJECT);
		Cache.remove(Constants.CACHE_JOB);
		
		Cache.remove(Constants.CACHE_DATASET_ATTRIBUTES);
		Cache.remove(Constants.CACHE_DATASET_FEATURES);
		Cache.remove(Constants.CACHE_MATCHER);
		Cache.remove(Constants.CACHE_BEST_ITEMPAIRS);
		Cache.remove(Constants.CACHE_ITEMPAIRS_LABELLED);
		Cache.remove(Constants.CACHE_TRAIN_ITERATION_COUNTER);
		Cache.remove(Constants.CACHE_TEST_ITEMPAIRS_LABELLED);
		Cache.remove(Constants.CACHE_RULES);
		Cache.remove(Constants.CACHE_PHASE);
	}
	
	public static void initializeActiveLearnerCache()
	{
    	Cache.set(Constants.CACHE_ITEMPAIRS_LABELLED, 0);
    	Cache.set(Constants.CACHE_TEST_ITEMPAIRS_LABELLED, 0);
    	Cache.set(Constants.CACHE_TRAIN_ITERATION_COUNTER, 1);
    	Cache.set(Constants.CACHE_PHASE, "TRAIN");
	}
	
	public static boolean isTrainPhase()
	{
		boolean isTrainPhase = true;
		Integer iterationCount = (Integer) Cache.get(Constants.CACHE_TRAIN_ITERATION_COUNTER);
		if(iterationCount != null) {
			if(iterationCount > Constants.NUM_TRAIN_ITERATIONS) {
				isTrainPhase = false;
				Logger.info("Training phase completed ..");
			}
		}
		
		return isTrainPhase;
	}
	
	public static boolean isTestPhase()
	{
		return Cache.get(Constants.CACHE_PHASE).equals("TEST");
	}
	
	public static boolean isTestPhaseDone()
	{
		if(!isTrainingPhaseDone()) {
			return false;
		}
		
		boolean isTestPhaseDone = false;
		Integer testItemPairsLabelled = (Integer) Cache.get(Constants.CACHE_TEST_ITEMPAIRS_LABELLED);
		if(testItemPairsLabelled != null && testItemPairsLabelled >= Constants.NUM_ITEMPAIRS_TO_LABEL_IN_TEST_PHASE) {
			isTestPhaseDone = true;
			Logger.info("Test Phase completed ..");
		}
		
		return isTestPhaseDone;
	}
	
	public static boolean isTrainingPhaseDone()
	{
		return !isTrainPhase();
	}
	
	public static Learner getMatcher()
	{
		return (Learner) Cache.get(Constants.CACHE_MATCHER);
	}
}
