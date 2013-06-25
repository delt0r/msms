package at.mabs.external;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import at.mabs.abc.SGA;
import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import at.mabs.gui.MSMSPlay;
import at.mabs.model.SampleConfiguration;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.stats.StatsCollector;
import at.mabs.util.Bag;
import at.mabs.util.random.Random64;

import java.io.*;

import com.esotericsoftware.yamlbeans.YamlWriter;

/**
 * more generic for general export scripts.
 * 
 * @author bob
 * 
 */
public class RomanStatExport {
	private List<StatsCollector> collectors;
	private SampleConfiguration sampleConfig;
	private boolean plainText = false;
	private boolean randomizeResamples = false;
	private String inFileName;
	private String outFileName = "-";
	private CommandLineMarshal msmsparser;
	private boolean help = false;
	private boolean haploid = false;

	@CLNames(names = { "-msms" }, required = true)
	@CLDescription("The msms comand line for contex. Need the number of demes and sampling stratagy and -s to be correct. Of course this includes the stats collectors used. Dont forget that each indivd is 2 samples")
	public void msmsCmdLine(String[] msmsArgs) {
		msmsparser = new CommandLineMarshal();
		try {
			CmdLineParser<CommandLineMarshal> marshel = CommandLineMarshal.getCacheParser();// new
																							// CmdLineParser<CommandLineMarshal>(msmsparser);
			marshel.processArguments(msmsArgs, msmsparser);
			sampleConfig = msmsparser.getSampleConfig();

			collectors = msmsparser.getStatsCollectors();
			if (collectors == null || collectors.size() == 0) {
				throw new RuntimeException("Need at least one -stat option");
			}
			for (StatsCollector sc : collectors) {
				sc.init();
			}

		} catch (Exception e) {
			System.err.println("Error Parsing MSMS comand line. Note that all non msms options must come before the -msms switch.");
			throw new RuntimeException(e);
		}
	}

	@CLNames(names = { "-help", "-h", "--help", "--h", "help" })
	public void setHelp() {
		this.help = true;
	}

	@CLNames(names = { "-oText" })
	@CLDescription("Plain output as per msms stats. Usefull for other ABC setups.")
	public void setPlainText() {
		this.plainText = true;
	}

	@CLNames(names = { "-randomize", "-R" })
	@CLDescription("Randomize the resampling. Still continus sets of SNPs")
	public void setRandomizeResamples() {
		this.randomizeResamples = true;
	}

	@CLNames(names = { "-haploid", "-hap" })
	@CLDescription("Assume each line is a single dna sequence and count 1s and 2s as the same")
	public void setHaploid() {
		this.haploid = true;
	}

	@CLNames(names = { "-inFile", "-if" })
	@CLDescription("The input file name. Reads in $FILENAME where each line is 2 samples (hetro/homo), deme locations are defined by order")
	public void setInFileName(String inFileName) {
		this.inFileName = inFileName;
	}

	@CLNames(names = { "-outFile", "-of" })
	@CLDescription("Output file. - for std output")
	public void setOutFileName(String outFileName) {
		this.outFileName = outFileName;
	}

	public void run() throws IOException {
		BufferedReader br = null;//
		if (inFileName.equalsIgnoreCase("-")) {
			br = new BufferedReader(new InputStreamReader(System.in));
		} else {
			br = new BufferedReader(new FileReader(inFileName));
		}
		// first col is the sequence id. since each becomes 2....
		// we remove -1 cols..
		Bag<FixedBitSet> list = new Bag<FixedBitSet>();

		String line = br.readLine();
		// now init things from the first line. ie snp count ...
		StringTokenizer st = new StringTokenizer(line);
		int snpCount = st.countTokens() - 1;
		int sampleCount = sampleConfig.getMaxSamples();// note should be 2x the number of rows.

		for (int i = 0; i < snpCount; i++) {
			list.add(new FixedBitSet(sampleCount));
		}
		int leaf = 0;
		while (line != null) {
			st = new StringTokenizer(line);
			st.nextToken();// index... we don't care.
			int snp = 0;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				FixedBitSet fbs = list.get(snp);
				// System.out.println("I:"+i+"\t"+st.countTokens()+"\t"+sampleCount+"\t"+token);
				if (fbs != null) {
					int code = Integer.parseInt(token);
					if (!haploid) {
						switch (code) {
						case -1:
							list.set(snp, null);// drop.
							break;
						case 0:
							break;
						case 1:
							fbs.set(leaf * 2);
							break;
						case 2:
							fbs.set(leaf * 2);
							fbs.set(leaf * 2 + 1);
							break;
						default:
							System.err.println("Code error:" + code);
						}
					} else {
						switch (code) {
						case -1:
							list.set(snp, null);// drop.
							break;
						case 0:
							break;
						case 1:
						case 2:
							fbs.set(leaf);
							break;
						default:
							System.err.println("Code error:" + code);
						}
					}
				}
				snp++;
			}

			leaf++;
			line = br.readLine();
		}
		int p = -1;
		List<InfinteMutation> muts = new ArrayList<InfinteMutation>();
		for (FixedBitSet fbs : list) {
			p++;
			if (fbs != null)
				muts.add(new InfinteMutation((double) p / list.size(), fbs));
		}

		// so now we work with the output.
		int perLocusSnps = (int) msmsparser.getSegSiteCount();
		int locusCount = msmsparser.getRepeats();
		Random rand = new Random64();
		for (int i = 0; i < locusCount; i++) {
			int start = i * perLocusSnps;
			if (randomizeResamples)
				start = rand.nextInt(muts.size() - perLocusSnps);
			if (start + perLocusSnps > muts.size()) {
				throw new RuntimeException("more loci than data. Recommend using the randomize option");
			}
			Bag<InfinteMutation> sublist = new Bag(muts.subList(start, start + perLocusSnps));
			SegmentEventRecoder ser = new SegmentEventRecoder(sublist, msmsparser.getFoldMutations(), msmsparser.getFoldMutations());

			for (StatsCollector sc : collectors) {
				sc.collectStats(ser);
			}
		}

		Writer writer = null;
		if (outFileName.equalsIgnoreCase("-")) {
			writer = new OutputStreamWriter(System.out);
		} else {
			writer = new FileWriter(outFileName);
		}

		if (plainText) {
			plainTextOut(writer);
			writer.flush();
			writer.close();
		} else {
			saveStats(writer, collectors);
		}
		

	}

	private void plainTextOut(Writer writer) throws IOException {
		writer.write("Stats Output\n");
		for (StatsCollector sc : collectors) {
			double[] r = sc.summaryStats();
			for (double d : r) {
				writer.write(d + "\t");
			}
		}
		writer.write("\n");
	}

	private static void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

	public static void main(String[] args) {
		RomanStatExport rse = new RomanStatExport();
		CmdLineParser<RomanStatExport> parser = null;
		try {
			parser = new CmdLineParser<RomanStatExport>(RomanStatExport.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {
			parser.processArguments(args, rse);
			if (rse.help) {
				System.err.println(parser.longUsage());
				System.exit(0);
			}
			rse.run();
		} catch (Exception e) {
			System.err.println(parser.longUsage());
			if (!rse.help)
				e.printStackTrace();

		}
	}
}
