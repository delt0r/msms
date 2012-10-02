package at.mabs.stats;

import java.util.List;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

public class ThetaW extends StatsCollectorAdapter {
	private FixedBitSet mask;
	private String name;
	private int bins = 0;

	public ThetaW() {
		name = "t_w";
	}

	public ThetaW(String name) {
		this.name = name;
	}
	
	public ThetaW(String name,String bin) {
		this.name = name;
		bins=Integer.parseInt(bin);
	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		//int segSiteCount = 0;// recorder.getMutations().size();
		double[] result = new double[bins + 1];// fist bin is all of them.
		int maskCount = mask.countSetBits();
		double harm = 1.0 / Util.haromicNumber(maskCount - 1);

		double delta = 1.0 / bins;
		int binIndex = 1;

		List<InfinteMutation> mutations = recorder.getMutationsUnsorted();
		for (InfinteMutation m : mutations) {
			int setCount = m.leafSet.countSetBitsMask(mask);
			if (!(setCount == 0 || setCount == maskCount))
				result[0] += harm;
			if (bins > 0) {
				if (binIndex * delta < m.position) {
					binIndex++;
				}
				result[binIndex] += harm / delta;

			}

		}

		// double tw = segSiteCount / harm;
		// result[0] = tw;

		return result;

	}

	@Override
	public String[] getStatLabels() {
		return new String[] { name };
	}

	@Override
	public void setLeafMask(FixedBitSet mask) {

		this.mask = mask;
	}

	@Override
	public void setSecondLeafMask(FixedBitSet mask) {
		// noop
	}

	@Override
	public String toString() {

		return super.toString();
	}

}
