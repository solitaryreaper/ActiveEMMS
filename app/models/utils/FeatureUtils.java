package models.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import models.FeatureData;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;

/**
 * Utility methods for converting data into various formats like weka instance format and actual
 * text data format. 
 *
 */
public class FeatureUtils {

	public static Instance getInstance(List<FeatureData> features, ArrayList<Attribute> attributes)
	{
		double[] values = new double[attributes.size()];
		Map<String, FeatureData> featureNameMap = Maps.newHashMap();
		for(FeatureData fData : features) {
			featureNameMap.put(fData.featureName, fData);
		}
		
		for(int i=0; i < attributes.size() - 1; i++) {
			String attrName = attributes.get(i).name();
			if(featureNameMap.containsKey(attrName)) {
				values[i] = featureNameMap.get(attrName).featureValue;
			}
			else {
				values[i] = 0.0;
			}
			
		}
		
		return new DenseInstance(1.0, values);
	}

	public static Attribute getClassAttribute()
	{
		List<String> classVal = Lists.newArrayList();
		classVal.add("match");
		classVal.add("mismatch");
		classVal.add("unknown");
		Attribute classAttr = new Attribute("class",classVal);
		
		return classAttr;
	}
}
