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
package at.mabs.external;

import java.util.Random;


import at.mabs.cern.jet.random.Binomial;
import at.mabs.util.random.Random64;

/**
 * simple direct simulation of a steping stone model with dispersion
 * 
 * Parameters:
 * 
 * no of demes. Size of demes. migration rate between adjacent demes Selection parameters, s and mu
 * dispersion migration rate from a meta deme
 * 
 * 
 * we are going to assume hapliod population. We need to track the number of orgins.
 * 
 * we assume biallelic locus.
 * 
 * @author greg
 * 
 */
public class SteppingStones {


	private int demeCount = 100000;//L
	private double mnear = .05;//m
	private double mfar = 1e-6;//m_l
	private double msame = 1.0 - mnear * 2 - mfar;
	private int N = 1000;//K
	private double s = 0.05;
	private double sp1 = s + 1;// s+1;
	private double mu = 0e-7;//u

	private boolean noDrift=false;

	private double[] frequency =new double[demeCount];
	private double[] selection =new double[demeCount];// f after selection.

	private Random random =new Random64();
	private Binomial binom =new Binomial(random);

	private double eps =1e-6;//1.0 / N;

	private int gen;

	
	
	/**
	 * 
	 * @return true if allele is fixed
	 */
	public boolean step() {
		gen++;
		double totalF =0;
		for (int i =0; i < demeCount; i++) {
			double x =frequency[i];

			x =(x * sp1 + (1 - x) * mu) / (x * sp1 + (1 - x));

			// now mutation

			// System.out.println("Dood:\t"+(x-xp));
			totalF +=x;
			selection[i] =x;
		}

		// now ss migration. note total migration into a deme is 2mnear+mfar.
		// total migariton out of a deme is 2mnear+demeCount*mfar
		totalF /=demeCount;
		for (int i =1; i < demeCount - 1; i++) {
			double xnear =selection[i - 1] + selection[i + 1];
			double x =mnear * xnear + totalF * mfar + selection[i] * msame;
			frequency[i] =x;

		}

		// now the 2 edges.
		frequency[0] =(selection[1] + selection[demeCount - 1]) * mnear + totalF * mfar + selection[0] * msame;
		frequency[demeCount - 1] =(selection[0] + selection[demeCount - 2]) * mnear + totalF * mfar + selection[demeCount - 1] * msame;

		// //now we add drift.
		if (!noDrift) {
			totalF =0;
			for (int i =0; i < demeCount; i++) {
				double x =(double) binom.generateBinomial(N, frequency[i]) / N;
				frequency[i] =x;
				totalF +=x;
			}
			totalF /=demeCount;
		}
		// System.out.println();
		 System.out.println(totalF+"\tNA\t"+gen);
		if (totalF == 0)
			init();
		return totalF >= 1 - eps;
	}

	public void init() {
		int d =random.nextInt(demeCount);
		frequency[d] =1.0 / N;
		gen =0;
	}

	public static void main(String[] args) {
		double sum=0;
		double sum2=0;
		int n=1;
		for (int i =0; i < n; i++) {
			SteppingStones ss =new SteppingStones();
			while (!ss.step())
				;
			sum+=ss.gen;
			sum2+=(double)ss.gen*ss.gen;
			//System.out.println(i);
		}
		double ave=sum/n;
		double var=sum2/n-ave*ave;
	//	System.out.println("SS1:"+ave+"\t"+var);
	}

}
