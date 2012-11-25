package at.mabs.stats;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

import java.util.*;
/**
 * returns a single number. 
 * @author bob
 *
 */
public class OverFst extends StatsCollectorAdapter {
	private FixedBitSet inMask;
	private FixedBitSet outMask;
	private FixedBitSet allMask;
	private String name;
	
	public OverFst() {
		name="fst";
	}
	
	public OverFst(String name) {
		this.name=name;
	}
	
	@Override
	public void setLeafMask(FixedBitSet mask) {
		inMask=mask;
	}
	
	@Override
	public void setSecondLeafMask(FixedBitSet mask) {
		outMask=mask;
		this.allMask=new FixedBitSet(mask.getTotalLeafCount());
		allMask.or(inMask);
		allMask.or(outMask);	
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		List<InfinteMutation> mutations=recorder.getMutationsSorted();
		double inDiff=0;
		double outDiff=0;
		double allDiff=0;
		for(InfinteMutation m:mutations){
			inDiff+=m.leafSet.getPairwiseDifferenceMask(inMask);
			outDiff+=m.leafSet.getPairwiseDifferenceMask(outMask);
			allDiff+=m.leafSet.getPairwiseDifferenceMask(allMask);
		}
		double between=allDiff;//-inDiff-outDiff;
		double inSize=inMask.countSetBits();
		double outSize=outMask.countSetBits();
		double allSize=allMask.countSetBits();
		between/=(allSize-1)*allSize/2;//inSize*outSize;
		double in=inDiff;
		in/=inSize*(inSize)/2;
		double out=outDiff;
		out/=outSize*(outSize-1)/2;
		double w=inSize/(inSize+outSize);
		double[] result= {1.0/((between-(in*w+(1-w)*out))/between)};
		return result;
	}
	
	

	@Override
	public String[] getStatLabels() {
		return new String[] {name};
	}
	
}
