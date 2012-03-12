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
/**
 * This is to get around a simple oversite with the exp model... 
 * 
 * that it the exp base size it based on when the model is applied. Since i don't 
 * know that at parse time, the exp object must be constructed at forward model creation time. 
 * @author bob
 *
 */
public class ExpPopulationEvent extends ModelEvent{
	private final double alpha;
	private final int deme;//-1 for all demes
	
	public ExpPopulationEvent(long t,int deme,double alpha) {
		super(t);
		this.deme=deme;
		this.alpha=alpha;
	}
	
	public ExpPopulationEvent(long t,double alpha) {
		this(t,-1,alpha);
	}
	
	@Override
	public void modifiyModel(Model model) {
		//System.out.println("Setting ExpGroth");
		if(deme<0){
			model.setExpGrowth(alpha, this.getEventTime());
		}else{
			model.setExpGrowth(deme, alpha, this.getEventTime());
		}
		
	}

	@Override
	public void processEventCoalecent(LineageState state) {
		//no op... nothing needs to be done
		
	}

	@Override
	public boolean isModelOnly() {
		return true;
	}
	
	
}
