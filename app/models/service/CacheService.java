package models.service;

import java.util.List;

import models.Constants;
import play.Logger;
import play.cache.Cache;
import play.mvc.Controller;
import weka.core.Attribute;

import com.walmartlabs.productgenome.rulegenerator.algos.Learner;

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
		Cache.remove(Constants.CACHE_ITERATION_COUNTER);
	}
	
	public static void initializeActiveLearnerCache()
	{
    	Cache.set(Constants.CACHE_ITEMPAIRS_LABELLED, 0);
    	Cache.set(Constants.CACHE_ITERATION_COUNTER, 1);
	}
	
	public static boolean isTrainPhase()
	{
		boolean isTrainPhase = true;
		Integer iterationCount = (Integer) Cache.get(Constants.CACHE_ITERATION_COUNTER);
		if(iterationCount != null) {
			if(iterationCount > Constants.NUM_TRAIN_ITERATIONS) {
				isTrainPhase = false;
				Logger.info("Training phase completed ..");
			}
		}
		
		return isTrainPhase;
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
