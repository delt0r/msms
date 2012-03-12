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

import java.util.Random;

public class ConstantPopulation implements PopulationSizeModel {
	private final double N;
	public ConstantPopulation(double N){
		this.N=N;
	}
	@Override
	public double populationSize(double t) {
		return N;
	}
	public double getN() {
		return N;
	}
	
	@Override
	public double generateWaitingTime(double t,double lambda, Random rand) {
		
		return -Math.log(rand.nextDouble())*N/lambda;
	}
	
	
	
}