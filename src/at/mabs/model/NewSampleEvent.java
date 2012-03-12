package at.mabs.model;

import at.mabs.coalescent.LineageState;

/**
 * Were do samples come from. Here! We add leaf here aka serial samples.  
 * @author bob
 *
 */

public class NewSampleEvent extends ModelEvent {
	private final int[] demeSampleCounts;
	
	public NewSampleEvent(long time,int[] demeCounts) {
		super(time);
		demeSampleCounts=demeCounts.clone();//just to be safe.
		fineSortOrder=-1;//come first 
	}
	
	@Override
	protected void processEventCoalecent(LineageState state) {
		//add our new samples. Easy right? Yea right. 
		for(int d=0;d<demeSampleCounts.length;d++){
			int count=demeSampleCounts[d];
			
			for(int i=0;i<count;i++){
				state.addSample(d);
			}
		}
	}

	@Override
	protected void modifiyModel(Model model) {
		

	}
}
