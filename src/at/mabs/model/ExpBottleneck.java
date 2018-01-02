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

/**
 * a population model where the population size is $indv$ for single generation, and then
 * (in backward time) goes to $N$. This has the effect of clipping the number of alleles
 * to a max of $2*indv$. 
 * 
 * @author swamidass
 *
 */
public class ExpBottleneck implements PopulationSizeModel{
	private final double indv, N;
	
	public ExpBottleneck(double indv, double N) {
		this.indv = indv;
		this.N = N;
	}

	@Override
	public double populationSize(double t) {
		return N;
	}

	public double getIndv() {
		return indv;
	}

	public double getN() {
		return N;
	}

	@Override
	public double generateWaitingTime(double t,double lambda, Random rand) {
		double ncr2 =2*indv * (2*indv - 1) / 4.0;//diploid

		if ( lambda > ncr2 ) {  // if number of alleles greater than 2*a
			return 0.000001;   // then we coalesce here (using a small dt instead of 0).
		} else {
			return -N * Math.log(rand.nextDouble()) / lambda; // otherwise, we are in population size $b$ regime
		}
	}
}
