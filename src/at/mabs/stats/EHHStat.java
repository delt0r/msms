package at.mabs.stats;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import java.util.*;

public class EHHStat extends StatsCollectorAdapter {
	private int binCount = 10;
	private FixedBitSet mask;
	private String name;

	public EHHStat() {
	}

	public EHHStat(String n) {
		this.name = n;
	}

	public EHHStat(String n, String binCount) {
		this(n);
		this.binCount = Integer.parseInt(binCount);
	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		List<InfinteMutation> mutations = recorder.getMutationsSorted();
		double[] bins = new double[binCount];// FIXME assuming 0-1 locus.
		int[] binC = new int[binCount];

		int total = mask.countSetBits();

		for (int i = 0; i < mutations.size(); i++) {
			InfinteMutation mutation = mutations.get(i);
			FixedBitSet muSet = new FixedBitSet(mutation.leafSet);
			muSet.and(mask);
			if (muSet.countSetBits() == 0)
				continue;// not segregating in given group
			FixedBitSet notMuSet = new FixedBitSet(mutation.leafSet);
			notMuSet.invert();
			notMuSet.and(mask);

			double ione = 0;
			double izero = 0;
			int pzeroCount = notMuSet.countSetBits();
			int ponesCount = muSet.countSetBits();
			double lastp = mutation.position;
			for (int j = i + 1; j < mutations.size(); j++) {
				// add a new mutation.
				InfinteMutation next = mutations.get(j);

				FixedBitSet newMu = new FixedBitSet(next.leafSet);
				newMu.and(mask);
				if (newMu.countSetBits() == 0)
					continue;// not segregating in given group

				muSet.and(newMu);
				newMu.invert();
				newMu.and(mask);
				notMuSet.and(newMu);

				int zeroCount = notMuSet.countSetBits();
				int onesCount = muSet.countSetBits();
				
				ione += (ponesCount+onesCount) * (next.position - lastp)/2;
				izero += (pzeroCount+zeroCount) * (next.position - lastp)/2;

				

				double zeroR = (double) zeroCount / total;
				double onesR = (double) onesCount / total;
				if (Math.max(zeroR, onesR) < .01) {
					double l = next.position - mutation.position;
					double c = mutation.position ;//+ l / 2;
					int index = (int) (c * binCount);
					bins[index] += Math.max(izero,ione);///Math.min(izero, ione);//Math.max(ione, izero);// Math.max(bins[index],
															// l);
					binC[index]++;
					// System.out.println(c+"\t"+l);
					break;
				}
				pzeroCount = zeroCount;
				ponesCount = onesCount;
				lastp = next.position;
			}
		}
		for (int i = 0; i < binCount; i++) {
			if (binC[i] != 0)
				bins[i] /= binC[i];
		}

		return bins;
	}

	@Override
	public String[] getStatLabels() {
		// TODO Auto-generated method stub
		return new String[0];
	}

	@Override
	public void setLeafMask(FixedBitSet mask) {
		this.mask = mask;

	}

	@Override
	public void setSecondLeafMask(FixedBitSet mask) {
		// TODO Auto-generated method stub

	}

}
