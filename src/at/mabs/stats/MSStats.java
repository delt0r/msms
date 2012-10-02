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
package at.mabs.stats;


import java.util.List;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;
/**
 * ms compatable output generator. 
 * @author greg
 *
 */
public class MSStats implements StringStatsCollector {
	private boolean printSegSites=true;
	private boolean loption=false;
	
	
	
	
	
	public boolean isPrintSegSites() {
		return printSegSites;
	}
	
	public void setPrintSegSites(boolean printSegSites) {
		this.printSegSites = printSegSites;
	}
	
	public void setLoption(boolean loption) {
		this.loption =loption;
	}
	
	public boolean isLoption() {
		return loption;
	}
	
	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		List<InfinteMutation> mutations=recorder.getMutationsSorted();
		//builder.append("//\n");//this is a start sim output delimiter... so its not for here..
		if (recorder.isTrackTrees())
			recorder.treeStrings(builder);
		if(isLoption()){
			double[] treeL=recorder.getTreeLengthDataAt(.5);
			builder.append("time:\t").append(Util.defaultFormat.format(treeL[0])).append('\t').append(Util.defaultFormat.format(treeL[1])).append('\n');
		}
		builder.append("segsites: " + mutations.size() + "\n");
		if (mutations.isEmpty() || !printSegSites) {
			builder.append('\n');
			return;
		}
		builder.append("positions: ");
		for (int m = 0; m < mutations.size(); m++) {
			InfinteMutation mutation = mutations.get(m);
			builder.append(Util.defaultFormat.format(mutation.position));
			builder.append(' ');

		}
		builder.append('\n');
		
		int noLeaves = mutations.get(0).leafSet.size();
		for (int i = 0; i < noLeaves; i++) {
			for (int m = 0; m < mutations.size(); m++) {
				InfinteMutation mutation = mutations.get(m);
				builder.append(mutation.tranlasteAtSample(i));
			}
			builder.append('\n');

		}
		builder.append('\n');
		
	}
	
	
	
	
	@Override
	public void summary(StringBuilder builder) {
		//noop
	}
}
