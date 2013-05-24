package at.mabs.stats;

import java.util.List;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

public class TjD extends StatsCollectorAdapter {
	private FixedBitSet mask;
	private String name;
	
	
	public TjD(){
		name="tjd";
	}
	
	public TjD(String name) {
		this.name=name;
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		int segSiteCount=0;//recorder.getMutations().size();
		double pairSum=0;
		int maskCount=mask.countSetBits();
		
		List<InfinteMutation> mutations=recorder.getMutationsSorted();
		for(InfinteMutation m:mutations){
			int setCount=m.leafSet.countSetBitsMask(mask);
			if(setCount!=0 || setCount!=maskCount)
				segSiteCount++;
			pairSum+=m.leafSet.getPairwiseDifferenceMask(mask);
		}
		
		double tw=segSiteCount/Util.haromicNumber(maskCount-1);
		double pairs=(double)maskCount*(maskCount-1)/2;
		double pi=pairSum/pairs;
		
		double td=Util.tjD(tw, pi, maskCount);
		
		double[] r={td};
		
		return r;
	}


	@Override
	public String[] getStatLabels() {
		return new String[] {name};
	}

	@Override
	public void setLeafMask(FixedBitSet mask) {
		this.mask=mask;
	}

	@Override
	public void setSecondLeafMask(FixedBitSet mask) {
		//noop
	}

}
