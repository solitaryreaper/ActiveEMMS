
package models.service;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import models.Constants;
import models.FeatureData;
import models.ItemData;
import models.ItemPairGoldData;
import models.Job;
import models.utils.DBUtils;
import models.utils.FeatureUtils;
import play.Logger;
import play.cache.Cache;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlRow;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.algos.RandomForestLearner;
import com.walmartlabs.productgenome.rulegenerator.model.data.Dataset;
import com.walmartlabs.productgenome.rulegenerator.model.data.Item;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;
import com.walmartlabs.productgenome.rulegenerator.model.rule.Rule;
import com.walmartlabs.productgenome.rulegenerator.utils.WekaUtils;

public class DBService 
{
	/**
	 * Loads the dataset into database after converting it in the proper db format.
	 * 
	 * This includes :
	 * 1) Loading the original text data (attribute key-value pairs) into db.
	 * 2) Loading the feature dataset into db.
	 * 
	 * @param dataset
	 */
	public static void loadDataset(Dataset dataset)
	{
		Logger.info("Loading the dataset into database ..");
		Job job = (Job) Cache.get(Constants.CACHE_JOB);
		int matchedItemPairs = 0;
		int uniqueItemSourceA = 0;
		int uniqueItemSourceB = 0;
		
		// TODO : Only persist unique items in database. Adding unnecessary overhead ..
		for(ItemPair itemPair : dataset.getItemPairs()) {
			Item itemA = itemPair.getItemA();
			Item itemB = itemPair.getItemB();
			boolean isMatch = itemPair.getMatchStatus().equals(MatchStatus.MATCH);
			
			if(isMatch) {
				ItemPairGoldData goldData = new ItemPairGoldData(itemPair.getId(), itemA.getId(), itemB.getId(), MatchStatus.MATCH);
				goldData.job = job;
				goldData.save();
				
				++matchedItemPairs;
			}
			
			// Persist item from first source
			for(Map.Entry<String, String> entry : itemA.getAttrMap().entrySet()) {
				ItemData data = new ItemData(Constants.DATA_SOURCE1_ID, itemPair.getId(), itemA.getId(), entry.getKey(), entry.getValue());
				data.job = job;
				data.save();
				++uniqueItemSourceA;
			}				

			// Persist item from second source
			for(Map.Entry<String, String> entry : itemB.getAttrMap().entrySet()) {
				ItemData data = new ItemData(Constants.DATA_SOURCE2_ID, itemPair.getId(), itemB.getId(), entry.getKey(), entry.getValue());
				data.job = job;
				data.save();
				++uniqueItemSourceB;
			}
			
		}
		
		Logger.info("Persisted " + matchedItemPairs + " matched itempairs in db ..");
		Logger.info("Persisted " + uniqueItemSourceA + " unique source A itempairs in db ..");
		Logger.info("Persisted " + uniqueItemSourceB + " unique source B itempairs in db ..");
		
		Logger.info("Persisting features in database ..");
		loadFeaturesInDB(dataset, job);
	}
	
	/**
	 * Extracts the features from the original data and persists in the database.
	 */
	@SuppressWarnings("unchecked")
	private static boolean loadFeaturesInDB(Dataset dataset, Job job)
	{
		boolean areFeaturesPersisted = true;
		Instances wekaInstances = WekaUtils.getWekaInstances(dataset);
		
		// Save the features in cache. This will be used to create weka instances on fly later ..
		Enumeration<Attribute> features = wekaInstances.enumerateAttributes();
		List<Attribute> wekaInstFeatures = Lists.newArrayList();
		while(features.hasMoreElements()) {
			wekaInstFeatures.add((Attribute)features.nextElement());
		}
		wekaInstFeatures.add(FeatureUtils.getClassAttribute());
		Cache.set(Constants.CACHE_DATASET_FEATURES, wekaInstFeatures);
		Logger.info("## Features : " + wekaInstFeatures.toString());
		
		for(Instance instance : wekaInstances) {
			int itemPairId = (int)instance.value(Constants.WEKA_INSTANCE_ITEMPAIR_ATTRIBUTE_ID);
			
			ArrayList<Attribute> featuresList = (ArrayList<Attribute>)Cache.get(Constants.CACHE_DATASET_FEATURES);
			List<FeatureData> itemPairFeatures = getFeatures(itemPairId, instance, featuresList, job);
			persistItemPairFeatures(itemPairFeatures);
		}
		
		return areFeaturesPersisted;
	}
	
	private static List<FeatureData> getFeatures(int itemPairId, Instance instance, ArrayList<Attribute> features, Job job)
	{
		List<FeatureData> featureDataList = Lists.newArrayList();
		
		// Hack : Skip first attribute which represents the itempair id and also last attribute which represents the classification label ..
		for(int i=1; i < (features.size() - 1) ; i++) {
			double featureValue = instance.value(i);
			String featureName = features.get(i).name();
			FeatureData featData = new FeatureData(itemPairId, featureName, featureValue);
			featData.job = job;
			featureDataList.add(featData);
		}
		
		return featureDataList;
	}
	
	private static void persistItemPairFeatures(List<FeatureData> itemPairFeatures)
	{
		for(FeatureData featureData : itemPairFeatures) {
			try {
				featureData.save();
			}
			catch(Exception e) {
				Logger.error("Failed to save feature : " + featureData.toString());
			}
			
		}
	}
	
