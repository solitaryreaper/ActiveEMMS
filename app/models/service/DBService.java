
package models.service;

import java.util.List;
import java.util.Map;

import models.Constants;
import models.ItemData;
import models.ItemPairGoldData;
import models.Job;
import play.Logger;
import play.cache.Cache;
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
import com.walmartlabs.productgenome.rulegenerator.service.EntropyCalculationService;
import com.walmartlabs.productgenome.rulegenerator.utils.WekaUtils;

public class DBService 
{
	/**
	 * Loads the dataset into database after converting it in the proper db format.
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
		Learner learner = null;
		
		// Check if any informative itempairs from last lot is still left to be labelled. If no,
		// fetch another lot of most informative itempairs from the remaining unlabelled itempairs
		// in the dataset.
		if(mostInfoItemPairs == null || mostInfoItemPairs.isEmpty()) {
			Job currJob = (Job) Cache.get(Constants.CACHE_JOB);
			List<ItemPair> unlabelledItemPairs = getAllUnlabelledItemPairs(currJob.id);
			List<ItemPair> labelledItemPairs = getAllLabelledItemPairs(currJob.id);
			
			List<String> attributes = (List<String>) Cache.get(Constants.CACHE_DATASET_ATTRIBUTES);
			Dataset labelledData = new Dataset(currJob.name, attributes, labelledItemPairs);
			learner = getLearnerUsingLabelledData(labelledData);
			Cache.set(Constants.CACHE_MATCHER, learner);
			
			Logger.info("Found " + labelledItemPairs.size() + " labelled pairs ..");

			Dataset unlabelledData = new Dataset(currJob.name, attributes, unlabelledItemPairs);
			mostInfoItemPairs = EntropyCalculationService.getTopKInformativeItemPairs(learner, unlabelledData);
		}

		mostInfoItemPair = mostInfoItemPairs.remove(0);
		Cache.set(Constants.CACHE_BEST_ITEMPAIRS, mostInfoItemPairs);
		return mostInfoItemPair;
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
	
	public static List<ItemPair> getAllUnlabelledItemPairs(long jobId)
	{
		String fetchUnlabelledPairsSQL = 
			"SELECT item.item_pair_id, item.datasource_id, item.item_id, item.attribute, item.value " +
			"FROM item_data item LEFT OUTER JOIN itempair_gold_data gold ON (gold.item_pair_id = item.item_pair_id) " +
			"WHERE gold.item_pair_id IS NULL AND item.job_id = " + jobId + " ORDER BY item.item_pair_id";
		Logger.info(fetchUnlabelledPairsSQL);
		
		List<SqlRow> unlabelledItemPairsRaw = Ebean.createSqlQuery(fetchUnlabelledPairsSQL).findList();
		return extractItemPairs(unlabelledItemPairsRaw, MatchStatus.UNKNOWN);
	}

	public static List<ItemPair> getAllLabelledItemPairs(long jobId)
	{
		List<ItemPair> labelledItemPairs = Lists.newArrayList();
		String fetchMatchedPairsSQL = 
			"SELECT DISTINCT item.item_pair_id, item.datasource_id, item.item_id, item.attribute, item.value " +
			"FROM itempair_gold_data gold JOIN item_data item ON (gold.item_pair_id = item.item_pair_id) " +
			"WHERE gold.match_status = 0 AND item.job_id = " + jobId + " ORDER BY item.item_pair_id";
		Logger.info(fetchMatchedPairsSQL);
		List<SqlRow> matchedItemPairsRaw = Ebean.createSqlQuery(fetchMatchedPairsSQL).findList();
		if(!(matchedItemPairsRaw == null || matchedItemPairsRaw.isEmpty())) {
			List<ItemPair> matchedItemPairs = extractItemPairs(matchedItemPairsRaw, MatchStatus.MATCH);
			labelledItemPairs.addAll(matchedItemPairs);
		}

		String fetchMismatchedPairsSQL = 
				"SELECT DISTINCT item.item_pair_id, item.datasource_id, item.item_id, item.attribute, item.value " +
				"FROM itempair_gold_data gold JOIN item_data item ON (gold.item_pair_id = item.item_pair_id) " +
				"WHERE gold.match_status = 1 AND item.job_id = " + jobId + " ORDER BY item.item_pair_id";
		Logger.info(fetchMismatchedPairsSQL);
		List<SqlRow> mismatchedItemPairsRaw = Ebean.createSqlQuery(fetchMatchedPairsSQL).findList();
		if(!(mismatchedItemPairsRaw == null || mismatchedItemPairsRaw.isEmpty())) {
			List<ItemPair> mismatchedItemPairs = extractItemPairs(mismatchedItemPairsRaw, MatchStatus.MISMATCH);
			labelledItemPairs.addAll(mismatchedItemPairs);
		}
		
		Logger.info("Found " + labelledItemPairs.size() + " labelled itempairs ..");
		return labelledItemPairs;
	}

	private static List<ItemPair> extractItemPairs(List<SqlRow> itemPairsRaw, MatchStatus matchStatus)
	{
		// Group items for the same itempair first
		Map<Integer, List<SqlRow>> itemPairItemsMap = Maps.newHashMap();
		for(SqlRow row : itemPairsRaw) {
			Integer itemPairId = row.getInteger("item_pair_id");
			List<SqlRow> itemPairItems = null;
			if(itemPairItemsMap.containsKey(itemPairId)) {
				itemPairItems = itemPairItemsMap.get(itemPairId);
			}
			else {
				itemPairItems = Lists.newArrayList();
			}
			
			itemPairItems.add(row);
			itemPairItemsMap.put(itemPairId, itemPairItems);
		}

		List<ItemPair> itemPairs = Lists.newArrayList();
		for(Map.Entry<Integer, List<SqlRow>> entry : itemPairItemsMap.entrySet()) {
			List<SqlRow> rawItems = entry.getValue();
			
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
			
			Item item1 = new Item(item1Id, item1AttrMap);
			Item item2 = new Item(item2Id, item2AttrMap);
			
			itemPairs.add(new ItemPair(item1, item2, matchStatus));
		}
		
		return itemPairs;
	}
	
	/**
	 * Returns a random unlabelled itempair which has to be labelled.
	 */
	public static ItemPair getRandomItemPairToLabel(Long jobId)
	{
		Logger.info("Finding random item pair to label ..");
		String randomItem1FetchSQL = 
			"select distinct data.item_id, data.attribute, data.value "
			+ "from item_data data join (select item_id from item_data where datasource_id = 1 order by rand() limit 1) src1 "
			+ "on (data.item_id = src1.item_id) ";
		String randomItem2FetchSQL =
			"select distinct data.item_id, data.attribute, data.value from item_data data join "
			+ "(select item_id from item_data where datasource_id = 2 order by rand() limit 1) src2 "
			+ "on (data.item_id = src2.item_id)";

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
