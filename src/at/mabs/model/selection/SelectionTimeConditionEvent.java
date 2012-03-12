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

import at.mabs.coalescent.LineageState;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;

/**
 * This represent a time when some "condition" is true. The model must be time invariant. 
 * Note this does not need to be "fixation" it can be say a frequency of the allele in a deme. 
 * 
 * However the condition must be something that will always happen. Otherwise we have a non termination condition. 
 * 
 * The coalescent state managment is delegated to SelectionStartEvent which *must* be added with the forward 
 * simulator now. 
 * 
 * Event time is rounded to an integer value. 
 * 
 * @author bob
 * 
 */
public class SelectionTimeConditionEvent extends ModelEvent {
	private FrequencyCondition condition;
	public SelectionTimeConditionEvent(long t,FrequencyCondition c) {
		super(t);
		this.condition=c;
	}

	@Override
	protected void modifiyModel(Model model) {

	}

	@Override
	protected void processEventCoalecent(LineageState state) {
		// we do nothing
		//delegated to SelectionStarted added via SelectionData.simulate(). 
//		for (int d =0; d < state.getDemeCount(); d++) {
//			int n =state.getLineageSize(d, 0);
//			for (int i =0; i < n; i++)
//				state.alleleMoveEvent(d, 0, 1);//alleleMutationEvent(d, 0, 1, 0);
//		}
//		state.setSelection(true);
	}
	
	public FrequencyCondition getCondition() {
		return condition;
	}

}
