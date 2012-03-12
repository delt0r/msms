package at.mabs.stats;

import java.util.Random;

import at.mabs.config.CommandLineMarshal;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.random.Random64;
/**
 * hacky way to have an easy model in the framework. Should not really be used 
 * 
 * Just generates a random number between the given parameters n times and then returns the mean and std
 * @author bob
 *
 */
public class UniformStat extends StatsCollectorAdapter {
	private String name;
	private double s,l,n;
	private Random random=new Random64();
	
	
	
	public UniformStat() {
		name="random";
	}
	
	public UniformStat(String name) {
		this.name=name;
	}
	
	public UniformStat(String name,String start,String end,String n) {
		// TODO Auto-generated constructor stub
		this.name=name;
		this.s=Double.parseDouble(start);
		this.l=Double.parseDouble(end);
		this.n=Double.parseDouble(n);
		//System.out.println(a+"\tb:"+b);
		
		//l=Math.sqrt(12*l);
		//s=s-l/2;
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		String[] params=CommandLineMarshal.HACK_PARAMS;//EVIL
		this.s=Double.parseDouble(params[0]);
		this.l=Double.parseDouble(params[1]);
		this.n=Double.parseDouble(params[2]);
//		double sum=0;
//		double sum2=0;
//		for(int i=0;i<n;i++){
//			double v=random.nextDouble()*l+s;
//			sum+=v;
//			sum2+=v*v;
//		}
//		double mean=sum/n;
//		double var=sum2/n-mean*mean;
		double[] r={s,l};//mean,var};
	
		return r;
	}
	
	

	@Override
	public String[] getStatLabels() {
		
		return new String[] {"mean","var"};
	}

	@Override
	public void setLeafMask(FixedBitSet mask) {
	}

	@Override
	public void setSecondLeafMask(FixedBitSet mask) {
	}

}
