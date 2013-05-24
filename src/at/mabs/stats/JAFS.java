package at.mabs.stats;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;

public class JAFS extends StatsCollectorAdapter {
	//private FixedBitSet groupa;
	//private FixedBitSet groupb;
	private String name;
	
	
	public JAFS() {
		name="jafs";
	}
	
	public JAFS(String name) {
		this.name=name;
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		int asize=mask.countSetBits();
		int bsize=mask2.countSetBits();
		//System.out.println("\nAsize:"+asize+"\t"+bsize);
		//assert false;
		double[] result=new double[(asize+1)*(bsize+1)];
		
		for(InfinteMutation m:recorder.getMutationsUnsorted()){
			int a=m.leafSet.countSetBitsMask(mask);
			int b=m.leafSet.countSetBitsMask(mask2);
			result[a+b*(asize+1)]+=m.weight;
		}
		
		return result;
	}

	
	
//	@Override
//	public void setLeafMask(FixedBitSet mask) {
//		groupa=mask;
//		//System.out.println("Mask1:"+mask);
//	}
//
//	@Override
//	public void setSecondLeafMask(FixedBitSet mask) {
//		groupb=mask;
//		//System.out.println("Mask2:"+mask);
//	}

	@Override
	public String[] getStatLabels() {
		int asize=mask.countSetBits();
		int bsize=mask2.countSetBits();
		String[] names=new String[(asize+1)*(bsize+1)];
		for(int i=0;i<=asize;i++){
			for(int j=0;j<=bsize;j++){
				names[i+j*(asize+1)]=name+"_"+i+","+j;
			}
		}
		return names;
	}
}
