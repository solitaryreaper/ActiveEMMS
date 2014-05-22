package models.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import models.Constants;
import models.FeatureData;
import models.Job;
import play.Logger;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlRow;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.algos.RandomForestLearner;
import com.walmartlabs.productgenome.rulegenerator.model.data.Item;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;

/**
 * A set of utility functions for helping in the data access and persistence to DB.
 * @author excelsior
 *
 */
public class DBUtils {

	/**
	 * Returns the itempair corresponding to the item pair id.
	 */
	public static ItemPair getItemPairById(int itemPairId, Job currJob, MatchStatus matchStatus)
	{
		String itemPairSQL = 
				"SELECT item.item_pair_id, item.datasource_id, item.item_id, item.attribute, item.value " +
				"FROM item_data item WHERE item.item_pair_id = " + itemPairId + " AND item.job_id = " + currJob.id;
		
		List<SqlRow> rawItems = Ebean.createSqlQuery(itemPairSQL).findList();
		
		String item1Id = null;
		String item2Id = null;
		Map<String, String> item1AttrMap = Maps.newHashMap();
		Map<String, String> item2AttrMap = Maps.newHashMap();
		for(SqlRow itemAttrValuePair : rawItems) {
			String itemId = itemAttrValuePair.getString("item_id");
			String attribute = itemAttrValuePair.getString("attribute");
			String value = itemAttrValuePair.getString("value");
			Integer dataSourceId = itemAttrValuePair.getInteger("datasource_id");
			
			if(dataSourceId.equals(Constants.DATA_SOURCE1_ID)) {
				item1Id = itemId;
				item1AttrMap.put(attribute, value);
			}
			else {
				item2Id = itemId;
				item2AttrMap.put(attribute, value);
			}
		}
		
		if(matchStatus == null) {
			matchStatus = MatchStatus.UNKNOWN;
		}
		
		Item item1 = new Item(item1Id, item1AttrMap);
		Item item2 = new Item(item2Id, item2AttrMap);		
		return new ItemPair(item1, item2, matchStatus);
	}
	
