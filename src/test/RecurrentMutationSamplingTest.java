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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import at.mabs.cern.jet.random.Binomial;
import at.mabs.util.random.Random64;


/**
 * Just a test example...
 * 
 * @author bob
 * 
 */
public class RecurrentMutationSamplingTest {

	public static void main(String[] args) {
		List<Integer> f =new ArrayList<Integer>();
		Binomial binomial =new Binomial(new Random64());
		int N =10000;
		double SA =2000.0/N;
		double mu =0.1 / (N);
		System.out.println(mu);
		int samples=2;
		
		int hc=0;
		int shc=0;
		int reps =1;
		for (int r =0; r < reps; r++) {
			f.clear();
			int sa =sum(f);
			while (sa < N) {
				if(f.isEmpty()){
					f.add(1);
					sa=1;
				}
				
				double x =(double) sa / N;
				x =(1 + SA) * x / ((1 + SA) * x + (1 - x));
				// now "add" mutations from last generation.
				int newMutes =binomial.generateBinomial(N - sa, mu);
				
				// now the total number of selected in the next genreation.
				int next =binomial.generateBinomial(N - newMutes, x);

				// now for the partions of catagories.
				for (int i =0; i < f.size() - 1 && next > 0; i++) {
					int n =f.get(i);
					double p =(double) n / sa;
					n =binomial.generateBinomial(next, p);
					assert n >= 0;
					next -=n;
					assert next >= 0;
					f.set(i, n);
				}
				// the last one gets the rest
				//if (f.size() > 0)
					f.set(f.size() - 1, next);
				// clear zeros
				ListIterator<Integer> iter =f.listIterator();
				while (iter.hasNext()) {
					if (iter.next() == 0)
						iter.remove();
				}

				for (int i =0; i < newMutes; i++) {
					f.add(Integer.valueOf(1));
				}
				
				sa =sum(f);
				System.out.println(f.size() + "\t" + sa + "\t" + f);
			}
			System.out.println(r+":"+f.size() + "\t" + sa + "\t" + f);
			if(f.size()<2){
				hc++;
			}
			//now pull samples..
			
			for(int i=0;i<f.size();i++){
				int n=binomial.generateBinomial(samples, (double)f.get(i)/N);
				if(n==samples){
					shc++;
					break;
				}
				if(n!=0){
					break;
				}
			}
		}
		System.out.println("HC%:"+((double)hc/reps)+" "+((double)shc/reps));
	}

	private static int sum(List<Integer> d) {
		int a =0;
		for (int i : d)
			a +=i;
		return a;
	}
}
