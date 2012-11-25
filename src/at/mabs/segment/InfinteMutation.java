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

package at.mabs.segment;


/**
 * Mutation. The samples that have this mutation are stored in leafSet. 
 * 
 * 
 * @author greg
 *
 */
public class InfinteMutation implements Comparable<InfinteMutation>{
	public final double position;
	public final FixedBitSet leafSet;
	public final double weight; 

	/**
	 * @param p
	 * @param s
	 */
	public InfinteMutation(double p, FixedBitSet s) {
		this.position =p;
		leafSet =s;
		weight=1;
	}
	public InfinteMutation(double p, FixedBitSet s,double w) {
		this.position=p;
		this.leafSet=s;
		this.weight=w;
	}
	
	@Override
	public int compareTo(InfinteMutation o) {
		if (position < o.position)
			return -1;
		if (position > o.position)
			return 1;
		return 0;
	}
	
	/**
	 * gives the char representation of this mutation at sample i. can support k alleles or different mutation origins.
	 * @param i
	 * @return
	 */
	public char tranlasteAtSample(int i){
		return leafSet.contains(i)?'1':'0';
	}
}