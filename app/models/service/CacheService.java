package models.service;

import java.util.List;

import javax.swing.text.html.HTML.Attribute;

import models.Constants;

import com.google.common.collect.Lists;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;

import play.Logger;
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
		List<String> attributes = (List<String>) Cache.get(Constants.CACHE_DATASET_ATTRIBUTES);
		if(attributes == null || attributes.isEmpty()) {
			attributes = Lists.newArrayList();
			attributes.add("name");
			attributes.add("addr");
			attributes.add("city");
			attributes.add("type");
			
			Cache.set(Constants.CACHE_DATASET_ATTRIBUTES, attributes);
		}
		
		return attributes;
	}
	
	public static void clearCache()
	{
		session().clear();
		Cache.remove(Constants.CACHE_PROJECT);
		Cache.remove(Constants.CACHE_JOB);
		
		Cache.remove(Constants.CACHE_DATASET_ATTRIBUTES);
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
