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
package at.mabs.model;

import at.mabs.coalescent.LineageState;
import at.mabs.model.selection.SelectionData;


/**
 * All events are not constrained to be on integer generation boundries. 
 * 
 * The idea is that we have a list of events that affect the model at specific points in time.
 * 
 * Events have 2 main things to do. Adjust the model for the forward time. And to adjust the model
 * and move samples for the backward time version.
 * 
 * time is defined as time since "present" or sampling in generations.
 * 
 * there are some book keeping events. When a deme gets a zero in frequency there is an event to
 * ensure that all lineages that are from that allele are removed from the deme. In this case its
 * either a migration or a mutation. We just use conditional probabilities for each.
 * 
 * models to add: split. Merge. Zeros for selection.
 */
public abstract class ModelEvent implements Comparable<ModelEvent>{
	protected final long eventTime;
	protected int fineSortOrder=0;//permits subclass to be sorted in a predefined way
	
	
	public ModelEvent(long t) {
		eventTime =t;
	}

	public boolean isModelOnly(){
		return false;
	}
	
	public boolean isVolatileSelectionEvent(){
		return false;
	}
	
	public long getEventTime() {
		return eventTime;
	}

	/**
	 * The task here is to update the freqencys in a maner consitant with this event. Default is to 
	 * just apply state. 
	 * 
	 * The pastward model frequency are placed into state. 
	 * Also if deme size is changed, ensure that state.getCurrentDemeCount is correct for the current
	 * data....
	 * @param pastwardData TODO
	 * @param currentData TODO
	 */
	 protected void processEventSelection(SelectionData oldData,SelectionData currentData,FrequencyState state){
		
	}

	/**
	 * Modifies state in the proper way that respects the chagnes that this model Event represents.
	 */
	protected abstract void processEventCoalecent(LineageState state);
	
	/**
	 * from the base model, or a model in a event sequence produce a new model. 
	 * 
	 * This should only be called once on any model.
	 * 
	 * The general contract is that this model has a valid start and end time and other parameters. However
	 * it is *not* finalised. 
	 * 
	 * Needs a better name. 
	 * @param model
	 * @return
	 */
	protected abstract void modifiyModel(Model model);

	@Override
	public int compareTo(ModelEvent o) {
		if(eventTime<o.eventTime)
			return -1;
		if(eventTime>o.eventTime)
			return 1;
		if(fineSortOrder<o.fineSortOrder)
			return -1;
		if(fineSortOrder>o.fineSortOrder)
			return 1;
		return 0;
	}
	
	public String toString(){
		return "[event:"+(double)eventTime+" " +this.getClass()+"]";
	}
	
	
}
