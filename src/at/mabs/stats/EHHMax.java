package at.mabs.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.cuckoo.CHashMap;

/**
 * |Does the iEHH thing based on the most frequent haplotype.
 * 
 * @author bob
 * 
 */

public class EHHMax extends StatsCollectorAdapter {
	private int binCount = 24;
	private String name = "EHHMax";
	private int bits=10;

	public EHHMax() {
	}

	public EHHMax(String name) {
		this.name = name;
	}

	public EHHMax(String name, String bins) {
		this.name = name;
		this.binCount = Integer.parseInt(bins);
	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {

		int[] counts = new int[binCount];
		double[] values = new double[binCount];
		if (recorder.getMutationsSorted().size() <bits)
			return values;

		FixedBitSet[] seqAsBits = transposeToSequences(recorder.getMutationsSorted());
		double[] maxF=subStringMaxFrequency(seqAsBits);//note that this destroys the seqAsBits
		List<InfinteMutation> muts=recorder.getMutationsSorted();
		for(int i=0;i<maxF.length;i++){
			double start=muts.get(i).position;
			double end=muts.get(i+bits).position;
			double v=(start+end)/2;
			int index=(int)(v*binCount);
			counts[index]++;
			values[index]=maxF[i]*(end-start);//Math.max(maxF[i]*(end-start),values[index]);
		}
		
		for(int i=0;i<counts.length;i++){
			if(counts[i]!=0)
			values[i]/=counts[i];
		}
		return values;
	}

	private FixedBitSet[] transposeToSequences(List<InfinteMutation> muts) {
		FixedBitSet[] bits = new FixedBitSet[muts.get(0).leafSet.getTotalLeafCount()];

		for (int i = 0; i < bits.length; i++) {
			bits[i] = new FixedBitSet(muts.size());
		}

		int bitCount = 0;
		for (InfinteMutation im : muts) {
			for (int i = 0; i < bits.length; i++) {
				if (im.leafSet.contains(i))
					bits[i].set(bitCount);
			}
			bitCount++;
		}
		return bits;
	}

	private double[] subStringMaxFrequency(FixedBitSet[] strings) {
		final long mask = (1l<<bits)-1;// last ten bits.
		int[] counts = new int[(int) mask + 1];
		double[] results = new double[strings[0].getTotalLeafCount() - bits];
		//System.out.println("Counts:"+counts.length);
		for (int r = 0; r < results.length; r++) {
			Arrays.fill(counts, 0);
			double freq = 0;
			int hap=-1;
			double freq2=0;
			for (int i = 0; i < strings.length; i++) {
				int value = (int) (strings[i].lastBitsAsLong() & mask);
				
				counts[value]++;
				//System.out.println("Value:"+Integer.toBinaryString(value)+"\t"+counts[value]);
				if (counts[value] > freq) {
					if(value!=hap){
						freq2=freq;
					}
					hap=value;
					freq = counts[value];
				}
				strings[i].shiftRight();
			}
			
			results[r]=(freq)/strings.length;
		}
		return results;
	}

}
