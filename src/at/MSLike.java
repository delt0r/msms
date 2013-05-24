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
package at;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import at.mabs.util.Util;

import at.mabs.util.random.RandomGenerator;
import at.mabs.cmdline.CmdLineBuildException;
import at.mabs.cmdline.CmdLineParseException;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.coalescent.CoalescentEventCalculator;

import at.mabs.config.CommandLineMarshal;
import at.mabs.config.SilentException;

import at.mabs.model.ModelHistroy;
import at.mabs.model.selection.FrequencyTrace;
import at.mabs.model.selection.SuperFrequencyTrace;

import at.mabs.segment.SegmentEventRecoder;
import at.mabs.segment.SegmentSet;
import at.mabs.stats.DiversityStat;
import at.mabs.stats.EHHBeta;
import at.mabs.stats.EHHMax;
import at.mabs.stats.ForwardStatsCollector;
import at.mabs.stats.MSStats;
import at.mabs.stats.StatsCollector;
import at.mabs.stats.StringStatsCollector;

/**
 * main launch point. Does a bit of marshaling even and manages threads and data
 * collection. Pretty tightly coupled to the rest of course.
 * 
 * @author bob
 * 
 */
public class MSLike {
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private String[] args;
	private String[] pasteArgs;
	private String[] firstArgs;// pastes and first. Easyer for sync
	private String[] firstTbsArgs;
	private List<StringStatsCollector> stringCollectors = new ArrayList<StringStatsCollector>();
	private PrintStream ps;
	private ReentrantLock stringStatsOutLock = new ReentrantLock();
	private ProgressControl progress;

	private List<StatsCollector> statsCollectors = new ArrayList<StatsCollector>();
	private ReentrantLock statsLock = new ReentrantLock();
	private boolean statsTable = true;

	private BufferedReader stdInreader;
	private StringTokenizer tbsLineTokens;
	private ReentrantLock tbsInLock = new ReentrantLock();

	private int[] tbsSlots;
	private boolean isTBS;

	private int threadCount;
	private int totalReps = -1;

	private long seed;

	private boolean batchMode;
	private boolean isError;

	public MSLike(String[] args, List<? extends StringStatsCollector> xtraCollectors, List<? extends StatsCollector> xstatsCollector, PrintStream ps,
			ProgressControl progress) {
		this.isError = true;
		this.args = args;
		this.pasteArgs = args;
		if (xtraCollectors != null)
			this.stringCollectors.addAll(xtraCollectors);
		if (xstatsCollector != null) {
			this.statsCollectors.addAll(xstatsCollector);
		}
		this.ps = ps;
		this.progress = progress;

		tbsSlots = calculateTBSSlots(args);
		isTBS = tbsSlots.length > 0;
		if (isTBS) {
			stdInreader = new BufferedReader(new InputStreamReader(System.in));
			pasteArgs = args.clone();
		}

		CommandLineMarshal parser = new CommandLineMarshal();
		CmdLineParser<CommandLineMarshal> processor =  CommandLineMarshal.getCacheParser();
		try {
			firstTbsArgs = updateParser(processor,parser);
			firstArgs = pasteArgs.clone();
		} catch (CmdLineParseException cmpe) {
			printErrorMessage(cmpe, processor,parser);
			return;
		}

		threadCount = parser.getThreadCount();
		totalReps = parser.getRepeats();
		// threadCount =Math.max(threadCount, 1);
		batchMode = parser.isBatchMode();

		if (progress != null) {
			progress.setReps(totalReps);
		}

		if (stringCollectors.isEmpty() && !parser.isBatchMode()) {
			MSStats msStats = new MSStats();
			msStats.setLoption(parser.isTimeOption());
			msStats.setPrintSegSites(!parser.isNoSequence());
			stringCollectors.add(msStats);
			// collectors.add(new EHHBeta());
			// collectors.add(new EHHMax());
			// collectors.add(new DiversityStat());
		}
		stringCollectors.addAll(parser.getStringStatsCollectors());
		statsCollectors.addAll(parser.getStatsCollectors());

		seed = RandomGenerator.resetSeed();
		if (parser.isSeedSet()) {
			seed = parser.getSeed();
		}
		statsTable = parser.isPrintTableStats();
		// stats tables
		if (statsTable) {
			printStatsTable();
		}

		for (StatsCollector sc : statsCollectors) {
			sc.init();
		}
		isError = false;
	}

	public int getTotalReps() {
		return totalReps;
	}

	public void setTotalReps(int totalReps) {
		this.totalReps = totalReps;
	}

