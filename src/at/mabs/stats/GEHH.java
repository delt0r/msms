package at.mabs.stats;

import java.util.*;

import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
/**
 * so we use a kinda iEHH score thing without the divide thing... Divides do evil to statistics. 
 * 
 * This is a greedy version. We take the next snp that gives the highest frequency haplotype with the previously chosen snps fixed. 
 * This frequency is integrated over the length to the cutoff of 5% frequency. 
 * @author bob
 *
 */
public class GEHH extends StatsCollectorAdapter {
	private String name;
	private int binCount=10;
	
	public GEHH() {
	}
	
	public GEHH(String n){
		this.name=n;
	}
	
	public GEHH(String n,String binCount){
		this(n);
		this.binCount=Integer.parseInt(binCount);
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		//so for each snp we go in both directions.
		double[] data=new double[binCount];
		List<InfinteMutation> muts=recorder.getMutationsSorted();
		if(muts.size()==0){
			return data;
		}
		int[] counts=new int[binCount];
		
		int leafCount=muts.get(0).leafSet.getTotalLeafCount();
		for(int i=0;i<muts.size();i++){
			InfinteMutation currentMutation=muts.get(i);
			//first forwards 
			FixedBitSet mask=new FixedBitSet(leafCount);
			mask.invert();//set all!
			int maskCount=mask.countSetBits();
			double iEHH=0;
			double fs=0;
			double lp=currentMutation.position;
			for(int j=i;j<muts.size();j++){
				//so what do we keep?
				InfinteMutation nextMut=muts.get(j);
				FixedBitSet leafSet=nextMut.leafSet;
				int c=leafSet.countSetBitsMask(mask);
				if(c<maskCount/2.0){
					c=maskCount-c;
					mask.nand(leafSet);
				}else{
					mask.and(leafSet);
				}
				double f=(double)c/leafCount;
				iEHH+=(nextMut.position-lp)*f;
				lp=nextMut.position;
				maskCount=mask.countSetBits();
				if(f<.01 || maskCount<=1){
					break;
				}
			}
			//int bin=(int)(currentMutation.position*binCount);
			//data[bin]=Math.max(data[bin], iEHH);
			fs=iEHH;
			
			
			mask=new FixedBitSet(leafCount);
			mask.invert();//set all!
			maskCount=mask.countSetBits();
			lp=currentMutation.position;
			iEHH=0;
			for(int j=i;j>=0;j--){
				//so what do we keep?
				InfinteMutation nextMut=muts.get(j);
				FixedBitSet leafSet=nextMut.leafSet;
				int c=leafSet.countSetBitsMask(mask);
				if(c<maskCount/2.0){
					c=maskCount-c;
					mask.nand(leafSet);
				}else{
					mask.and(leafSet);
				}
				double f=(double)c/leafCount;
				iEHH+=(lp-nextMut.position)*f;
				lp=nextMut.position;
				maskCount=mask.countSetBits();
				if(f<.01 || maskCount<=1){
					break;
				}
			}
			fs*=iEHH;
			int bin=(int)(binCount*currentMutation.position);
			data[bin]+=fs;
			counts[bin]++;
		}
		for(int i=0;i<data.length;i++){
			if(counts[i]>0)
				data[i]/=counts[i];
		}
		return data;
	}

}
