package at.mabs.stats;

import java.util.List;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

public class ThetaPi extends StatsCollectorAdapter {
	private FixedBitSet mask;
	private String name;
	private int bins=0;

	
	
	public ThetaPi() {
		name="t_pi";
	}
	
	public ThetaPi(String name) {
		this.name=name;
	}
	
	public ThetaPi(String name,String bin) {
		this.name=name;
		this.bins=Integer.parseInt(bin);
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		double segSiteCount=0;//recorder.getMutations().size();
		double[] results=new double[bins+1];
		
		double binDelta=1.0/bins;//FIXME assume 0-1 locus!
		int binIndex=1;
		
		double maskCount=mask.countSetBits();
		double pairs=maskCount*(maskCount-1)/2;
		double delta=1.0/pairs;
		
		
		
		List<InfinteMutation> mutations=recorder.getMutationsUnsorted();
		for(InfinteMutation m:mutations){
			results[0]+=m.leafSet.getPairwiseDifferenceMask(mask)*delta;
			if(bins>0){
				if(binIndex*binDelta<m.position){
					binIndex++;
				}
				results[binIndex]+=m.leafSet.getPairwiseDifferenceMask(mask)*delta/binDelta;
			}
		}
		
		
		return results;
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
