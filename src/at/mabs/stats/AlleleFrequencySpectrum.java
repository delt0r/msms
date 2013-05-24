/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
*/
package at.mabs.stats;

import java.util.Arrays;
import java.util.List;

import at.mabs.model.SampleConfiguration;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

/**
 * calulates AFS and jAFS as indicated with options. Displays the jAFS for each
 * deme pair and the global AFS for all demes. The summary is the sum pairwise
 * jAFS and the sum gloabal AFS.
 * 
 * @author greg
 * 
 */
public class AlleleFrequencySpectrum implements StringStatsCollector {
	private final boolean jAFS;
	private final boolean AFS;
	private final boolean onlySummary;
	private FixedBitSet[] demeMasks;
	private int ndemes;
	private int[] lastAFS;
	private int[] gloabalAFS;

	private int[][][] pairwiseJAFS;
	private int[][][] cumulantJAFS;

	public AlleleFrequencySpectrum(boolean jAFS, boolean afs, boolean onlySummary, SampleConfiguration sampleConfig) {
		assert jAFS != false || afs != false;
		this.jAFS = jAFS;
		AFS = afs;
		this.onlySummary = onlySummary;
		int sampleCount = sampleConfig.getMaxSamples();
		gloabalAFS = new int[sampleCount - 1];
		ndemes = sampleConfig.getDemeCount();
		
		demeMasks=sampleConfig.getMasks();
		if (jAFS) {
			if (ndemes < 1)
				throw new RuntimeException("Can't collect jAFS stats with only one deme");
//			demeMasks = new LeafSet[ndemes];
//
//			int leafCount = 0;
//			for (int i = 0; i < ndemes; i++) {
//				LeafSet mask = new LeafSet(sampleCount);
//				for (int j = 0; j < sampleConfig[i]; j++) {
//					mask.set(leafCount++);
//				}
//				demeMasks[i] = mask;
//			}

			// now Allocate the jAFS arrays.
			int nC2 = (ndemes * (ndemes - 1)) / 2;
			pairwiseJAFS = new int[nC2][0][0];
			cumulantJAFS = new int[nC2][0][0];
			int pair = 0;
			for (int i = 0; i < ndemes; i++) {
				for (int j = i + 1; j < ndemes; j++) {
					pairwiseJAFS[pair] = new int[sampleConfig.getTotalSamplesInDeme(i) + 1][sampleConfig.getTotalSamplesInDeme(j) + 1];
					cumulantJAFS[pair++] = new int[sampleConfig.getTotalSamplesInDeme(i) + 1][sampleConfig.getTotalSamplesInDeme(j) + 1];
				}
			}
		}
	}
	

	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		if (AFS) {
			int[] freq = getFrequencySpectrum(recorder);
			lastAFS=freq;
			Util.arrayAdd(gloabalAFS, freq);
			if (!onlySummary) {
				builder.append("AFS: ");
				for (int i = 0; i < freq.length; i++) {
					builder.append(freq[i]).append(' ');
				}
				builder.append('\n');
			}
		}
		
		if (!jAFS)
			return;
		List<InfinteMutation> mutations = recorder.getMutationsUnsorted();
		clearPairArrays();
		int ndemes = demeMasks.length;
		int[] demeFreq = new int[ndemes];
		for (InfinteMutation mutation : mutations) {
			fillDemeFrequency(mutation.leafSet, demeFreq);
			// System.out.println(Arrays.toString(demeFreq));
			int pairCount = 0;
			for (int i = 0; i < ndemes; i++) {
				for (int j = i + 1; j < ndemes; j++) {
					pairwiseJAFS[pairCount][demeFreq[i]][demeFreq[j]]++;
					cumulantJAFS[pairCount++][demeFreq[i]][demeFreq[j]]++;
				}
			}
		}
		if (!onlySummary) {
			printJAFSmatrix(pairwiseJAFS, builder);
		}
	}

	private void printJAFSmatrix(int[][][] matrix, StringBuilder builder) {
		// for(int[][] m:matrix){
		int count = 0;
		for (int da = 0; da < ndemes; da++) {
			for (int db = da + 1; db < ndemes; db++) {
				int[][] m=matrix[count++];
				builder.append("jAFS ").append(da).append(" vrs ").append(db).append('\n');
				for (int i = 0; i<m.length; i++) {
					for (int j = 0; j < m[i].length; j++) {
						builder.append(m[i][j]).append(' ');
					}
					// remove the last space.
					builder.deleteCharAt(builder.length() - 1);
					builder.append('\n');
				}
			}
		}
		builder.append('\n');
	}

	private void clearPairArrays() {
		for (int[][] array : pairwiseJAFS) {
			for (int i = 0; i < array.length; i++) {
				Arrays.fill(array[i], 0);
			}
		}
	}

	private void fillDemeFrequency(FixedBitSet set, int[] demeF) {
		for (int i = 0; i < demeMasks.length; i++) {
			FixedBitSet mask = demeMasks[i];
			demeF[i] = set.countSetBitsMask(mask);
		}
	}

	private int[] getFrequencySpectrum(SegmentEventRecoder recorder) {
		List<InfinteMutation> mutations = recorder.getMutationsUnsorted();
		if (mutations.isEmpty())
			return new int[0];// nice than null;

		int[] freq = new int[mutations.get(0).leafSet.size() - 1];
		
		for (int i = 0; i < mutations.size(); i++) {
			int f = mutations.get(i).leafSet.countSetBits();
			freq[f - 1]++;
		}
		return freq;
	}

	@Override
	public void summary(StringBuilder builder) {
		if (AFS) {
			builder.append("Summary AFS:");
			for (int i = 0; i < gloabalAFS.length; i++) {
				builder.append(gloabalAFS[i]).append(' ');
			}
			builder.deleteCharAt(builder.length() - 1);
			builder.append('\n');
		}
		if (jAFS) {
			builder.append("Summary jAFS\n");
			printJAFSmatrix(cumulantJAFS, builder);
		}
	}
	
	public int[][][] getCumulantJAFS() {
		return cumulantJAFS;
	}
	
	public int[][][] getPairwiseJAFS() {
		return pairwiseJAFS;
	}
	
	public int[] getLastAFS() {
		return lastAFS;
	}
	
	public int[] getGlobalAFS() {
		return gloabalAFS;
	}

}
