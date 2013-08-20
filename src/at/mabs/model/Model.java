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

import java.util.*;

//import at.mabs.coalescent.CoalescentEvent;
//import at.mabs.coalescent.CoalescentEventCalculator;
import at.mabs.model.selection.SelectionData;
import at.mabs.util.Bag;
import at.mabs.util.Util;

/**
 * The main model class. Contains all the details of a model between events. this is linked list of interleaving models and events<p>
 * 
 * The root model, or "present day" model has a terminating event for a parent. The last model has a terminating event for a child. Child is always more pastward. Time information is
 * in the events. We are ignoring performance for now. 
 * 
 * Data stored here? Population sizes, selection parameters, haplotype data if needed. 
 * 
 * @author bob
 * 
 */
public final  class Model{
	private Event pastward;//yonger or ansestral event, null for 'root' model
	private Event presentward;//pastward event.
	
	private PopulationSizeModel[] populationSizes;
	
	private Bag<MigrationEntry> migrations=new Bag<MigrationEntry>();
	
	/**
	 * clone constructor.
	 * @param model
	 */
	Model(Model model) {
		
	}
	
	Model() {
		
	}
	
	//should only be called by Event
	void setPresentward(Event forwd) {
		this.presentward=forwd;
	}
	
	//should only be called by Event
	void setPastward(Event parent) {
		this.pastward = parent;
	}
	
	public Event getPastward() {
		return pastward;
	}
	
	public Event getPresentward() {
		return presentward;
	}
	
	public final static class MigrationEntry{
		final int i,j;
		final double rate;
		/**
		 * from i to j in *pastward* time. 
		 * @param i
		 * @param j
		 * @param rate
		 */
		public MigrationEntry(int i, int j, double rate) {
			super();
			this.i = i;
			this.j = j;
			this.rate = rate;
		}
		
	} 
}
