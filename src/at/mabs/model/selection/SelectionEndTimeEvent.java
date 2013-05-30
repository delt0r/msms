/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
*/
package at.mabs.model.selection;

import java.util.Arrays;

import at.mabs.coalescent.LineageState;
import at.mabs.model.FrequencyState;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;

/**
 * Rather than specify a time of fixation. We can specify a start time were selection matters.
 * 
 * 
 * So it inits SelectionData on all models that are newer than this model (and this model). Before a
 * selection run this model is set with the init frequencys...
 * 
 * the even time is round to an integer value. 
 * 
 * This is now the ONLY end of all selection event. 
 * 
 * @author bob
 * 
 */
public class SelectionEndTimeEvent extends ModelEvent {
	private final FrequencyState initalFrequencys;
	private final boolean volitle;

	public SelectionEndTimeEvent(long t){
		super(t);
		initalFrequencys=null;
		volitle=true;
		//throw new RuntimeException("WTF");
	}
	
	public SelectionEndTimeEvent(long t, FrequencyState initalFrequencys,boolean v) {
		super(t);
		this.initalFrequencys =initalFrequencys;
		volitle=v;
		//throw new RuntimeException("WTF2");
	}
	
	

	@Override
	protected void modifiyModel(Model model) {
		// no op
	}

	@Override
	protected void processEventCoalecent(LineageState state) {
		//System.err.println("We are processing The end of selection With C "+initalFrequencys+"\t"+state.getLineageSize(0, 1));
		if(initalFrequencys==null){
			coaleseAll(state);
			state.setSelection(false);
			return;
		}
		// move everything to the 0 allele
		
		for (int d =0; d < state.getDemeCount(); d++) {
			int n =state.getLineageSize(d, 1);
			for (int i =0; i < n; i++) {
				//System.out.println("Allele mutation at selection end");
				state.alleleMutationEvent(d, 1, 0, 0);
			}
		}
		state.setSelection(false);

	}
	
	private void coaleseAll(LineageState state){
		for (int d =0; d < state.getDemeCount(); d++) {
			int n =state.getLineageSize(d, 1);
			//coalese them till we have just one. 
			while (state.getLineageSize(d, 1)>1) {
				System.out.println(state.getLineageSize(d, 1) +"\t"+n);
				state.coalescentEvent(d, 1, 0);//?? 
				//state.alleleMutationEvent(d, 1, 0, 0);//we want to count!
				
			}
			//should [almost] never happen, since this must be the last coalesent event by segment coalescent.
			//but in the limit it will happen. ie very high selection.
			n =state.getLineageSize(d, 1);
			//System.err.println("Lineage left? "+n+"\t"+state.getLinagesActive()+"\t"+state.getLineageSize(0, 1));
			if(n>0){
				state.alleleMutationEvent(d, 1, 0, 0);
			}
		}
	}

	public FrequencyState getInitalFrequencys() {
		return initalFrequencys;
	}

	@Override
	public boolean isVolatileSelectionEvent() {
		return volitle;
	}
	
	@Override
	public String toString() {
	
		return "SelectionEnd:"+getEventTime()+"\t"+isVolatileSelectionEvent()+"\t"+initalFrequencys;
	}
}
