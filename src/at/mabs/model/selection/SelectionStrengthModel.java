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
package at.mabs.model.selection;

/**
 * gives the selection strength at a given time. We assume Diploid populations and ignore
 * heterozygoe selection in the haploid case.
 * 
 * @author bob
 * 
 */
public interface SelectionStrengthModel{
	/**
	 * this should always be symmetric in alleleA and alleleB. ie swapping them should not
	 * change the selection coefficient. 
	 * @param deme
	 * @param alleleA
	 * @param alleleB
	 * @param time
	 * @return
	 */
	public double getStrength(int alleleA,int alleleB,double time);
	
	public static class Simple implements SelectionStrengthModel{
		private final double aa,aA,AA;
		
		public Simple(double aa, double aA, double AA) {
			this.aa =aa;
			this.aA =aA;
			this.AA =AA;
			//System.out.println("CREATED:"+this+"\t"+AA);
		}



		@Override
		public double getStrength(int alleleA, int alleleB, double t) {
			//System.out.println("QUERRY:"+this+"\t"+AA);
			if(alleleA==1 && alleleB==1){
				return AA;
			}
			if(alleleA==0 && alleleB==0){
				return aa;
			}
			return aA;
		}
	}
}
