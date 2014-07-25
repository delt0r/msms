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

import java.util.Arrays;
import java.util.Random;

import at.mabs.cern.jet.random.Binomial;
import at.mabs.util.random.Random64;

public class SteppingStones2 {
	private int demeCount = 3;
	private double mnear = 1e-2;
	private double mfar = 1e-7;
	// private double msame = 1.0 - mnear * 2 - mfar;// 1-2mnear-mfar
	private int N = 10000;
	private double s = 0.01;
	private double sp1 = s + 1;// s+1;

	private double mu = 1e-6;

	private boolean noDrift;

	private double[][] frequency = new double[demeCount][2];
	private double[][] selection = new double[demeCount][2];// f after
	private double[] classTots = new double[2];

	private int classSize = 2; // class zero is the wild type.

	private Random random = new Random64();
	private Binomial binom = new Binomial(random);

	private double eps = 1.0 / N;
	
	private int generation=0;

	
	public SteppingStones2() {
		init();
	}
	
	public void init() {
		for(int d=0;d<demeCount;d++){
			frequency[d][0]=1;
		}
		int d = random.nextInt(demeCount);
		frequency[d][1] = 1.0 / N;
		frequency[d][0]=1-1.0/N;
		generation=0;
	}
	
	private double[] doubleSize(double[] array) {
		double[] n = new double[array.length * 2];
		System.arraycopy(array, 0, n, 0, array.length);
		return n;
	}

	private void migration1d(double[][] freq, double[][] freqResult, double[] classTotals) {
		// the intermediate step for 1d migration... makes adding 2d easier.
		double mii = 1 - 2 * mnear - mfar;
		for (int d = 1; d < demeCount-1; d++) {
			for (int c = 0; c < classSize; c++) {
				double nf = freq[d][c] * mii + (freq[d - 1][c] + freq[d + 1][c]) * mnear + classTotals[c] * mfar;
				freqResult[d][c] = nf;
			}
		}
		//edges
		for (int c = 0; c < classSize; c++) {
			double nf = freq[0][c] * mii + (freq[demeCount-1][c] + freq[1][c]) * mnear + classTotals[c] * mfar;
			freqResult[0][c] = nf;
			nf = freq[demeCount-1][c] * mii + (freq[0][c] + freq[demeCount-2][c]) * mnear + classTotals[c] * mfar;
			freqResult[demeCount-1][c] = nf;
		}
		
	}

	private void addClass(int deme){
		//first do we have enought space
		if(classSize==classTots.length){
			for(int d=0;d<demeCount;d++){
				frequency[d]=doubleSize(frequency[d]);
				selection[d]=doubleSize(selection[d]);
			}
			classTots=doubleSize(classTots);
			//System.out.println("Double");
		}
		
		classSize++;
		frequency[deme][classSize-1]=1.0/N;
		classTots[classSize-1]=1.0/N;
	}
	
	private void removeClass(int classId){
		
		if(classSize==2){
			init();
			return;
		}
			
		//use a swap and clear method.
		for(int d=0;d<demeCount;d++){
			frequency[d][classId]=frequency[d][classSize-1];
			frequency[d][classSize-1]=0;
		}
		classSize--;
		//System.out.println("Delete");
		
	}
	
	public boolean step() {
		//System.out.println("Step");
		generation++;
		Arrays.fill(classTots, 0);
		for (int d = 0; d < demeCount; d++) {
			selection[d][0]=frequency[d][0];
			classTots[0] += selection[d][0] / demeCount;
			for (int c = 1; c < classSize; c++) {
				selection[d][c] = frequency[d][c] * sp1;
				classTots[c] += selection[d][c] / demeCount;
			}
		}
		migration1d(selection, frequency, classTots);

		Arrays.fill(classTots, 0);
		double totalTotal=0;
		// so now we have the wilde type in class one.
		int size=classSize;
		for (int d = 0; d < demeCount; d++) {
			double norm=0;
			for (int c = 0; c < size; c++) {
				norm+=frequency[d][c];
			}
			assert norm>0;
			double cumlativeP=0;
			int count=0;
			double alleleTot=0;
			for (int c = 1; c < size && N-count>0; c++) {
				double p=frequency[d][c]/norm;
				int n=binom.generateBinomial(N-count, p/(1-cumlativeP));
				assert n<=N-count:n;
				assert n>=0:n+"\tP1:"+(N-count)+"\tp2:"+p/(1-cumlativeP);
				
				cumlativeP+=p;
				
				//assert cumlativeP<=1:(cumlativeP)+"\t"+c+"\t"+norm;
				
				count+=n;
				assert count<=N:count;
				frequency[d][c]=(double)n/N;
				//System.out.println(frequency[d][c]+"\t"+p+":p "+n+"\tc:"+count);
				alleleTot+=frequency[d][c];
				classTots[c]+=frequency[d][c]/demeCount;
			}
			
			//now do we have a "muatation" we assume that the proablity is really really small. 
			double mutP=(frequency[d][0]/norm)*mu*(N-count)/(1-cumlativeP);
			//int mCount=binom.generateBinomial(N-count, mutP/(1-cumlativeP));
			if(random.nextDouble()<mutP){
				addClass(d);
				alleleTot+=1.0/N;
			}
			frequency[d][0]=1-alleleTot;
			classTots[0]+=(1-alleleTot)/demeCount;
			totalTotal+=alleleTot;
		}
		totalTotal/=demeCount;
		
		//now we can use the classTot to remove classes....
		if((generation&0x0)==0){
			size=classSize;
			double sum=0;
			for(int c=0;c<size;c++){
				//System.out.print(classTots[c]+" ");
				sum+=classTots[c];
				if(classTots[c]==0)
					removeClass(c);
			}

			//System.out.println(sum);
		}

		System.out.println(totalTotal+"\t"+(classSize-1)+"\t"+generation);
		if(totalTotal>=1-1.0/N)
			return true;
		return false;

	}

	public static void main(String[] args) {
		double sum=0;
		double sum2=0;
		int n=1;
		for (int i =0; i < n; i++) {
			SteppingStones2 ss =new SteppingStones2();
			while (!ss.step())
				;
			//sum+=ss.generation;
			//sum2+=(double)ss.generation*ss.generation;
			//System.out.println(i);
		}
		//double ave=sum/n;
		//double var=sum2/n-ave*ave;
		//System.out.println(ave+"\t"+var);
	}
}
