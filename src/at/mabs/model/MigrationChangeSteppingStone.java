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

public class MigrationChangeSteppingStone extends ModelEvent {
	private final int noDemes;
	private final double rate;
	private final boolean is2d;
	private final double crate;
	
	public MigrationChangeSteppingStone(long t,int noDemes,double rate, boolean is2d,double crate) {
		super(t);
		this.noDemes=noDemes-(crate>0?1:0);
		this.rate=rate;
		this.is2d=is2d;
		this.crate=crate;
	}
	
	
	
	@Override
	public void modifiyModel(Model model) {
		//System.out.println("ApplyMatrixChangeEvent");
		//assume all 1 d unwrapped..
		for(int i=1;i<noDemes;i++){
			model.setMigrationRate(i-1, i, rate);
			model.setMigrationRate(i, i-1, rate);
		}
		if(crate>0){
			for(int i=0;i<noDemes;i++){
				model.setMigrationRate(i, noDemes, crate);
				model.setMigrationRate( noDemes,i, 0);
			}
		}
		
		
	}

	@Override
	public void processEventCoalecent(LineageState state) {
		//no op

	}

	@Override
	public boolean isModelOnly() {
		return true;
	}
	

}
