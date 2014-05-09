import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.util.List;

import models.service.DBService;

import org.junit.Test;

import play.Logger;

import com.google.common.collect.Lists;
import com.walmartlabs.productgenome.rulegenerator.algos.Learner;
import com.walmartlabs.productgenome.rulegenerator.model.data.Dataset;
import com.walmartlabs.productgenome.rulegenerator.model.data.ItemPair;
import com.walmartlabs.productgenome.rulegenerator.service.EntropyCalculationService;
public class DBServiceTest {
	
	@Test
	public void testGetItemPairsFromDB() {
		running(fakeApplication(), new Runnable() {
			public void run() {
				long jobId = 2l;
				//List<ItemPair> unlabelledItemPairs = DBService.getAllUnlabelledItemPairs(jobId);
				List<ItemPair> labelledItemPairs = DBService.getAllLabelledItemPairs(jobId);
				
				//Logger.info("Unlabelled itempair : " + unlabelledItemPairs.get(0).getItemB().getAttrMap().toString());
				Logger.info("Labelled itempair : " + labelledItemPairs.get(0).getItemB().getAttrMap().toString());
				
				/*
				Logger.info("# Unlabelled Itempairs found : " + unlabelledItemPairs.size());
				Logger.info("# Labelled itempairs found : " + labelledItemPairs.size());
				
				List<String> attributes = Lists.newArrayList("name", "addr", "city", "type");
				
				Dataset labelledData = new Dataset("test", attributes, labelledItemPairs);
				Dataset unlabelledData = new Dataset("test", attributes, unlabelledItemPairs);
				Logger.info("# Labelled : " + labelledData.getItemPairs().size());
				Logger.info("# Unlabelled : " + unlabelledData.getItemPairs().size());
				
				Learner learner = DBService.getLearnerUsingLabelledData(labelledData);				
				List<ItemPair> mostInfoItemPairs = EntropyCalculationService.getTopKInformativeItemPairs(learner, unlabelledData);
				
				ItemPair pair = mostInfoItemPairs.get(0);
				Logger.info("ItemA : " + pair.getItemA().getAttrMap().toString());
				Logger.info("ItemB : " + pair.getItemB().getAttrMap().toString());
				
				System.out.println("# Most informative itempairs : " + mostInfoItemPairs.size());
				*/
			}
		});
	}
}