	private void printStatsTable() {
		if (statsCollectors.isEmpty())
			return;
		for (StatsCollector sc : statsCollectors) {
			String[] names = sc.getStatLabels();
			if (names == null) {
				ps.print("Unknown\t");
			} else {
				for (String name : names) {
					ps.print(name + "\t");
				}
			}
		}
		ps.print("\n");
	}

	private void runMe() {
		if (!isError)
			runThreads();
	}

	private int[] calculateTBSSlots(String[] args) {
		List<Integer> slots = new ArrayList<Integer>();
		for (int i = 0; i < args.length; i++) {
			if ("tbs".equals(args[i])) {
				slots.add(i);
			}
		}
		return Util.toArrayPrimitiveInteger(slots);
	}

	private String[] updateParser(CmdLineParser<CommandLineMarshal> processor,CommandLineMarshal clm) throws CmdLineParseException {
		// update tbs

		if (tbsSlots.length > 0) {
			String[] args = getTBSPasteArgs();
			clm.reset();
			processor.processArguments(args,clm);
			return args;

		} else {
			clm.reset();
			processor.processArguments(args,clm);
		}
		return args;

	}

	private String[] getTBSPasteArgs() {
		String[] args = pasteArgs.clone();
		tbsInLock.lock();
		try {
			for (int i : tbsSlots) {
				if (tbsLineTokens == null || !tbsLineTokens.hasMoreTokens()) {
					try {
						tbsLineTokens = new StringTokenizer(stdInreader.readLine());
					} catch (IOException e) {
						System.err.println("Error Parsing standard input for tbs options");
						throw new RuntimeException(e);
					}
				}
				args[i] = tbsLineTokens.nextToken();
			}
		} finally {
			tbsInLock.unlock();
		}
		return args;
	}

	/**
	 * this clones copies of required objects to avoid mux on them.
	 * 
	 * @param reps
	 */
	private void run(int startRep, int count) {
		CommandLineMarshal parser = new CommandLineMarshal();
		CmdLineParser<CommandLineMarshal> processor = null;
		processor = CommandLineMarshal.getCacheParser();

		// complicated block of logic to get the correct starting state.
		String[] tbsArgs = null;
		if (startRep == 0 && isTBS) {
			// special case of the first one.
			try {
				// System.out.println("First:"+Arrays.toString(firstTbsArgs));
				processor.processArguments(firstTbsArgs, parser);
				tbsArgs = firstTbsArgs;
			} catch (CmdLineParseException e) {
				printErrorMessage(e, processor,parser);
				return;
			}
		} else {
			try {
				tbsArgs = updateParser(processor,parser);
			} catch (CmdLineParseException cmpe) {
				printErrorMessage(cmpe, processor,parser);
				return;
			}
		}

		ModelHistroy modelHistory = createModelHistory(parser);

		SegmentEventRecoder segmentEventRecoder = new SegmentEventRecoder(modelHistory);
		segmentEventRecoder.setAddSelectedAlleleMutations(parser.isMarkSelectedMutatations());
		segmentEventRecoder.setTrackTrees(parser.isTrackTrees());
		segmentEventRecoder.setConditionalMutation(parser.isCondtionalMutation());

		CoalescentEventCalculator calulator = new CoalescentEventCalculator(modelHistory);
		if (parser.getEventTracker() != null) {
			calulator.setEventTracker(parser.getEventTracker());
			stringCollectors.add(parser.getEventTracker());
		}

		StringBuilder stringBuilder = new StringBuilder();
		List<double[]> rawStats = new ArrayList<double[]>();

		for (int reps = startRep; reps < startRep + count; reps++) {
			if (progress != null) {
				progress.iterationComplete();
				if (progress.isCanceled())
					return;
			}
			// System.out.println("Seed:"+Long.toHexString(seed+reps));
			RandomGenerator.setThreadSeed(seed + reps);

			// *************the real work
			modelHistory.simulateSelection();
			if (!parser.isNoCoalescent()) {
				calulator.calculateCoalescentHistory(segmentEventRecoder);
			}

			rawStats.clear();
			statsLock.lock();
			try {

				for (StatsCollector collector : statsCollectors) {
					double[] s = collector.collectStats(segmentEventRecoder);
					rawStats.add(s);
					// System.err.println(rawStats);
				}
			} finally {
				statsLock.unlock();
			}

			stringStatsOutLock.lock();
			try {
				// first the output delimitor
				if (!batchMode) {
					stringBuilder.append("//");
					if (isTBS) {
						for (String arg : tbsArgs)
							stringBuilder.append('\t').append(arg);
					}
					stringBuilder.append('\n');
					printFlush(stringBuilder);
					//memory means we write to the stream. 
					ForwardStatsCollector forward = modelHistory.getForwardTraceOutput();
					if (forward != null) {
						List<SuperFrequencyTrace> traces = modelHistory.getSelectionTraces();
						ps.println("Frequency Trace:\n");
						for (SuperFrequencyTrace ft : traces) {
							// stringBuilder.append("Frequency Trace:\t"+ft.getStartTime()+"\t"+ft.getEndTime()+"\n");
							forward.collectStats(ft, ps);
							//printFlush(stringBuilder);
						}
					}
				}

				for (StringStatsCollector stats : stringCollectors) {
					stats.collectStats(segmentEventRecoder, stringBuilder);
					// printFlush(stringBuilder);
					// System.out.println(stringBuilder);
					// if(!parser.getIsPhased())
					// {
					// stats.pairShuffle(segmentEventRecoder, stringBuilder,
					// stats.getLengthBeforePol());
					// //System.out.println(stringBuilder);
					// }
					//
					// //System.out.println(stringBuilder);
					// if(!parser.getHasOutgroup())
					// {
					// //System.out.println(parser.getHasOutgroup());
					// stats.noAncestralState(segmentEventRecoder,
					// stringBuilder, stats.getLengthBeforePol());
					// //System.out.println(stringBuilder);
					// }

					printFlush(stringBuilder);
				}

				if (parser.isOriginCount() && !batchMode) {
					ps.println("OriginCount:" + calulator.getSelectedAlleleMutationCount());
				}
				stringBuilder.setLength(0);
				if (statsTable && !rawStats.isEmpty()) {
					for (double[] s : rawStats) {
						for (double d : s) {
							stringBuilder.append(d).append("\t");
						}
					}
					// stringBuilder.append("\n");
					ps.println(stringBuilder);
					stringBuilder.setLength(0);
				}
				stringBuilder.setLength(0);
			} finally {
				stringStatsOutLock.unlock();
			}

			// if we are in tbs mode and we have at least one more to do... get
			// new params.
			if (isTBS && reps + 1 < startRep + count) {
				try {
					tbsArgs = updateParser(processor,parser);
				} catch (CmdLineParseException cmpe) {
					printErrorMessage(cmpe, processor,parser);
					return;
				}
				modelHistory = createModelHistory(parser);
				segmentEventRecoder = new SegmentEventRecoder(modelHistory);
				segmentEventRecoder.setTrackTrees(parser.isTrackTrees());
				segmentEventRecoder.setConditionalMutation(parser.isCondtionalMutation());

				segmentEventRecoder.setAddSelectedAlleleMutations(parser.isMarkSelectedMutatations());
				calulator = new CoalescentEventCalculator(modelHistory);
			}
		}

	}

