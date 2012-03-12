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
 * a population model where the population size is N=a*exp(-b*t). (time is going backwards) 
 * Note that while it is not an error to call this with a time that is
 * less that the timeOffset, this should not be generally done.
 * 
 * Also not that if you use this with a -b so that population size is increasing into the past.
 * It is possible that no coalescent events will ever happen.... 
 * @author bob
 *
 */
public class ExpPopulation implements PopulationSizeModel{
	private final double a,b,timeOffset;
	
	public ExpPopulation(double a, double b,double timeOffset) {
		this.a =a;
		this.b =b;
		this.timeOffset=timeOffset;
	}

	@Override
	public double populationSize(double t) {
		t-=timeOffset;
		return a*Math.exp(-b*t);
	}

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	@Override
	public double generateWaitingTime(double t,double lambda, Random rand) {
		t-=timeOffset;
		double dt=0;
		double inside=Math.exp(b*t)-a*b*Math.log(rand.nextDouble())/lambda;
		//this check is for growing popualtions. Its posible to grow faster than the 
		//coalescent rate and nothing will coalese --ever..
		//System.out.println("Exp:"+inside);
		if(inside<0)
			return Double.MAX_VALUE;
		//System.out.println("ExpDt:"+(Math.log(inside)/b-t));
		return Math.log(inside)/b-t;
	}
	
	
}