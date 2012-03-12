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

public class Solver {
	double a, b, c, y;

	public double function(double x) {
		return a * x + b * Math.exp(c * x) - y;
	}

	public double dfunction(double x) {
		return a + c * b * Math.exp(c * x);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		double a =10000*100, b =10000, c =100-b, d =-.0001, t =0;
		while (true) {
			double theta =c* Math.exp(d * t) / d;
			double ry=Math.log(Math.random());
			double alpha =-theta + a * ry;

			// we guess with b*x+alpha=0
			double x = -a*ry/(b+c*Math.exp(d*t));//-alpha/b;
			System.out.println("Guess:" + x);
			// now the iterations.
			double y =b * x + theta * Math.exp(d * x) + alpha;
			System.out.println(x + "\t" + y);
			double tol=0.00001;
			while (Math.abs(y) > tol*Math.abs(x)+tol) {
				double yp =b + d*theta* Math.exp(d * x);
				x =x - y / yp;
				y =b * x + theta * Math.exp(d * x) + alpha;

				System.out.println("i:" + x + "\t" + y);
			}
			System.out.println(x + "\t" + y);
		}
	}

}
