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

import java.util.*;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
@Deprecated
public class EHHBeta implements StringStatsCollector {
	private double minFrequency=0.05;
	private double lastMax;
	private double lastP;
	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		List<InfinteMutation> muts=recorder.getMutationsSorted();
		if(muts.isEmpty())
			return;
		builder.append("EHHBeta:\n");
		double sampleSize=muts.get(0).leafSet.size();//FIXME
		double pmax=-1;
		double max=-Double.MAX_VALUE;
		for(int i=0;i<muts.size();i++){
			InfinteMutation mutation=muts.get(i);
			FixedBitSet leafs=new FixedBitSet(mutation.leafSet);
			double freq=leafs.countSetBits()/sampleSize;
			if(freq<.5)
				leafs.invert();
			double iHS=0;
			double lastF=1;
			double lastp=mutation.position;
			double length=0;
		//	System.out.println("MasterSet:"+leafs);
			for(int j=i+1;j<muts.size();j++){
				if(i==j || muts.get(j).leafSet.countSetBits()<0)
					continue;
				InfinteMutation m=muts.get(j);
				FixedBitSet nextSet=m.leafSet;
				int count=nextSet.countSetBitsMask(leafs);
				//double mf=m.leafSet.countSetBitsMask(leafs)/sampleSize;
				if(count<leafs.countSetBits()/2.0){
					nextSet=new FixedBitSet(nextSet);
					nextSet.invert();
					nextSet.and(leafs);
					count=nextSet.countSetBits();
				}
				double f=count/sampleSize;
				iHS+=(m.position-lastp)*(lastF*lastF+f*f)/2;
				leafs.and(nextSet);
				//System.out.println(iHS+"\t"+m.postion+"\t"+f);
				lastF=f;
				lastp=m.position;
				if(f<=minFrequency)
					break;
			}
			length+=lastp-mutation.position;
			//now other side...
			leafs=new FixedBitSet(mutation.leafSet);
			freq=leafs.countSetBits()/sampleSize;
			if(freq<.5)
				leafs.invert();
			double iHSleft=0;
			lastF=1;
			lastp=mutation.position;
			for(int j=i-1;j>-0;j--){
				if(i==j || muts.get(j).leafSet.countSetBits()<0)
					continue;
				InfinteMutation m=muts.get(j);
				FixedBitSet nextSet=m.leafSet;
				int count=nextSet.countSetBitsMask(leafs);
				//double mf=m.leafSet.countSetBitsMask(leafs)/sampleSize;
				if(count<leafs.countSetBits()/2.0){
					nextSet=new FixedBitSet(nextSet);
					nextSet.invert();
					nextSet.and(leafs);
					count=nextSet.countSetBits();
				}
				double f=count/sampleSize;
				iHSleft+=(lastp-m.position)*(lastF*lastF+f*f)/2;
				leafs.and(nextSet);
				//System.out.println(iHS+"\t"+m.postion+"\t"+f);
				lastF=f;
				lastp=m.position;
				if(f<=minFrequency)
					break;
			}
			double lengthr=length;
			double lengthl=mutation.position-lastp;
			length+=lengthl;
			double test=1.0-2*Math.min(lengthr,lengthl)/(length);
			builder.append(mutation.position+"\t"+iHS+"\t"+iHSleft+"\t"+lengthl+"\t"+lengthr+"\n");
			double score=iHS*iHSleft;//*lengthl*lengthr;
			if(score>max){
				max=score;
				pmax=mutation.position;
			}
		}
		//System.out.println("EHHMAX: "+pmax+" "+max);
		//builder.append("EHHMAX: "+pmax+" "+max);
		lastMax=max;
		lastP=pmax;
	}

	private void addStat(double stat,double p){
		
	}
	
	
	@Override
	public void summary(StringBuilder builder) {
		

	}
	
	public double getLastMax() {
		return lastMax;
	}
	
	public double getLastP() {
		return lastP;
	}

}
