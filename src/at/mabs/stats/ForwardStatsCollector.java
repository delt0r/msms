package at.mabs.stats;

import java.io.IOException;
import java.io.*;

import at.mabs.model.ModelHistroy;
import at.mabs.model.selection.FrequencyTrace;
import at.mabs.model.selection.SuperFrequencyTrace;

/**
 * for now only one class since this happens only at one place for one thing.
 * 
 * @author bob
 * 
 */
public class ForwardStatsCollector {
	private double generationScale;

	public ForwardStatsCollector(ModelHistroy mh) {
		generationScale = 1.0 / (mh.getN() * 4);
	}

	public void collectStats(SuperFrequencyTrace trace, PrintStream output) {
		// start at the most pastward time.
		// int deme=trace.getDemeCount();

		trace.setIndexMostPastward();
		double[] freq = null;
		long t = trace.getIndexTime();
		while (trace.hasMoreForward()) {
			freq = trace.getFrequencys(freq);
			paste(t, freq, output);
			trace.moveForward();
			t = trace.getIndexTime();
			// System.out.println(output);
			// output.setLength(0);
		}

	}

	private void paste(double time, double[] freq, PrintStream out) {
		out.append("" + time * generationScale).append('\t');
		for (int d = 0; d < freq.length; d++) {
			out.append("" + (1.0 - freq[d]) + "\t" + freq[d] + "\t");
			// for(int a=1;a<freq[0].length;a++){
			// out.append(freq[d][a]).append('\t');
			// }
		}
		out.append('\n');
	}
}
