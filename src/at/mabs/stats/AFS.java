package at.mabs.stats;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;

public class AFS extends StatsCollectorAdapter {
	private String name;
	private int bins = 1;

	public AFS() {
		name = "afs";
	}

	public AFS(String name) {
		this.name = name;
	}

	public AFS(String name, String bins) {
		this.name = name;
		this.bins = Integer.parseInt(bins);
	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		int asize = mask.countSetBits();
		// System.out.println("\nAsize:"+asize);
		// assert false;
		double[] result = new double[(asize + 1) * bins];
		for (InfinteMutation m : recorder.getMutationsUnsorted()) {
			int bin = (int) (Math.min(m.position,1-1e-9) * bins);//clamp. 
			int a = m.leafSet.countSetBitsMask(mask);
			
			result[a + (asize + 1) * bin]+=m.weight;
		}
		
		return result;
	}

	@Override
	public String[] getStatLabels() {
		int asize = mask.countSetBits();

		String[] names = new String[(asize + 1) * bins];
		int counter=0;
		for (double b = 0; b < bins; b++) {
			for (int i = 0; i <= asize; i++) {

				names[counter++] = name + "_" + i+"["+b/bins+","+(b+1)/bins+")";
			}
		}
		return names;
	}
}
