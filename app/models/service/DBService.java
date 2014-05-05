package models.service;

import java.util.Map;

import models.ItemData;
import models.ItemPairGoldData;

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
		for(ItemPair itemPair : dataset.getItemPairs()) {
			Item itemA = itemPair.getItemA();
			Item itemB = itemPair.getItemB();
			boolean isMatch = itemPair.getMatchStatus().equals(MatchStatus.MATCH);
			
			if(isMatch) {
				ItemPairGoldData goldData = new ItemPairGoldData(itemA.getId(), itemB.getId());
				goldData.job = null;
				goldData.save();
			}
			
			for(Map.Entry<String, String> entry : itemA.getAttrMap().entrySet()) {
				ItemData data = new ItemData(itemA.getId(), entry.getKey(), entry.getValue());
				data.save();
			}
		}
	}
	
	public static ItemPair getBestItemPairToLabel()
	{
		
	}
	
	public static ItemPair getRandomItemPairToLabel()
	{
		
	}
}
