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
 * reads in next gen sequences from std in, spits out a stats thing. Using the GMI type format... 
 * 
 * @author bob
 * 
 */
public class StatsExport {
	public static void main(String[] args) {
		try {
			CommandLineMarshal clm = new CommandLineMarshal();
			CmdLineParser<CommandLineMarshal> parser = CommandLineMarshal.getCacheParser();
			parser.processArguments(args, clm);

			// hack has the filename we care about.
			String fileName = clm.HACK_PARAMS[0];
			BufferedReader br = new BufferedReader(new FileReader(fileName));

			List<Place> namesPlaces = new ArrayList<Place>();
			String line = br.readLine();
			while (line != null) {
				StringTokenizer st = new StringTokenizer(line);
				String name = st.nextToken();
				int place = Integer.parseInt(st.nextToken());
				namesPlaces.add(new Place(place, name));
				line = br.readLine();
			}
			Collections.sort(namesPlaces);
			// System.out.println(namesPlaces);
			// need to order as places.
			br.close();

			ArrayList<String> names = new ArrayList<String>();
			for (Place p : namesPlaces) {
				names.add(p.name);
			}

			ArrayList<Integer> bitPos = new ArrayList<Integer>();
			// first up is the header, with Pos as the first "thing"
			br = new BufferedReader(new InputStreamReader(System.in));
			line = br.readLine();
			// assume this line is the header.
			StringTokenizer st = new StringTokenizer(line);
			st.nextToken();// dump the "Pos"
			while (st.hasMoreTokens()) {
				String name = st.nextToken();
				// System.out.println("Name:"+name+"\t"+);
				bitPos.add(names.indexOf(name));
			}
			int[] bitPosFast = Util.toArrayPrimitiveInteger(bitPos);
			System.out.println("PosArray:" + Arrays.toString(bitPosFast));
			// now we parse the rest of stnd in.
			List<InfinteMutation> mutations = new ArrayList<InfinteMutation>();
			line = br.readLine();
			while (line != null) {
				st = new StringTokenizer(line);
				int pos = Integer.parseInt(st.nextToken());
				String state = st.nextToken();// our zero state. all others are
												// 1
				FixedBitSet leafSet = new FixedBitSet(names.size());
				for (int i = 1; i < bitPosFast.length; i++) {
					String token = st.nextToken();
					int bit = bitPosFast[i];
					if (bit < 0)
						continue;
					if (!token.equals(state))
						leafSet.set(bit);
				}
				line = br.readLine();
				int set=leafSet.countSetBits();
				if(set==leafSet.size() || set==0){
					continue;
				}
				InfinteMutation im = new InfinteMutation(pos, leafSet);
				mutations.add(im);
				if (mutations.size() % 500 == 0)
					System.out.print("*");
			}
			System.out.println("\nParsed " + mutations.size() + " SNPs");
			Collections.sort(mutations);
			// renormalize;
			double largest = mutations.get(mutations.size() - 1).position;

			for (int i = 0; i < mutations.size(); i++) {
				InfinteMutation im = mutations.get(i);
				mutations.set(i, new InfinteMutation(im.position / largest, im.leafSet));
			}
			// now we have the mutations. we get -s snps in a row, reps times...
			for (int j = 0; j < 10; j++) {
				List<StatsCollector> collectors = clm.getStatsCollectors();
				for(StatsCollector sc:collectors){
					sc.init();
				}
				int snps = (int)clm.getSegSiteCount();
				Random rand = new Random64();
				for (int i = 0; i < clm.getRepeats(); i++) {
					int start = rand.nextInt(mutations.size());
					Bag<InfinteMutation> sublist =new Bag(mutations.subList(start, Math.min(start + snps, mutations.size() - 1)));
					SegmentEventRecoder ser = new SegmentEventRecoder(sublist, true, false);
					for (StatsCollector sc : collectors) {
						sc.collectStats(ser);
					}
				}
				Writer writer = new FileWriter(clm.HACK_PARAMS[1]+j);
				saveStats(writer, collectors);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

	private static class Place implements Comparable<Place> {
		int place;
		String name;

		public Place(int p, String n) {
			this.place = p;
			this.name = n;
		}

		@Override
		public int compareTo(Place o) {
			return place - o.place;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Place other = (Place) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public String toString() {

			return name + " @ " + place;
		}

	}
}
