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

public class MigrationChangeEvent extends ModelEvent {
	private final int i,j;// -1 for all...
	private final double rate;
	
	public MigrationChangeEvent(long t,int i,int j,double rate) {
		super(t);
		this.i=i;
		this.j=j;
		this.rate=rate;
		//System.out.println("MCE:"+t+"\ti:"+i+"\tj:"+j+"\trate:"+rate);
	}
	
	public MigrationChangeEvent(long t,double rate) {
		this(t,-1,-1,rate);
	}
	
	@Override
	public void modifiyModel(Model model) {
		//System.out.println("ApplyMatrixChangeEvent:"+model);
		if(i<0){
			model.setMigrationMatrix(rate);
		}else{
			model.setMigrationRate(i, j, rate);
		}
	}

	@Override
	public void processEventCoalecent(LineageState state) {
		//no op
		//System.out.println("MigraionChangeCoalescent:"+state.getCurrentTime());

	}

	@Override
	public boolean isModelOnly() {
		return true;
	}
	

}
