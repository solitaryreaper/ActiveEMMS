package models.service;

import java.util.List;

import com.walmartlabs.productgenome.rulegenerator.algos.Learner;

import play.mvc.Controller;

/**
 * Service class to persist state and common objects across different HTTP requests.
 * 
 * @author excelsior
 *
 */
public class CacheService extends Controller
{
	public static List<String> getDatasetAttributes()
	{
		
	}
	
	public static void clearCache()
	{
		session().clear();
	}
	
	public static boolean isTrainPhase()
	{
		
	}

	public static boolean isTestingPhaseDone()
	{
		
	}
	
	public static boolean isTrainingPhaseDone()
	{
		
	}
	
	public static Learner getMatcher()
	{
		
	}
}
