package models.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Constants;
import models.FeatureData;
import play.Logger;
import play.cache.Cache;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlRow;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

/**
 * A set of utility functions for helping in the data access and persistence to DB.
 * @author excelsior
 *
 */
public class DBUtils {

	/**
	 * Returns the item pair id corresponding to the weka instance.
	 * 
	 *  This itempair id can be used to locate the actual textual item pair corresponding to
	 *  the weka instance.
	 */
	public static Integer getItemPairIdWekaInstance(Instance instance)
	{
		return null; // TODO
	}
	
	/**
	 * Returns the itempair corresponding to the item pair id.
	 */
	public static ItemPair getItemPairById(int itemPairId)
	{
		return null; // TODO
	}
	
	public static Map<Integer, Instance> getTopKEntropyInstances(Learner learner, 
			Map<Integer, Instance> itemPairInstanceMap, int K)
	{
		return null; // TODO
	}
	
	public static Instances getLabelledInstances(long jobId)
	{
		ArrayList<Attribute> attributes = (ArrayList<Attribute>) Cache.get(Constants.CACHE_DATASET_FEATURES);
		attributes.add(getClassAttribute());
		Instances instances = new Instances("Labelled Data", attributes, 0);
		instances.setClassIndex(instances.numAttributes() - 1);
		
		String matchedItemPairsSQL = 
			" SELECT feature.item_pair_id, feature.feature_name, feature.feature_value"
			+ " FROM itempair_gold_data gold JOIN feature_data feature "
			+ " ON (gold.item_pair_id = feature.item_pair_id AND gold.job_id = feature.job_id)"
			+ " WHERE gold.match_status IN (0,1) ORDER BY feature.item_pair_id";
		List<SqlRow> matchedItemPairsRaw = Ebean.createSqlQuery(matchedItemPairsSQL).findList();
		if(!matchedItemPairsRaw.isEmpty()) {
			Map<Integer, List<FeatureData>> matchedItemPairs = getFeaturesByItemPair(matchedItemPairsRaw);
			for(Map.Entry<Integer, List<FeatureData>> entry : matchedItemPairs.entrySet()) {
				Instance instance = FeatureUtils.getInstance(entry.getValue(), attributes);
				instance.setValue(instances.numAttributes() - 1, "match");
				instance.setDataset(instances);
				instances.add(instance);
			}			
		}
		
		String mismatchedItemPairsSQL = 
				" SELECT feature.item_pair_id, feature.feature_name, feature.feature_value"
				+ " FROM itempair_gold_data gold JOIN feature_data feature "
				+ " ON (gold.item_pair_id = feature.item_pair_id AND gold.job_id = feature.job_id)"
				+ " WHERE gold.match_status IN (1) ORDER BY feature.item_pair_id";
			List<SqlRow> mismatchedItemPairsRaw = Ebean.createSqlQuery(matchedItemPairsSQL).findList();
			if(!mismatchedItemPairsRaw.isEmpty()) {
				Map<Integer, List<FeatureData>> mismatchedItemPairs = getFeaturesByItemPair(matchedItemPairsRaw);
				for(Map.Entry<Integer, List<FeatureData>> entry : mismatchedItemPairs.entrySet()) {
					Instance instance = FeatureUtils.getInstance(entry.getValue(), attributes);
					instance.setValue(instances.numAttributes() - 1, "mismatch");
					instance.setDataset(instances);
					instances.add(instance);
				}	
			}
	
		
		Logger.info("Found " + instances.numInstances() + " instances in the dataset ..");
		return instances;
	}
	
	private static Map<Integer, List<FeatureData>> getFeaturesByItemPair(List<SqlRow> features)
	{
		Map<Integer, List<FeatureData>> featuresByItemPair = Maps.newHashMap();
		for(SqlRow row : features) {
			Integer itemPairId = row.getInteger("item_pair_id");
			List<FeatureData> itemPairFeatures = null;
			if(featuresByItemPair.containsKey(itemPairId)) {
				itemPairFeatures = featuresByItemPair.get(itemPairId);
			}
			else {
				itemPairFeatures = Lists.newArrayList();
			}
			
			String featureName = row.getString("feature_name");
			double featureValue = row.getDouble("feature_value");
			
			FeatureData featureData = new FeatureData(itemPairId, featureName, featureValue);
			itemPairFeatures.add(featureData);
			
			featuresByItemPair.put(itemPairId, itemPairFeatures);
		}
		
		return featuresByItemPair;
	}
	
	private static Attribute getClassAttribute()
	{
		List<String> classVal = Lists.newArrayList();
		classVal.add("match");
		classVal.add("mismatch");
		classVal.add("unknown");
		Attribute classAttr = new Attribute("class",classVal);
		
		return classAttr;
	}
	
	public static Map<Integer, Instance> getUnlabelledInstances(long jobId)
	{
		return null;// TODO
	}
}
