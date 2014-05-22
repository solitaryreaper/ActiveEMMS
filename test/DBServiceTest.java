import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.util.ArrayList;
import java.util.List;

import models.service.DBService;
import models.utils.DBUtils;

import org.junit.Test;

import play.Logger;
import weka.core.Attribute;
import weka.core.Instances;

import com.google.common.collect.Lists;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.algos.RandomForestLearner;
import com.walmartlabs.productgenome.rulegenerator.model.rule.Rule;

public class DBServiceTest {
	
	@Test
	public void testLearnModelUsingTrainData()
	{
		running(fakeApplication(), new Runnable() {
			public void run() {
				long jobId = 14l;
	    		Instances labelledData = DBUtils.getLabelledInstances(jobId, getFeatures());
	    		Logger.info("Found " + labelledData.numInstances() + " labelled instances ..");
	    		Learner learnerRaw = DBService.learnMatcher(labelledData);
	    		
	    		RandomForestLearner learner = (RandomForestLearner)learnerRaw;
	    		List<Rule> rules = learner.getMatchingRules();
	    		
	    		Logger.info("Showing the learnt rules " + rules.size() + " ..");
	    		for(Rule rule : rules) {
	    			Logger.info(rule.toString());
	    		}

			}
		});		
	}
	
	private static ArrayList<Attribute> getFeatures()
	{
		ArrayList<Attribute> features = new ArrayList<Attribute>(5);
		
		features.add(new Attribute("name_cosine"));
	    features.add(new Attribute("name_euclidean"));
		features.add(new Attribute("name_exact_string"));
		features.add(new Attribute("name_jaro_winkler"));
		features.add(new Attribute("name_lev"));
		features.add(new Attribute("name_smith_waterman"));
		features.add(new Attribute("addr_cosine"));
		features.add(new Attribute("addr_euclidean"));
		features.add(new Attribute("addr_exact_string"));
		features.add(new Attribute("addr_jaro_winkler"));
		features.add(new Attribute("addr_lev"));
		features.add(new Attribute("addr_smith_waterman"));
		features.add(new Attribute("type_cosine"));
		features.add(new Attribute("type_euclidean"));
		features.add(new Attribute("type_exact_string"));
		features.add(new Attribute("type_jaro"));
		features.add(new Attribute("type_jaro_winkler"));
		features.add(new Attribute("type_lev"));
		features.add(new Attribute("type_smith_waterman"));
		features.add(new Attribute("city_cosine"));
		features.add(new Attribute("city_euclidean"));
		features.add(new Attribute("city_exact_string"));
		features.add(new Attribute("city_jaro"));
		features.add(new Attribute("city_jaro_winkler"));
		features.add(new Attribute("city_lev"));
		features.add(new Attribute("city_smith_waterman"));
		
		List<String> classVal = Lists.newArrayList();
		classVal.add("match");
		classVal.add("mismatch");
		classVal.add("unknown");
		features.add(new Attribute("class",classVal));
		
		return features;
	}
}
