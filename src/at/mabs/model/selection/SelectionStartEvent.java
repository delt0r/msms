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
import java.util.Random;

import at.mabs.coalescent.LineageState;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.segment.FixedBitSet;
import at.mabs.util.Util;
import at.mabs.util.random.RandomGenerator;

/**
 * Represents the start of selection. If we are at time zero, then the frequencys may not be at
 * fixation. Samples are randomaly assined to alleles via frequency.
 * 
 * time is rounded to an integer.
 * 
 * @author bob
 * 
 */
public class SelectionStartEvent extends ModelEvent {
	private double[] frequencys;
	private boolean isVolatile=true;
	public SelectionStartEvent(long t, double[] frequencys) {
		super(t);
		//System.out.println("SelectionStart:"+t+"\t"+Arrays.toString(frequencys));
		//assert t==0:t;
		if (frequencys != null)
			this.frequencys =frequencys.clone();// just to be sure
		
	}

	@Override
	protected void modifiyModel(Model model) {
		// noop
	}

	@Override
	protected void processEventCoalecent(LineageState state) {
		// we move lineages to selected allele with probablity f.
		//System.out.println("DemeCount:"+state.getDemeCount());
		//System.out.println("Freq:"+Arrays.toString(frequencys));
		//System.err.println("Coalescent Starting thing!:"+frequencys);
		for (int d =0; d < state.getDemeCount(); d++) {
			double f =1;
			if (frequencys != null) {
				f =frequencys[d];
			}
			
			int n =state.getLineageSize(d, 0);
			
			Random random=RandomGenerator.getRandom();
			//System.out.println("LOOP "+this.getEventTime()+"\t"+state.getLinagesActive()+"\tn:"+n+"\tf:"+f);
			//int c=0;
			for (int i =0; i < n; i++) {
				// not quite the fastest...
				
				if (f == 1 ||  random.nextDouble()<=f) {
					state.alleleInitalMoveEvent(d, 0, 1);// MutationEvent(d, 0, 1, 0);
					//System.out.println("swap one:"+f);
				}
			}
			//System.out.println("\t"+c);
		}
		state.setSelection(true);
	//System.err.println("Coalescent Starting thing FINSIHED:"+state.getLineageSize(0, 1)+"\t"+state.getLinagesActive());
	}

	@Override
	public boolean isVolatileSelectionEvent() {
		return isVolatile;
	}
	
	public void setVolatile(boolean isVolatile) {
		this.isVolatile = isVolatile;
	}
	
	@Override
	public String toString() {
		
		return "SelectionStartEvent:"+this.getEventTime()+":"+Arrays.toString(frequencys);
	}

}
