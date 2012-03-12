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


/**
 * the bridge between 2 different SelectionData objects with the possibility of different numbers of demes. 
 * @author bob
 *
 */
public class FrequencyState {
	private double[][] frequency;
	private int currentDemeCount;
	
	public FrequencyState(int maxDemes,int currentcount){
		frequency=new double[maxDemes][2];
		this.currentDemeCount=currentcount;
	}
	
//	public FrequencyState(double[][] freqs) {
//		this.frequency=freqs;
//		this.currentDemeCount=freqs.length;
//	}

	public int getCurrentDemeCount() {
		return currentDemeCount;
	}

	public void setCurrentDemeCount(int currentDemes) {
		this.currentDemeCount =currentDemes;
	}
	
	public double getFrequency(int deme,int allele){
		return frequency[deme][allele];
	}
	
	public void setFrequency(int deme,int allele,double f){
		frequency[deme][allele]=f;
	}
	
	@Override
	public String toString() {
		String s="";
		for(double[] f:frequency){
			s+=f[1]+" ";
		}
		return "frq:"+s;
	}
	
}
