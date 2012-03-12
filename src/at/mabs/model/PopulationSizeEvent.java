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
 * a general change in the population demographics.
 * @author bob
 *
 */
public class PopulationSizeEvent extends ModelEvent{
	private final PopulationSizeModel sizeModel;
	private final int deme;//-1 for all demes
	
	public PopulationSizeEvent(long t,int deme,PopulationSizeModel sizeModel) {
		super(t);
		this.deme=deme;
		this.sizeModel=sizeModel;
	}
	
	public PopulationSizeEvent(long t,PopulationSizeModel sizeModel) {
		this(t,-1,sizeModel);
	}
	
	@Override
	public void modifiyModel(Model model) {
		if(deme<0){
			model.setPopulationModel(sizeModel);
		}else{
			model.setPopulationModel(deme, sizeModel);
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
