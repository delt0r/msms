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

public class MigrationMatrixEvent extends ModelEvent {
	private final double[][] matrix;//implied nxn where we ignore i==j
	
	
	
	public MigrationMatrixEvent(long t, double[][] matrix) {
		super(t);
		this.matrix =matrix;
	}

	@Override
	public void modifiyModel(Model model) {
		model.setMigrationRates(matrix);

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
