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
 * simple population size model for a single deme. I may include other parameters and data. 
 * 
 * Note that some simulation may round the population size to a int. In this case it should use some
 * floor function. Note that size population size is always positive, just casting to an int is fine. 
 * @author bob
 *
 */
public interface PopulationSizeModel {
	public final static PopulationSizeModel NULL_POPULATION=new NullPopulation();
	public double populationSize(double t);
	/**
	 * used to generate a random value that represents the waiting time to the next 
	 * event. t is the time. 
	 * 
	 * So we can use 
	 * 
	 * U=exp(-lamda \int_t^T f(x) dx) 
	 * 
	 * and solve for T returning T-t; f(x) is the *coalescent rate per lineage* as a function of
	 * time. 
	 * 
	 * @param lambda typicaly the n(n-1) factor. 
	 * @param u
	 * @return dt from t of the event
	 */
	public double generateWaitingTime(double t,double lambda,Random random);
	
	public static class NullPopulation implements PopulationSizeModel{
		
		private NullPopulation() {
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public double generateWaitingTime(double t, double lambda, Random random) {
			assert false;
			throw new RuntimeException("Linage in a turned Off deme");
		}
		
		@Override
		public double populationSize(double t) {
			throw new RuntimeException("Linage in a turned Off deme");
			//return 0;
		}
	}
}	
