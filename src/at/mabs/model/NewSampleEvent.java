package at.mabs.model;

import at.mabs.coalescent.LineageState;

/**
 * Were do samples come from. Here! We add leaf here aka serial samples.  
 * @author bob
 *
 */

public class NewSampleEvent extends Event {
	private final int[] demeSampleCounts;
	
	public NewSampleEvent(long time,int[] demeCounts) {
		super(time);
		demeSampleCounts=demeCounts.clone();//just to be safe.
		
	}
	
	
}
