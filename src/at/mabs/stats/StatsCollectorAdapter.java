package at.mabs.stats;

import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;

public abstract class StatsCollectorAdapter implements StatsCollector {
	private double[] sum;
	private double[] sum2;
	private int n=0;
	protected FixedBitSet mask;
	protected FixedBitSet mask2;
	
	@Override
	public final double[] collectStats(SegmentEventRecoder recorder) {
		double[] stats=collectStatsImp(recorder);
		if(sum==null){
			sum=new double[stats.length];
			sum2=new double[stats.length];
		}
		assert sum.length==stats.length;
		n++;
		for(int i=0;i<stats.length;i++){
			sum[i]+=stats[i];
			sum2[i]+=stats[i]*stats[i];
		}
		return stats;
	}
	
	public abstract double[] collectStatsImp(SegmentEventRecoder recorder);
	
	@Override
	public double[] summaryStats() {
		//System.out.println("SumStats"+this+"\t"+n);
		assert n!=0;
		if(n==1){
			return sum;
		}
		double[] results=new double[sum.length*2];
		for(int i=0;i<sum.length;i++){
			double mu=sum[i]/(n);
			results[i+sum.length]=Math.sqrt(sum2[i]/(n-1)-n*mu*mu/(n-1));
			results[i]=mu;
		}
		
		
		return results;
	}

	@Override
	public String[] getStatLabels() {
		
		return new String[0];
	}

	@Override
	public void init() {
		//System.out.println(this+"\tinit"+Thread.currentThread().getStackTrace()[0]);
		sum=null;
		sum2=null;
		n=0;
	}

	@Override
	public void setLeafMask(FixedBitSet mask) {
		//System.out.println("MASK:"+mask);
		this.mask=mask;
	}

	@Override
	public void setSecondLeafMask(FixedBitSet mask) {
		this.mask2=mask;
		//System.out.println("MAS2:"+mask);
		//System.out.println(mask);
	}

}
