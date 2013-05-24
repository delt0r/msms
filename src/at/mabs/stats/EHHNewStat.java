package at.mabs.stats;

import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import java.util.*;

public class EHHNewStat extends StatsCollectorAdapter {
	private int binCount = 10;

	private String name;

	public EHHNewStat() {
	}

	public EHHNewStat(String name) {
	}

	public EHHNewStat(String name, String binCountString) {
		// System.out.println("Parsing!"+binCountString);
		binCount = Integer.parseInt(binCountString);
	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		List<InfinteMutation> mutations = recorder.getMutationsSorted();
		if (mutations.size() < 2)
			return new double[binCount];

		double[] data = new double[mutations.size()];

		int leafCount = mask.countSetBits();
		for (int i = 0; i < mutations.size(); i++) {
			InfinteMutation start = mutations.get(i);

			FixedBitSet oneMask = new FixedBitSet(start.leafSet);
			oneMask.and(mask);
			FixedBitSet zeroMask = new FixedBitSet(start.leafSet);
			zeroMask.invert();
			zeroMask.and(mask);

			double oneF = (double) oneMask.countSetBits() / leafCount;
			double zeroF = (double) zeroMask.countSetBits() / leafCount;

			double isum = 0;
			double lastp = start.position;
			double lastf = Math.max(oneF, zeroF);

			for (int j = i + 1; j < mutations.size(); j++) {
				InfinteMutation end = mutations.get(j);

				oneMask.and(end.leafSet);
				zeroMask.nand(end.leafSet);

				double noneF = (double) oneMask.countSetBits() / leafCount;
				double nzeroF = (double) zeroMask.countSetBits() / leafCount;
				double nf = Math.max(noneF, nzeroF);
				
				isum += (lastf + nf) * (end.position-lastp) / 2;

				lastf = nf;
				lastp = end.position;
				oneF = noneF;
				zeroF = nzeroF;
				if (oneF == 0 && zeroF == 0) {
					break;
				}
			}
			data[i] = isum;
		}

		// that was forward, now for backward
		for (int i = mutations.size() - 1; i >= 0; i--) {
			InfinteMutation start = mutations.get(i);

			FixedBitSet oneMask = new FixedBitSet(start.leafSet);
			oneMask.and(mask);
			FixedBitSet zeroMask = new FixedBitSet(start.leafSet);
			zeroMask.invert();
			zeroMask.and(mask);

			double oneF = (double) oneMask.countSetBits() / leafCount;
			double zeroF = (double) zeroMask.countSetBits() / leafCount;

			double isum = 0;
			double lastp = start.position;
			double lastf = Math.max(oneF, zeroF);

			for (int j = i - 1; j >= 0; j--) {
				InfinteMutation end = mutations.get(j);

				oneMask.and(end.leafSet);
				zeroMask.nand(end.leafSet);

				double noneF = (double) oneMask.countSetBits() / leafCount;
				double nzeroF = (double) zeroMask.countSetBits() / leafCount;
				double nf = Math.max(noneF, nzeroF);

				isum += (lastf + nf) * (lastp - end.position) / 2;

				lastf = nf;
				lastp = end.position;
				oneF = noneF;
				zeroF = nzeroF;
				if (oneF == 0 && zeroF == 0) {
					break;
				}
			}
			data[i] *= isum;
		}

		// now we need to bin it..
		double[] bins = new double[binCount];
		int[] counts = new int[binCount];
		for (int i = 0; i < mutations.size(); i++) {
			int bin = (int) (mutations.get(i).position * binCount);
			bins[bin] = Math.max(data[i],bins[bin]);
			counts[bin]++;
		}
		for (int i = 0; i < bins.length; i++) {
			if (counts[i] > 0)
				bins[i] /= counts[i];
		}
		return bins;
	}

}
