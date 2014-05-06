package models.service;

import java.util.List;

import models.Constants;

import com.walmartlabs.productgenome.rulegenerator.algos.Learner;

import play.mvc.Controller;
import play.cache.*;

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
	
	public static void clearCache()
	{
		session().clear();
		Cache.remove(Constants.CACHE_PROJECT);
		Cache.remove(Constants.CACHE_JOB);
		
		Cache.remove(Constants.CACHE_DATASET_ATTRIBUTES);
		Cache.remove(Constants.CACHE_MATCHER);
	}
	
	public static boolean isTrainPhase()
	{
		boolean isTrainPhase = true;
		Integer iterationCount = (Integer) Cache.get(Constants.CACHE_ITERATION_COUNTER);
		if(iterationCount != null) {
			if(iterationCount >= Constants.NUM_TRAIN_ITERATIONS) {
				isTrainPhase = false;
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
