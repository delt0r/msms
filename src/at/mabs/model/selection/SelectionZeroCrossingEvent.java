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
 * this event ensures that lineages do not exist in allele's with zero frequency
 * @author bob
 *
 */
@Deprecated
public class SelectionZeroCrossingEvent extends ModelEvent {
	private final int deme;
	private final int allele;
	
	
	public SelectionZeroCrossingEvent(long t,int d,int a) {
		super(t);
		
		deme=d;
		allele=a;
		throw new RuntimeException("Do not use");
	}

	@Override
	protected
	void modifiyModel(Model model) {
		//noop

	}
	
	/**
	 * rather tricky since we really need the model and frequencys. 
	 * 
	 * Doing this properly is quite a bit of work (CPU).
	 */
	@Override
	protected
	void processEventCoalecent(LineageState state) {
		if(state.getLineageSize(deme, allele)==0)
			return;
		int n=state.getLineageSize(deme, allele);
		System.err.println("Stuff in zero:"+n+" in deme "+deme+" allele "+ allele);
		for(int i=0;i<n;i++){
			state.alleleMutationEvent(deme, allele, 1-allele, 0);//FIXME dirty hack 
		}

	}
	
	@Override
	public boolean isVolatileSelectionEvent() {
		return true;
	}

}