	public static Map<Integer, Instance> getTopKEntropyInstances(Learner learner, 
			Map<Integer, Instance> itemPairInstanceMap, int K)
	{
		Map<Integer, Instance> topKEntropyInstances = Maps.newHashMap();
		RandomForestLearner rf = (RandomForestLearner)learner;
		PriorityQueue<InstanceEntropy> mostInfoItemPairs = 
				new PriorityQueue<DBUtils.InstanceEntropy>(K);
		
		double maxEntropy = 0.0;
		for(Map.Entry<Integer, Instance> entry : itemPairInstanceMap.entrySet()) {
			Integer itemPairId = entry.getKey();
			Instance instance = entry.getValue();
			
			double entropy = 0.0;
			try {
				entropy = rf.getVotingEntropyForInstance(instance);
				if(Double.compare(entropy, maxEntropy) > 0) {
					maxEntropy = entropy;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(mostInfoItemPairs.size() < K) {
				mostInfoItemPairs.add(new InstanceEntropy(instance, entropy, itemPairId));
			}
			else {
				InstanceEntropy head = mostInfoItemPairs.peek();
				if(Double.compare(head.entropy,entropy) < 0) {
					mostInfoItemPairs.remove(head);
					mostInfoItemPairs.add(new InstanceEntropy(instance, entropy, itemPairId));
				}				
			}
		}
		
		for(InstanceEntropy instanceEntropy : mostInfoItemPairs) {
			topKEntropyInstances.put(instanceEntropy.itemPairId, instanceEntropy.instance);
		}
		
		return topKEntropyInstances;
	}
	
	private static class InstanceEntropy implements Comparable<InstanceEntropy>
	{
		private Instance instance;
		private double entropy;
		private Integer itemPairId;
		
		public InstanceEntropy(Instance instance, double entropy, Integer itemPairId)
		{
			this.instance = instance;
			this.entropy = entropy;
			this.itemPairId = itemPairId;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof InstanceEntropy) {
				InstanceEntropy that = (InstanceEntropy)obj;
				return Objects.equal(this.instance, that.instance) &&
						Objects.equal(this.entropy, that.entropy) &&
						Objects.equal(this.itemPairId, that.itemPairId);
			}

			return false;
		}

		@Override
		public int hashCode()
		{
			return Objects.hashCode(this.instance, this.entropy, this.itemPairId);
		}
		
		public int compareTo(InstanceEntropy that) {
			return Double.compare(this.entropy, that.entropy);
		}
	}
	
	/**
	 * Returns all the labelled instances by crowd for a specific matching job .. 
	 */
	public static Instances getLabelledInstances(long jobId, ArrayList<Attribute> attributes)
	{
		return getLabelledInstances(jobId, attributes, false);
	}
	
	/**
	 * Returns labelled instances by crowd for a specific matching job. A filter option to return
	 * only the instances labelled during testing phase is also provided.
	 */
	public static Instances getLabelledInstances(long jobId, ArrayList<Attribute> attributes, boolean onlyTestPhaseLabelled)
	{
		Instances instances = getBasicInstances("Labelled Data", attributes);
		int numInstances = 0;
		
		String phaseFilter = onlyTestPhaseLabelled ? " AND gold.is_labelled_in_train_phase = 0 " : "";
		
		String labelledItemPairsSQL = 
			" SELECT feature.item_pair_id, feature.feature_name, feature.feature_value"
			+ " FROM itempair_gold_data gold JOIN feature_data feature "
			+ " ON (gold.item_pair_id = feature.item_pair_id AND gold.job_id = feature.job_id)";
						
		String matchedItemPairsSQL = 
				labelledItemPairsSQL + 
				" WHERE gold.job_id = " + jobId + " AND gold.match_status IN (0) " + phaseFilter +
				" ORDER BY feature.item_pair_id";
		Logger.error(matchedItemPairsSQL);
		List<SqlRow> matchedItemPairsRaw = Ebean.createSqlQuery(matchedItemPairsSQL).findList();
		if(!matchedItemPairsRaw.isEmpty()) {
			Map<Integer, List<FeatureData>> matchedItemPairs = getFeaturesByItemPair(matchedItemPairsRaw);
			for(Map.Entry<Integer, List<FeatureData>> entry : matchedItemPairs.entrySet()) {
				Instance instance = FeatureUtils.getInstance(entry.getValue(), attributes);
				instance.setDataset(instances);
				instance.setValue(instances.numAttributes() - 1, "match");
				instances.add(instance);
				++numInstances;
			}			
		}
		
		String mismatchedItemPairsSQL = 
				labelledItemPairsSQL + 
				" WHERE gold.job_id = " + jobId + " AND gold.match_status IN (1) " + phaseFilter +
				" ORDER BY feature.item_pair_id";
		Logger.error(mismatchedItemPairsSQL);		
		List<SqlRow> mismatchedItemPairsRaw = Ebean.createSqlQuery(mismatchedItemPairsSQL).findList();
		if(!mismatchedItemPairsRaw.isEmpty()) {
			Map<Integer, List<FeatureData>> mismatchedItemPairs = getFeaturesByItemPair(mismatchedItemPairsRaw);
			for(Map.Entry<Integer, List<FeatureData>> entry : mismatchedItemPairs.entrySet()) {
				Instance instance = FeatureUtils.getInstance(entry.getValue(), attributes);
				instance.setDataset(instances);
				instance.setValue(instances.numAttributes() - 1, "mismatch");
				instances.add(instance);
				++numInstances;
			}	
		}
		
		Logger.info("Found " + instances.numInstances() + " instances in the dataset : " + numInstances);
		return instances;		
	}
	
	private static Instances getBasicInstances(String label, ArrayList<Attribute> attributes)
	{
		Instances instances = new Instances(label, attributes, 0);
		instances.setClassIndex(instances.numAttributes() - 1);
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
	
	public static Map<Integer, Instance> getUnlabelledInstances(long jobId, ArrayList<Attribute> attributes)
	{
		Instances instances = getBasicInstances("Unlabelled Data", attributes);
		
		String unlabelledItemPairsSQL = 
				" SELECT feature.item_pair_id, feature.feature_name, feature.feature_value "
				+ " FROM feature_data feature LEFT OUTER JOIN itempair_gold_data gold "
				+ " ON (gold.item_pair_id = feature.item_pair_id AND gold.job_id = feature.job_id) "
				+ " WHERE gold.item_pair_id IS NULL ORDER BY feature.item_pair_id ";
		
		Map<Integer, Instance> itemPairInstanceMap = Maps.newHashMap();
		List<SqlRow> unlabelledItemPairsRaw = Ebean.createSqlQuery(unlabelledItemPairsSQL).findList();
		if(!unlabelledItemPairsRaw.isEmpty()) {
			Map<Integer, List<FeatureData>> unlabelledItemPairs = getFeaturesByItemPair(unlabelledItemPairsRaw);
			for(Map.Entry<Integer, List<FeatureData>> entry : unlabelledItemPairs.entrySet()) {
				Integer itemPairId = entry.getValue().get(0).itemPairId;
				
				Instance instance = FeatureUtils.getInstance(entry.getValue(), attributes);
				instance.setDataset(instances);
				instance.setValue(instances.numAttributes() - 1, "unknown");
				instances.add(instance);
				
				itemPairInstanceMap.put(itemPairId, instance);
			}			
		}
		
		return itemPairInstanceMap;
	}
}
