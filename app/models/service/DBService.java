package models.service;

import java.util.Map;

import play.Logger;
import play.cache.Cache;
import models.Constants;
import models.ItemData;
import models.ItemPairGoldData;
import models.Job;

import com.walmartlabs.productgenome.rulegenerator.model.data.Dataset;
import com.walmartlabs.productgenome.rulegenerator.model.data.Item;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair.MatchStatus;

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
			
			for(Map.Entry<String, String> entry : itemA.getAttrMap().entrySet()) {
				ItemData data = new ItemData(itemA.getId(), entry.getKey(), entry.getValue());
				data.save();
			}
		}
		
		Logger.info("Persisted " + matchedItemPairs + " matched itempairs in db ..");
	}
	
	public static ItemPair getBestItemPairToLabel()
	{
		
	}
	
	public static ItemPair getRandomItemPairToLabel()
	{
		
	}
}
