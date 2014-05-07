package models.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.cache.Cache;
import sun.security.jca.GetInstance;
import models.Constants;
import models.ItemData;
import models.ItemPairGoldData;
import models.Job;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.SqlRow;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.model.data.Dataset;
import com.walmartlabs.productgenome.rulegenerator.model.data.Item;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;
import com.walmartlabs.productgenome.rulegenerator.service.EntropyCalculationService;

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
		for(ItemPair itemPair : dataset.getItemPairs()) {
			Item itemA = itemPair.getItemA();
			Item itemB = itemPair.getItemB();
			boolean isMatch = itemPair.getMatchStatus().equals(MatchStatus.MATCH);
			
			if(isMatch) {
				ItemPairGoldData goldData = new ItemPairGoldData(itemA.getId(), itemB.getId(), MatchStatus.MATCH);
				goldData.job = job;
				goldData.save();
				
				++matchedItemPairs;
			}
			
			// Persist item from first source
			for(Map.Entry<String, String> entry : itemA.getAttrMap().entrySet()) {
				ItemData data = new ItemData(Constants.DATA_SOURCE1_ID, itemA.getId(), entry.getKey(), entry.getValue());
				data.job = job;
				
				boolean isAlreadyInDB = 
					ItemData.find.where().
						eq("item_id", itemA.getId()).
						eq("datasource_id", Constants.DATA_SOURCE1_ID).
						findRowCount() > 0;
				if(!isAlreadyInDB) {
					data.save();					
				}
			}
			
			// Persist item from second source
			for(Map.Entry<String, String> entry : itemA.getAttrMap().entrySet()) {
				ItemData data = new ItemData(Constants.DATA_SOURCE2_ID, itemA.getId(), entry.getKey(), entry.getValue());
				data.job = job;
				
				boolean isAlreadyInDB = 
					ItemData.find.where().
						eq("item_id", itemB.getId()).
						eq("datasource_id", Constants.DATA_SOURCE2_ID).
						findRowCount() > 0;
				if(!isAlreadyInDB) {
					data.save();					
				}
			}
		}
		
		Logger.info("Persisted " + matchedItemPairs + " matched itempairs in db ..");
	}
	
	/**
	 * Returns the most informative unlabelled itempair whose labelling would add most information to the 
	 * existing learned model.
	 */
	public static ItemPair getBestItemPairToLabel()
	{
		ItemPair mostInfoItemPair = null;
		List<ItemPair> mostInfoItemPairs = (List<ItemPair>) Cache.get(Constants.CACHE_BEST_ITEMPAIRS);
		Learner learner = null;
		
		// Check if any informative itempairs from last lot is still left to be labelled. If no,
		// fetch another lot of most informative itempairs from the remaining unlabelled itempairs
		// in the dataset.
		if(mostInfoItemPairs == null || mostInfoItemPairs.isEmpty()) {
			Job currJob = (Job) Cache.get(Constants.CACHE_JOB);
			List<ItemPair> unlabelledItemPairs = getAllUnlabelledItemPairs(currJob);
			List<ItemPair> labelledItemPairs = getAllLabelledItemPairs(currJob);
			
			learner = (Learner) Cache.get(Constants.CACHE_MATCHER);			
			mostInfoItemPairs = EntropyCalculationService.getTopKInformativeItemPairs(learner, 
					unlabelledItemPairs, Constants.NUM_ITEMPAIRS_PER_ITERATION);
		}

		mostInfoItemPair = mostInfoItemPairs.remove(0);
		Cache.set(Constants.CACHE_BEST_ITEMPAIRS, mostInfoItemPairs);
		return mostInfoItemPair;
	}
	
	private static List<ItemPair> getAllUnlabelledItemPairs(Job job)
	{
		List<ItemPair> unlabelledItemPairs = null;
		return unlabelledItemPairs; // TODO
	}

	private static List<ItemPair> getAllLabelledItemPairs(Job job)
	{
		List<ItemPair> labelledItemPairs = null;
		return labelledItemPairs; // TODO
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