	private void printFlush(StringBuilder sb) {
		if (!batchMode)
			ps.print(sb);
		sb.setLength(0);
	}

	private void runThreads() {
		// first the basic outputs.
		// System.out.println("threads:"+threadCount);
		if (!batchMode) {
			msHeader(args, ps);
			ps.println();
			ps.println("0x" + Long.toHexString(seed));
			ps.println();
		}

		if (threadCount > 0) {
			launchThreads();
		} else {
			// single threaded.
			MSLike.this.run(0, totalReps);

		}

		// summary outputs.
		StringBuilder stringBuilder = new StringBuilder();
		for (StringStatsCollector stats : stringCollectors) {
			stats.summary(stringBuilder);
			ps.print(stringBuilder);
			stringBuilder.delete(0, stringBuilder.length());
		}
		ps.flush();
		// we are done?
	}

	private void launchThreads() {
		assert threadCount > 0;
		Thread[] threads = new Thread[this.threadCount];
		int delta = this.totalReps / this.threadCount;
		for (int i = 0; i < threadCount; i++) {
			final int startCount = i * delta;
			if (i + 1 == threadCount) {
				delta = totalReps - (threadCount - 1) * delta;// take care of
																// remaniders.
			}
			final int deltaFinal = delta;
			threads[i] = new Thread() {
				@Override
				public void run() {
					MSLike.this.run(startCount, deltaFinal);
				}
			};
			threads[i].start();
			try {
				Thread.sleep(10);// space em out a bit.
			} catch (InterruptedException e) {
				// doesn't affect symatics even if interuped.
			}
		}
		// join to all
		boolean isAnyAlive = true;
		while (isAnyAlive) {
			isAnyAlive = false;
			for (Thread t : threads) {
				if (t.isAlive()) {
					try {
						t.join();
					} catch (InterruptedException e) {
					}
					isAnyAlive |= t.isAlive();
				}
			}
		}
	}

