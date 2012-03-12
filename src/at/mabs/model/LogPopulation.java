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
 * implements the population model. (time increasing into the past)
 * 
 * N = a/(b+c*exp(d*t))
 * 
 * ie logistic growth/decay
 * @author bob
 *
 */
public class LogPopulation implements PopulationSizeModel{
	private final static double TOL=1e-8;
	private final double a,b,c,d,timeOffset;
	
	public LogPopulation(double a, double b, double c, double d,double timeOffset) {
		this.a =a;
		this.b =b;
		this.c =c;
		this.d =d;
		this.timeOffset=timeOffset;
	}

	@Override
	public double populationSize(double t) {
		t-=timeOffset;
		return a/(b+c*Math.exp(d*t));
	}

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	public double getC() {
		return c;
	}

	public double getD() {
		return d;
	}
	
	/**
	 * Not done well yet. Just really a place holder. Better guess/approximations when d*t is large/small
	 * are needed. 
	 */
	@Override
	public double generateWaitingTime(double t,double lambda, Random rand) {
		t-=timeOffset;
		//need to use Newtons method here. 
		// the form is b*x+theta*exp(d*x)+alpha=0
		double theta=c*Math.exp(d*t)/d;
		double alpha=-theta+a*Math.log(rand.nextDouble())/lambda;
		
		//we guess with b*x+alpha=0
		double x=-alpha/b;
		//now the iterations.
		double y=b*x+theta*Math.exp(d*x)+alpha;
		//the case when pop size hits zero
		if(Double.isInfinite(y))
			return 0;
		while(Math.abs(y)>TOL*x+TOL){
			double yp=b+theta*d*Math.exp(d*x);
			x=x-y/yp;
			y=b*x+theta*Math.exp(d*x)+alpha;
		}
		return x;
	}
	
}