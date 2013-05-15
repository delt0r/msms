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
 * change or set the selection strength
 * 
 * @author bob
 * 
 */
public class SelectionStrengthEvent extends ModelEvent {
	private final SelectionStrengthModel ssm;
	private final int deme;

	public SelectionStrengthEvent(long t, int deme, SelectionStrengthModel ssm) {
		super(t);
		this.ssm =ssm;
		this.deme =deme;
	}

	public SelectionStrengthEvent(long t, SelectionStrengthModel ssm) {
		this(t, -1, ssm);
	}

	@Override
	protected void modifiyModel(Model model) {
		//System.err.println("applying selection strength:"+model);
		model.initSelectionData();
		if (deme < 0) {
			model.getSelectionData().setSelectionStrength(ssm);
		} else {
			model.getSelectionData().setSelectionStrength(deme, ssm);
		}
		//System.err.println("Finished:"+model);
		//throw new RuntimeException("Apply Selection strength");
	}

	@Override
	protected void processEventCoalecent(LineageState state) {
		// noop

	}

	@Override
	public boolean isModelOnly() {
		return true;
	}

}