	private void printErrorMessage(CmdLineParseException cmpe, CmdLineParser<CommandLineMarshal> processor,CommandLineMarshal clm) {
		
		// first we ignore errors if help is set to true..
		if (clm.isHelp()) {
			System.out.println("msms " + processor.longUsage());
			return;
		}
		// nothing is its silent.
		if (cmpe.getCause() instanceof SilentException) {
			return;
		}

		System.err.println(cmpe.getMessage());
		if (cmpe.getArgs() != null && cmpe.getOption() != null) {
			System.err.print("options you tried was:\n" + cmpe.getOption() + " ");
			for (String arg : cmpe.getArgs()) {
				System.err.print(arg + " ");
			}
			System.err.println();
		}
		System.err.println("Option help:\n" + processor.longUsage(cmpe.getOption()));
		if (progress != null) {
			progress.error(cmpe);
		}
		cmpe.printStackTrace();
	}

	private ModelHistroy createModelHistory(CommandLineMarshal parser) {
		ModelHistroy modelHistory = new ModelHistroy(parser.getDemeCount(), parser.getN(), parser.getDefaultMigration(), parser.getEvents(),
				parser.isNoCoalescent());
		modelHistory.setNeutralMutationRate(parser.getTheta());
		modelHistory.setSegSiteCount(parser.getSegSiteCount());
		modelHistory.setRecombinationRate(parser.getRecombinationRate());
		modelHistory.setRecombinationCutSites(parser.getRecombinationCutSites());
		modelHistory.setForwardAlleleMutationRate(parser.getForwardMutationRate());
		modelHistory.setBackAlleleMutationRate(parser.getBackwardMutationRate());
		modelHistory.setSAA(parser.getSAA());
		modelHistory.setSaA(parser.getSaA());
		modelHistory.setSaa(parser.getSaa());
		modelHistory.setAlleleLocation(parser.getAllelePostion());
		modelHistory.setSampleConfiguration(parser.getSampleConfig());
		modelHistory.setLociConfiguration(parser.getNeutralLoci());
		modelHistory.setTimedCondition(parser.getStoppingCondition());
		modelHistory.setSelectionSimulator(parser.getSelectionSimulator());
		modelHistory.setRestartCondtion(parser.getRestartCondition());
		modelHistory.setFoldMutations(parser.getFoldMutations());
		modelHistory.setUnphase(!parser.getIsPhased());
		modelHistory.setMaxRecombinationRate(parser.getMaxRecombinationRate());
		modelHistory.setWeightedMutations(parser.isWeightedMutations());
		modelHistory.setSeedDeme(parser.getSeedDeme());
		if (parser.isFrequencyTrace()) {
			modelHistory.setForwardTraceOutput(new ForwardStatsCollector(modelHistory));
		}
		modelHistory.setSelection(parser.isSelection());
		return modelHistory;

	}

	public boolean isError() {
		return isError;
	}

	public static void main(String[] args, List<? extends StringStatsCollector> collectors, PrintStream ps, ProgressControl progress) {
		MSLike mslike = new MSLike(args, collectors, null, ps, progress);
		mslike.runMe();

	}

	public static void main(String[] args, List<? extends StringStatsCollector> collectors, List<? extends StatsCollector> scollectors, PrintStream ps,
			ProgressControl progress, int repCountOverride) {
		MSLike mslike = new MSLike(args, collectors, null, ps, progress);
		mslike.setTotalReps(repCountOverride);
		mslike.runMe();

	}

	public static void main(String[] args, List<? extends StringStatsCollector> collectors, List<? extends StatsCollector> scollectors, PrintStream ps,
			ProgressControl progress) {
		MSLike mslike = new MSLike(args, collectors, scollectors, ps, progress);
		mslike.runMe();
	}

	private static void msHeader(String[] args, PrintStream ps) {
		// we just output the command line with little regard for parsing tools.
		// there is on exception. -ms is cleaned to be ms.
		ps.print("ms ");
		for (String s : args) {
			if (s.equals("-ms")) {
				// ps.print("ms ");
			} else {
				ps.print(s + " ");
			}

		}
		ps.print(" [" + MSLike.class.getPackage().getImplementationVersion() + "]");
	}

	public static void main(String[] args) {
		// faster std IO

		// BufferedWriter out =new BufferedWriter(new OutputStreamWriter(new
		// FileOutputStream(java.io.FileDescriptor.out), "ASCII"), 4096);

		PrintStream ps = System.out;// new PrintStream(new
									// BufferedOutputStream(new
									// FileOutputStream(FileDescriptor.out),
									// 2048), false);

		main(args, null, ps, null);

		// Random random=new Random64();
		// double c=0;
		// for(int i=0;i<10000;i++){
		// c+=Math.min(random.nextInt(10000),random.nextInt(10000) );
		// System.out.println(c/i);
		// }
	}

}
