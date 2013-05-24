package at.mabs.external;

import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.stats.MSStats;
import at.mabs.stats.StatsCollector;
import at.mabs.util.Bag;
import at.mabs.util.Util;
import at.mabs.util.random.Random64;

import java.io.*;
import java.util.*;

import com.esotericsoftware.yamlbeans.YamlWriter;

/**
 * reads in next gen sequences from std in, spits out a stats thing. Using the
 * GMI type format...
 * 
 * @author bob
 * 
 */
public class StatsExportVetMed {
	public static void main(String[] args) {
		try {
			CommandLineMarshal clm = new CommandLineMarshal();
			CmdLineParser<CommandLineMarshal> parser = CommandLineMarshal.getCacheParser();
			parser.processArguments(args, clm);

			// hack has the filename we care about.
			String fileName = clm.HACK_PARAMS[0];
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			// assume format is:
			// chrX $POS $A/$C/$G/$T $A/$C/$G/$T
			List<InfinteMutation> mutations = new ArrayList<InfinteMutation>();
			String line = br.readLine();
			while (line != null) {
				StringTokenizer st = new StringTokenizer(line, " \t/");
				// dump the chrX
				st.nextToken();
				// dump the post like who cares we don't have linkage anyway.
				st.nextToken();
				// next four are deme A
				int[] demeA = new int[4];
				demeA[0] = Integer.parseInt(st.nextToken());
				demeA[1] = Integer.parseInt(st.nextToken());
				demeA[2] = Integer.parseInt(st.nextToken());
				demeA[3] = Integer.parseInt(st.nextToken());
				int[] demeB = new int[4];
				demeB[0] = Integer.parseInt(st.nextToken());
				demeB[1] = Integer.parseInt(st.nextToken());
				demeB[2] = Integer.parseInt(st.nextToken());
				demeB[3] = Integer.parseInt(st.nextToken());

				// assume folded spectrum.
				int wt = wildType(demeA, demeB);
				System.out.println(Arrays.toString(demeA) + "\t" + Arrays.toString(demeB) + "\t" + wt);
				int totalA = Util.sum(demeA);
				int totalB = Util.sum(demeB);
				FixedBitSet fbs = new FixedBitSet(totalA + totalB);
				for (int i = 0; i < demeA[wt]; i++) {
					fbs.set(i);
				}
				for (int i = totalA; i < totalA + demeB[wt]; i++) {
					fbs.set(i);
				}
				if (fbs.countSetBits() > fbs.size() / 2) {
					fbs.invert();
				}
				InfinteMutation mut = new InfinteMutation(0, fbs);
				mutations.add(mut);
				// System.out.println(mut.leafSet);

				line = br.readLine();
			}
			List<StatsCollector> collectors = clm.getStatsCollectors();
			for (StatsCollector sc : collectors) {
				sc.init();
			}
			System.out.println("Reps:"+mutations.size()/clm.getRepeats());
			for (int i = 0; i < clm.getRepeats(); i++) {
				int start = i*mutations.size()/clm.getRepeats();
				Bag<InfinteMutation> sublist =new Bag(mutations.subList(start, Math.min(start+mutations.size()/clm.getRepeats(),mutations.size())));
				System.out.println("SubList:"+sublist);
				SegmentEventRecoder ser = new SegmentEventRecoder(sublist, true, false);
				for (StatsCollector sc : collectors) {
					sc.collectStats(ser);
				}
			}
			
			Writer writer = new FileWriter(clm.HACK_PARAMS[1]);
			saveStats(writer, collectors);
			//just a little more stnd out
			System.out.println("Stats Output");
			for(StatsCollector sc:collectors){
				double[] r=sc.summaryStats();
				for(double d:r){
					System.out.print(d+"\t");
				}
			}
			System.out.println();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int wildType(int[] a, int[] b) {
		int max = 0;
		int index = -1;
		for (int i = 0; i < a.length; i++) {
			if (a[i] + b[i] > max) {
				max = a[i] + b[i];
				index = i;
			}
		}
		return index;
	}

	private static void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

}