	/**
	 * Returns the most informative unlabelled itempair whose labelling would add most information to the 
	 * existing learned model.
	 */
	@SuppressWarnings("unchecked")
	public static ItemPair getBestItemPairToLabel(Long jobId)
	{
		ItemPair mostInfoItemPair = null;
		List<ItemPair> mostInfoItemPairs = (List<ItemPair>) Cache.get(Constants.CACHE_BEST_ITEMPAIRS);
		
		// Check if any informative itempairs from last lot is still left to be labelled. If no,
		// fetch another lot of most informative itempairs from the remaining unlabelled itempairs
		// in the dataset.
		if(mostInfoItemPairs == null || mostInfoItemPairs.isEmpty()) {
			Job currJob = (Job) Cache.get(Constants.CACHE_JOB);
			ArrayList<Attribute> features = (ArrayList<Attribute>) Cache.get(Constants.CACHE_DATASET_FEATURES);			
			mostInfoItemPairs = getTopKMostInformativeItemPairs(currJob, features);
		}

		mostInfoItemPair = mostInfoItemPairs.remove(0);
		Cache.set(Constants.CACHE_BEST_ITEMPAIRS, mostInfoItemPairs);
		return mostInfoItemPair;
	}
	
	private static List<ItemPair> getTopKMostInformativeItemPairs(Job currJob, ArrayList<Attribute> attributes)
	{
		// Learn a matcher using current labelled data
		Instances labelledInstances = DBUtils.getLabelledInstances(currJob.id, attributes);
		Learner learner = learnMatcher(labelledInstances);
		
		// Find the itempairs with most information using unlabelled data and the current matcher
		Map<Integer, Instance> itemPairIdInstanceMap = DBUtils.getUnlabelledInstances(currJob.id, attributes);
		Map<Integer, Instance> topKEntropyInstances = DBUtils.getTopKEntropyInstances(learner, 
				itemPairIdInstanceMap, Constants.NUM_ITEMPAIRS_PER_ITERATION);
		
		List<ItemPair> topKEntropyItemPairs = Lists.newArrayList();
		for(Map.Entry<Integer, Instance> entry : topKEntropyInstances.entrySet()) {
			ItemPair itemPair = DBUtils.getItemPairById(entry.getKey(), currJob, MatchStatus.UNKNOWN);
			topKEntropyItemPairs.add(itemPair);
		}
		
		return topKEntropyItemPairs;
	}
	
	// Learn a matcher using current labelled data
	public static Learner learnMatcher(Instances labelledData)
	{
		Learner learner = new RandomForestLearner();
		List<Rule> rules = learner.learnRules(labelledData);
		Cache.set(Constants.CACHE_MATCHER, learner);
		Logger.info("Rules found : " + rules.size());
		
		RandomForestLearner rf = (RandomForestLearner)learner;
		Logger.info("Learnt " + rf.getRules().size() + " rules ..");
		return learner;
	}
	
	public static Learner getLearnerUsingLabelledData(Dataset labelledData)
	{
		Logger.info("Dataset : " + labelledData.getItemPairs().size());
		Logger.info("Attributes : " + labelledData.getAttributes());
		
		Instances wekaInstances = WekaUtils.getWekaInstances(labelledData);
		Logger.info("Found " + wekaInstances.numInstances() + " weka instances for labelled dataset ..");
		Learner learner = new RandomForestLearner();
		learner.learnRules(wekaInstances);
		return learner;
	}
	
	/**
	 * Returns a random unlabelled itempair which has to be labelled.
	 */
	public static ItemPair getRandomItemPairToLabel(Long jobId)
	{
		Logger.info("Finding random item pair to label ..");
		String randomItem1FetchSQL = 
			"select distinct data.item_id, data.attribute, data.value "
			+ "from item_data data join (select item_id from item_data where datasource_id = 1 AND job_id = " + jobId + " order by rand() limit 1) src1 "
			+ "on (data.item_id = src1.item_id) WHERE data.job_id = " + jobId;
		Logger.error(randomItem1FetchSQL);
		
		String randomItem2FetchSQL =
			"select distinct data.item_id, data.attribute, data.value from item_data data join "
			+ "(select item_id from item_data where datasource_id = 2 AND job_id = " + jobId + " order by rand() limit 1) src2 "
			+ "on (data.item_id = src2.item_id) WHERE data.job_id = " + jobId;
		Logger.error(randomItem2FetchSQL);

		List<SqlRow> item1Results = Ebean.createSqlQuery(randomItem1FetchSQL).findList();
		List<SqlRow> item2Results = Ebean.createSqlQuery(randomItem2FetchSQL).findList();
		
		Item itemA = getItem(item1Results);
		Item itemB = getItem(item2Results);
		MatchStatus matchStatus = getItemPairMatchStatus(itemA.getId(), itemB.getId(), jobId);
		
		return new ItemPair(itemA, itemB, matchStatus);
	}
	
	private static MatchStatus getItemPairMatchStatus(String item1Id, String item2Id, Long jobId)
	{
		MatchStatus status = MatchStatus.UNKNOWN;
		List<ItemPairGoldData> result1 = ItemPairGoldData.find.where().eq("item1id", item1Id).eq("item2id", item2Id).eq("job_id", jobId).findList();
		if(!(result1 == null || result1.isEmpty())) {
			status = result1.get(0).matchStatus;
		}
		else {
			List<ItemPairGoldData> result2 = ItemPairGoldData.find.where().eq("item2id", item1Id).eq("item1id", item2Id).eq("job_id", jobId).findList();
			if(!(result2 == null || result2.isEmpty())) {
				status = result2.get(0).matchStatus;	
			}
			
		}

		return status;
	}
	
	private static Item getItem(List<SqlRow> results)
	{
		String itemId = null;
		Map<String, String> itemAttrMap = Maps.newHashMap();

		for(SqlRow result : results) {
			itemId = result.getString("item_id");
			
			String attribute = result.getString("attribute");
			String value = result.getString("value");
			itemAttrMap.put(attribute, value);
		}
		
		return new Item(itemId, itemAttrMap);
	}
}
