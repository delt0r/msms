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
package test;

import at.mabs.cern.jet.random.Binomial;
import at.mabs.util.random.RandomGenerator;

public class CoEvol {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double p=Double.parseDouble(args[0]);
		double q=Double.parseDouble(args[1]);
		double sh=Double.parseDouble(args[2]);
		double sp=Double.parseDouble(args[3]);
		
		double mu=1e-4;
		int N=10000;
		int rep=Integer.parseInt(args[4]);
		
		Binomial bin=RandomGenerator.getBinomial();
		
		for(int i=0;i<rep;i++){
			double shqn=1-sh*q;
			double pn=1-p;
			double qn=1-q;
			double pp=p*shqn;
			double b=pp+pn*(1-sh*qn);
			
			pp/=b;
			
			
			
			double sppnn=1-sp*pn;
			double qp=q*sppnn;
			b=qp+qn*(1-sp*p);
			qp/=b;
			
			System.out.println(pp+"\t"+qp);
			
			q=qp*(1-mu)+(1-qp)*mu;
			p=pp*(1-mu)+(1-pp)*mu;
			
			p=(double)bin.generateBinomial(N, p)/N;
			//q=(double)bin.generateBinomial(N, q)/N;
		}

	}

}
