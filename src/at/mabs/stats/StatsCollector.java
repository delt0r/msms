package at.mabs.stats;

import java.util.*;

import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;

/**
 * so this should collect stats. However it doesn't produce output to a string. 
 * 
 * How should it produce output? We will start with a list of doubles. They are named. 
 * 
 * We will have a mask standard as well. This permits any of these stats to be used for "binning"
 * and for "deme masks" etc.
 * 
 *  
 * implementations should be thread safe in that collectStats should not block and should work properly when more than one
 * thread has called the method at the same time.
 *  
 * @author bob
 *
 */
public interface StatsCollector {
	
	
	
	public double[] collectStats(SegmentEventRecoder recorder);
	/**
	 * a list of means then varainces of all that this stat collector collects. Note that if the same size is one, just return the first collected stats.
	 * @return
	 */
	public double[] summaryStats();
	public String[] getStatLabels();
	/**
	 * called before collectStats is called
	 */
	public void init();
	/**
	 * all stats should use this. This is the sampels that should be used for the stat
	 */
	public void setLeafMask(FixedBitSet mask);
	
	/**
	 * This is optional. For example some stats are comparative. This gives the second set of samples to compare
	 * to. If the user does not provide a second set, or you don't want one and they do. Implementations are 
	 * free to ignore or raise errors or exceptions. 
	 * 
	 * This is always set *after* setLeafMask
	 * @param mask
	 */
	public void setSecondLeafMask(FixedBitSet mask);
	
	
	public static class ClassFactory {
		private static final Map<String,Class<? extends StatsCollector>> namedClasses;
		private static final String PACKAGE=(new ClassFactory()).getClass().getPackage().getName();
		static{
			namedClasses=new HashMap<String, Class<? extends StatsCollector>>();
			namedClasses.put("Fst",Fst.class);
			namedClasses.put("HackStat",GaussNLStat.class);
		}
		
		public static Class<? extends StatsCollector> getNamedStatsCollector(String name){
			//first a name lookup
			Class<? extends StatsCollector> cls=namedClasses.get(name);
			if(cls!=null)
				return cls;
			
			//now we append package info
			try {
				cls=(Class<? extends StatsCollector>)Class.forName(PACKAGE+"."+name);
				if(cls!=null)
					return cls;
				cls=(Class<? extends StatsCollector>)Class.forName(name);
				if(cls!=null)
					return cls;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Stats Collector not found. Check that the name is typed correctly and that the class path is correct",e);
			}
			assert false;
			return null;
		}
	}
}
