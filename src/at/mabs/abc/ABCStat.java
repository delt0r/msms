package at.mabs.abc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

import at.mabs.stats.StatsCollector;
import at.mabs.stats.StringStatsCollector;
/**
 * need to make the distance "signed" so that we know which "side" its on. 
 * 
 * Not sure how to do that if this is a summary of summarys... linear combination?
 * 
 * 
 * @author bob
 *
 * @param <T>
 * @deprecated
 */
public interface ABCStat<T extends ABCStat<T>> extends StatsCollector{
	
	/**
	 * 
	 * @param other
	 * @return array of signed distances. 
	 */
	public double[] distance(T other);//info on the "metric" is a little hard i guess. Perhaps have hints. 
	
	public void init(int[] sampleConfig);
	
	
	
}
