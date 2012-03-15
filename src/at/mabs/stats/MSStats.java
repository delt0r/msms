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
	
	private int lengthBeforePol = -1;
	
	@Override
	public int getLengthBeforePol()
	{
		return this.lengthBeforePol;
	}
	
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
		List<InfinteMutation> mutations=recorder.getMutations();
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
		
		this.lengthBeforePol = builder.length();
		
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
	public void pairShuffle(SegmentEventRecoder recorder, StringBuilder builder, int lengthBeforePol)
	{
		List<InfinteMutation> mutations=recorder.getMutations();
		int noLeaves = mutations.get(0).leafSet.size();
		if(noLeaves % 2 != 0)
		{
			System.err.println("No Sequences is not a multiple of 2... Do nothing");
			return;
		}
		
		int k = lengthBeforePol-1;
		//System.out.println("*** Current Position: "+k+"\t"+builder.charAt(k));
		
		int offset = mutations.size() + 1;
		while( k < builder.length() - offset)
		{
			for(int m = 0; m < mutations.size(); ++m)
			{
				k++;
				//System.out.println("Current Position: "+k+"\t"+builder.charAt(k));
				double r = Math.random();
				if(r < 0.5)
				{
					String replace1 = builder.substring(k, k + 1);
					String replace2 = builder.substring(k + offset, 
							k + 1 + offset);

					builder.replace(k, k + 1, replace2);
					builder.replace(k + offset, k + offset + 1, replace1);
				
				}
				
			}	
		k = k + offset+1;
		}
		
	}
	
	
	@Override
	public void noAncestralState(SegmentEventRecoder recorder, StringBuilder builder, int lengthBeforePol)
	{
		List<InfinteMutation> mutations=recorder.getMutations();
		int noLeaves = mutations.get(0).leafSet.size();
		/*if(noLeaves % 2 != 0)
		{
			System.err.println("No Sequences is not a multiple of 2... Do nothing");
			return;
		}*/
		
		int k = lengthBeforePol-1;
		//System.out.println("*** Current Position: "+k+"\t"+builder.charAt(k));
		
		int offset = mutations.size() + 1;
		
		for(int m = 0; m < mutations.size(); ++m)
		{
			k = lengthBeforePol  + m;	
			int ones = 0;
			for(int i = 0; i < noLeaves; ++i)
			{
				//System.out.println("Character at position "+k+" is "+builder.charAt(k));
				
				if(builder.charAt(k) == '1')
				{
					ones++;
				}
				k += offset;
			}
			
			//System.out.println("Ones are: " + ones);
			
			if(ones > noLeaves / 2.)
			{
				//System.out.println("CHANGE");
				k = lengthBeforePol + m;	
				
				for(int i = 0; i < noLeaves; ++i)
				{
					if(builder.charAt(k) == '1')
					{
						builder.replace(k, k+1, "0");
					}
					else if(builder.charAt(k) == '0')
					{
						builder.replace(k, k+1, "1");
					}
					k += offset;
				}
			}
			
		}
		
	}
	
	@Override
	public void summary(StringBuilder builder) {
		//noop
	}
}
