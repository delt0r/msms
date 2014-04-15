package at.mabs.abc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import at.MSLike;
import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CLUsage;
import at.mabs.cmdline.CmdLineBuildException;
import at.mabs.cmdline.CmdLineParseException;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import at.mabs.model.SampleConfiguration;
import at.mabs.stats.StatsCollector;
import at.mabs.util.NullPrintStream;
import at.mabs.util.Util;
import at.mabs.util.random.Random64;

/**
 * Finalizing the basic ABC framework. Pure rejection is done by simply keeping the n best.
 * 
 * Still need some form of stats normalization. For now default is the mean and variance of every stat. This has problems as discussed in the literature.
 * 
 * 
 * We are going to change the formatting a little. We will now use JSON for input and output. JS, python and other script langs all support it. The vector type
 * stats just don't fit with the ABCToolBox, so we will need a marshaling layer anyway.
 * 
 * We now also add the command line marshaling code. We have the ABC options followed by the msms "annotated" options. There are some restrictions. First only
 * "real" values can be changed. Not sample size for example or even the sampling. Adding "random" sampling is something for the future, or is more of a "model"
 * testing problem. As such there are problems with using ABC for this. Secondly we have restrucited prior distributions. Uniform, and log (is X~log(U(a,b))).
 * 
 * We are adding quite a few statistics. This gets messy when you have a lot. And for now linear combinations are not going to happen. There are defualt stats,
 * namely SFS.
 * 
 * Note that stats often can take arguments as well. These are just past on to the respective stat.
 * 
 * more changes. Stats are going to be restricted to R^n. No fancy "objects" hence distance metrics and normalizations are all handled more simply here.
 * 
 * @author greg
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class NewABC {
	private String[] anotatedCmdLine;
	private String dataFileName;
	private String outputFileName;
	private String chainFileName;

	private int sampleSize = 1000;
	private int reps = 1000000;

	private boolean plsNormalzation;
	private boolean pcaNormalzation;

	private boolean dumpAll;

	private ParameterStatPair.TransformData transformData;

	private boolean writeData;// good for simulations.
	private String[] writeArgs;

	private boolean mcmc;

	private boolean hackSample = false;

	private boolean reductions = false;

	// best n of the bunch.
	private TreeSet<ParameterStatPair> sampledPoints = new TreeSet<ParameterStatPair>();

	private double[] stds;
	// first n parameters produced.
	private List<ParameterStatPair> bootstrapPoints = new ArrayList<ParameterStatPair>();

	// currently unsaved mcmc chain samples
	private List<ParameterStatPair> chainPoints = new ArrayList<ParameterStatPair>();

	// ordered list with params, statDistances, totalDistance
	private ParameterStatPair currentState;

	private List<StatsCollector> collectionStats = new ArrayList<StatsCollector>();
	private List<StatsCollector> dataStats = new ArrayList<StatsCollector>();
	private int priorUpdate = Integer.MAX_VALUE;
	private Random random = new Random64();

	private int truncatStats = Integer.MAX_VALUE; 

	// private double[] normalizingFactors;

	public NewABC() {
	}

	public void run() {
		List<PriorDensity> priors = PriorDensity.parseAnnotatedStrings(anotatedCmdLine);
		System.out.println(priors);
		String[] msmsArgs = anotatedCmdLine.clone();
		paste(msmsArgs, priors, false, null);
		initStatCollectors(msmsArgs);

		if (writeData) {
			int argIndex = 0;
			// System.out.println("Write Lenght:"+writeArgs.length+"\t"+priors.size());
			for (PriorDensity pd : priors) {
				String temp = writeArgs[argIndex++];
				msmsArgs[pd.getArgIndex()] = temp;
			}
			MSLike.main(msmsArgs, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			writeDataStats(collectionStats);
			System.out.println("Wrote out Stats:" + Arrays.toString(msmsArgs));
			// return;
		}
		// now lets read in the stats.
		initDataFile();// may need to init these as well.
		double epsilon = Double.NaN;

		// collect the bootstrap samples.
		System.out.println("|---------|---------|---------|---------|---------|---------|---------|---------|");
		for (int r = 0; r < sampleSize; r++) {
			double[] values = null;
			paste(msmsArgs, priors, false, values);
			// System.out.println("Args:"+Arrays.toString(msmsArgs));
			MSLike.main(msmsArgs, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			double[] distances = collectStatitics(collectionStats);
			ParameterStatPair psp = ParameterStatPair.packIntoParamStat(distances, priors);
			bootstrapPoints.add(psp);
			if (bootstrapPoints.size() % (1 + sampleSize / 80) == 0) {
				System.out.print("=");
			}
		}
		System.out.println("|");
		transformData = ParameterStatPair.calculateTransformData(bootstrapPoints, false, pcaNormalzation, plsNormalzation);

		ParameterStatPair dataStatPair = getDataStatPair(dataStats);
		dataStatPair.transform(transformData);
		for (ParameterStatPair pair : bootstrapPoints) {
			pair.transform(transformData);
			pair.calculateDistance(dataStatPair, truncatStats);// euclid
		}

		sampledPoints.addAll(bootstrapPoints);

		stds = new double[priors.size()];
		for (int i = 0; i < stds.length; i++) {
			PriorDensity pd = priors.get(i);
			stds[i] = pd.getMax() - pd.getMin();
		}

		saveResultState();

		// init thing for mcmc.
		if (mcmc) {
			int count = 0;
			for (ParameterStatPair p : sampledPoints) {
				epsilon = p.getDistance();
				// System.out.println(epsilon);
				count++;
				if ((double) count / sampledPoints.size() > 0.01)
					break;
			}
			currentState = sampledPoints.first();
			// epsilon = 5.5;// sampledPoints.last().getDistance();
		}
		// System.out.println(sampledPoints);
		Writer allResults = null;

		if(dumpAll){
			//write out reps simulations in a gzip file...
			try {
				allResults=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFileName+".dump.gz"))));
			}catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		for (int r = 0; r < this.reps; r++) {
			double[] values = null;
			if (currentState != null) {
				values = currentState.getParameters();
			}
			if (hackSample) {
				ArrayList<ParameterStatPair> list = new ArrayList<ParameterStatPair>(sampledPoints);
				values = list.get(r % list.size()).getParameters();
			}
			if (!reductions) {
				paste(msmsArgs, priors, mcmc, values);
			} else {
				pasteFancy(msmsArgs, priors);
			}
			// System.out.println("Args:"+Arrays.toString(msmsArgs));
			MSLike.main(msmsArgs, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			double[] distances = collectStatitics(collectionStats);
			ParameterStatPair psp = ParameterStatPair.packIntoParamStat(distances, priors);
			psp.transform(transformData);
			psp.calculateDistance(dataStatPair, truncatStats);

			if (dumpAll && allResults!=null) {
				// write out reps simulations in a gzip file...
				writeResult(psp,allResults);
			}

			if (!sampledPoints.isEmpty() && psp.getDistance() < sampledPoints.last().getDistance()) {
				sampledPoints.add(psp);
				sampledPoints.pollLast();
			}
			if (mcmc && psp.getDistance() < epsilon) {
				currentState = psp;
			}
			if (r % 100 == 0 ) {
				if( !sampledPoints.isEmpty()){
				System.out.println("Completed:" + r + " out of " + reps + "\tDistance Range:" + sampledPoints.first().getDistance() + " <-->" + sampledPoints.last().getDistance());
				}else{
					System.out.println("Completed:"+r+" out of " + reps);
				}
				if (mcmc)
					System.out.println("MCMC Distance:" + currentState.getDistance() + "\teps:" + epsilon);
			}
			if (r % 100 == 0 && mcmc) {
				chainPoints.add(currentState);
				if (chainPoints.size() >= 10)
					saveChain();
			}

			if (r % priorUpdate == 0 && r >= priorUpdate) {
				updatePriors(priors);

				// transformData =
				// ParameterStatPair.calculateTransformData(bootstrapPoints,
				// false, pcaNormalzation, plsNormalzation);
				// dataStatPair.transform(transformData);
				// TreeSet<ParameterStatPair> nSampledPoints=new
				// TreeSet<ParameterStatPair>();
				// for(ParameterStatPair p:sampledPoints){
				// p.transform(transformData);
				// p.calculateDistance(dataStatPair);
				// nSampledPoints.add(p);
				// }
				// sampledPoints=nSampledPoints;
			}
			if (!sampledPoints.isEmpty() && r % sampleSize == 0) {
				saveResultState();
				updateStds(priors);
			}
		}
		saveResultState();
		if(allResults!=null){
			try {
				allResults.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeResult(ParameterStatPair psp, Writer allResults) {
		try {
			allResults.write(psp.toString()+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void updateStds(List<PriorDensity> priors) {
		double[] sum = new double[priors.size()];
		double[] sum2 = new double[priors.size()];
		for (ParameterStatPair psp : sampledPoints) {
			double[] params = psp.getParameters();
			for (int i = 0; i < sum.length; i++) {
				sum[i] += params[i];
				sum2[i] += params[i] * params[i];
			}
		}
		int n = sampledPoints.size();
		for (int i = 0; i < stds.length; i++) {
			double mu = sum[i] / n;
			stds[i] = Math.sqrt((sum2[i] / n) - (mu * mu));
		}
	}

	private ParameterStatPair getDataStatPair(List<StatsCollector> dataStats) {
		ArrayList<Double> stats = new ArrayList<Double>();
		for (StatsCollector sc : dataStats) {
			double[] s = sc.summaryStats();
			for (double d : s) {
				stats.add(d);
			}
		}
		ParameterStatPair dpsp = new ParameterStatPair(null, Util.toArrayPrimitiveDouble(stats));
		return dpsp;
	}

	private void updatePriors(List<PriorDensity> priors) {
		// simply the bounds for every prior.
		double[] mins = new double[priors.size()];
		double[] maxs = new double[priors.size()];
		for (int i = 0; i < mins.length; i++) {
			PriorDensity pd = priors.get(i);
			mins[i] = pd.getMax();
			maxs[i] = pd.getMin();
		}
		for (ParameterStatPair psp : sampledPoints) {
			double[] params = psp.getParameters();
			for (int i = 0; i < mins.length; i++) {
				mins[i] = Math.min(mins[i], params[i]);
				maxs[i] = Math.max(maxs[i], params[i]);
			}
		}
		for (int i = 0; i < mins.length; i++) {
			PriorDensity pd = priors.get(i);
			if (pd instanceof CopyPriorDensity)
				continue;
			pd.updateMinMax(mins[i], maxs[i]);
			System.out.println("UpdateMinMax:" + mins[i] + "\t" + maxs[i] + "\tspred:" + stds[i]);
		}
	}

	private void saveChain() {
		try {
			Writer writer = new FileWriter(chainFileName, true);
			for (ParameterStatPair psp : chainPoints) {
				writer.write(psp.toString() + "\n");
			}
			writer.close();
			chainPoints.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveResultState() {
		if(sampledPoints.isEmpty())
			return;
		try {
			Writer writer = new FileWriter(outputFileName);
			for (ParameterStatPair psp : sampledPoints) {
				writer.write(psp.toString() + "\n");
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// give mode estimates for all the parameters. including copy params.
		double[] sorted = new double[sampleSize];
		int paramCount = sampledPoints.first().getParameters().length;
		double[] modeEsts = new double[paramCount];
		double[] percentiles = new double[paramCount];
		int window = sampleSize / 50;// 2%
		for (int i = 0; i < paramCount; i++) {
			int index = 0;
			for (ParameterStatPair psp : sampledPoints) {
				sorted[index++] = psp.getParameters()[i];
			}
			Arrays.sort(sorted);
			double bestEst = 0;
			double width = Double.MAX_VALUE;
			for (int j = 0; j < sampleSize - window; j++) {
				if (width > sorted[j + window] - sorted[j]) {
					width = sorted[j + window] - sorted[j];
					bestEst = (sorted[j + window] + sorted[j]) / 2;
				}
			}
			modeEsts[i] = bestEst;
			percentiles[i] = Double.NaN;
			try {
				if (writeArgs != null) {
					double truth = Double.parseDouble(writeArgs[i]);
					int pos = Arrays.binarySearch(sorted, truth);
					pos = pos < 0 ? -pos + 1 : pos;// so we now have the
													// insertion location.
					percentiles[i] = (double) pos / sorted.length;
				}
			} catch (NumberFormatException nfe) {

			}
		}

		try {
			Writer writer = new FileWriter(outputFileName + ".mode");
			for (double v : modeEsts) {
				writer.write(v + "\t");
			}
			writer.write("\n");
			for (double v : percentiles) {
				writer.write(v + "\t");
			}
			writer.write("\n");
			writer.close();
		} catch (IOException e) {

		}

	}

	private double[] collectStatitics(List<StatsCollector> stats) {
		double[][] all = new double[stats.size()][0];
		int count = 0;
		for (int i = 0; i < stats.size(); i++) {
			StatsCollector stat = stats.get(i);
			all[i] = stat.summaryStats();
			count += all[i].length;
		}
		double[] allLinear = new double[count];
		int index = 0;
		for (int i = 0; i < all.length; i++) {
			System.arraycopy(all[i], 0, allLinear, index, all[i].length);
			index += all[i].length;
		}
		return allLinear;
	}

	private double[] distance(StatsCollector sim, StatsCollector data) {
		double[] s = sim.summaryStats();
		double[] d = data.summaryStats();
		double[] delta = new double[s.length];
		assert s.length == d.length : sim + "\t" + data + "\t" + s.length + "\t" + d.length;

		for (int i = 0; i < s.length; i++) {
			delta[i] = s[i] - d[i];

		}
		return delta;
	}

	private void initStatCollectors(String[] msmsArgs) {
		CommandLineMarshal msmsparser = new CommandLineMarshal();
		try {
			CmdLineParser<CommandLineMarshal> marshel = CommandLineMarshal.getCacheParser();
			marshel.processArguments(msmsArgs, msmsparser);
			SampleConfiguration sampleConfig = msmsparser.getSampleConfig();
			// for (StatsCollector stat : collectionStats)
			// stat.init(sampleConfig);FIXME?
			List<StatsCollector> defaultCollectors = msmsparser.getStatsCollectors();
			this.collectionStats.addAll(defaultCollectors);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void paste(String[] args, List<PriorDensity> priors, boolean proposial, double[] values) {
		do {
			for (int i = 0; i < priors.size(); i++) {
				PriorDensity pd = priors.get(i);
				double value = 0;//
				if (proposial) {
					value = pd.nextProp(values[i]);
				} else {
					pd.generateRandom();
					value = pd.getValue();
				}
				args[pd.getArgIndex()] = "" + value;
			}
		} while (isInvalid(priors));

	}

	private boolean isInvalid(List<PriorDensity> priors) {
		for (PriorDensity pd : priors) {
			if (!pd.isValid())
				return true;
		}
		return false;
	}

	private void pasteFancy(String[] args, List<PriorDensity> priors) {
		final double scale = 1.06 / Math.pow(this.sampleSize, 1.0 / priors.size());
		int randIndex = -1;// random.nextInt(priors.size());
		ArrayList<ParameterStatPair> randomlist = new ArrayList<ParameterStatPair>(sampledPoints);
		do {
			for (int i = 0; i < priors.size(); i++) {
				PriorDensity pd = priors.get(i);
				double value = Double.MAX_VALUE;//

				if (pd instanceof CopyPriorDensity) {
					value = pd.getValue();
				} else if (i == randIndex) {
					pd.generateRandom();
					value = pd.getValue();
				} else {
					ParameterStatPair psp = randomlist.get(random.nextInt(randomlist.size()));
					while (value > pd.getMax() || value < pd.getMin())
						value = psp.getParameters()[i] + scale * random.nextGaussian() * stds[i];
					pd.setValue(value);
					value = pd.getValue();// clamp just in case
				}

				args[pd.getArgIndex()] = "" + value;
			}
		} while (isInvalid(priors));
	}

	private void initDataFile() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFileName));
			dataStats.clear();
			loadStats(reader, dataStats);

			Collections.sort(dataStats, new ClassNameOrder());
			Collections.sort(collectionStats, new ClassNameOrder());
			// we now remove anything that dataStats has that Collection doesn't
			// and vice versa
			ListIterator<StatsCollector> iter = dataStats.listIterator();
			while (iter.hasNext()) {
				StatsCollector stat = iter.next();
				if (!containsClass(stat.getClass(), collectionStats)) {
					System.out.println("warrning: Stat in Data ignored:" + stat);
					iter.remove();
				}
			}
			iter = collectionStats.listIterator();
			while (iter.hasNext()) {
				StatsCollector stat = iter.next();
				if (!containsClass(stat.getClass(), dataStats)) {
					System.out.println("warrning: Stat in Collection ignored:" + stat);
					iter.remove();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean containsClass(Class type, List list) {
		for (Object o : list) {
			if (o.getClass().equals(type))
				return true;
		}
		return false;
	}

	private void writeDataStats(List<StatsCollector> stats) {
		try {
			Writer writer = new FileWriter(dataFileName);
			saveStats(writer, stats);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

	private void loadStats(BufferedReader reader, List<StatsCollector> stats) throws IOException {
		YamlReader yamlReader = new YamlReader(reader);
		yamlReader.getConfig().setPrivateFields(true);
		yamlReader.getConfig().setPrivateConstructors(true);
		List<StatsCollector> readStats = (List<StatsCollector>) yamlReader.read();
		if (readStats != null)
			stats.addAll(readStats);
		yamlReader.close();
		System.out.println(stats);
	}

	@CLNames(names = { "-msms", "-MSMS" }, rank = 2, required = true)
	@CLDescription("Set the command line for msms in a anontated format. The anotation is FromValue%toValue[%lg] for all parameters that are extimated.")
	@CLUsage("-msms {any valid command line}")
	public void setMSMSAnotatedCmdLine(String[] anotatedCmdLine) {
		this.anotatedCmdLine = anotatedCmdLine;
	}

	@CLNames(names = { "-data", "-DATA" }, required = true)
	@CLDescription("file name of a valid summary statistics data. Note statics that are in this file that are not specified via the -Stat options, are ignored.")
	@CLUsage("filename")
	public void setDataFileName(String file) {
		dataFileName = file;
	}

	@CLNames(names = { "-o", "-outfile", "-results" }, required = true)
	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	@CLNames(names = { "-pca" })
	@CLDescription("PCA of the stats.")
	public void setPCATrue() {
		this.pcaNormalzation = true;
	}
	
	@CLNames(names = { "-dump" })
	@CLDescription("Store all simulations in a gzip dump file")
	public void setDumpAllTrue() {
		this.dumpAll = true;
	}

	@CLNames(names = { "-pls" })
	@CLDescription("Automagic PLS of the stats. Doesn't really work. But then PLS doesn't really anyway")
	public void setPLSTrue() {
		this.plsNormalzation = true;
	}

	@CLNames(names = { "-mcmc" })
	@CLDescription("mcmc version of abc. All thats bad with abc combined with all that is hard with mcmc. Not enought tunining options to use.")
	public void setMcmcTrue() {
		this.mcmc = true;
	}

	@CLNames(names = { "-chainFile" })
	@CLDescription("mcmc output file when mcmc is enabled.")
	public void setChainFileName(String chainFileName) {
		this.chainFileName = chainFileName;
	}

	@CLNames(names = { "-truncate" })
	@CLDescription("ignore the last X summary stats")
	public void setTruncatStats(int truncatStats) {
		this.truncatStats = truncatStats;
	}

	@CLNames(names = { "-abcstat", "-STAT" })
	@CLDescription("Old way of adding stats. Deprecated")
	public void addStat(String[] statAndConfig) {
		String statName = statAndConfig[0];
		// turn this into a class in 2 ways. if it contains no . try using this
		// package as a prefix
		Class type = null;
		try {
			String packageName = this.getClass().getPackage().getName();
			type = Class.forName(packageName + "." + statName);
		} catch (ClassNotFoundException e) {
			try {
				type = Class.forName(statName);
			} catch (ClassNotFoundException e1) {
				throw new RuntimeException("Could not find ABCStat class " + statName);
			}
		}
		if (!StatsCollector.class.isAssignableFrom(type)) {
			throw new RuntimeException("Specified -stat class does not load a ABCStat object but rather a " + type);
		}
		StatsCollector stat = null;
		try {
			stat = (StatsCollector) type.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Specified -stat class does not have a default constructor", e);
		}

		if (statAndConfig.length == 1) {
			collectionStats.add(stat);
			return;
		}

		try {
			CmdLineParser<StatsCollector> parser = new CmdLineParser<StatsCollector>((Class<StatsCollector>) stat.getClass());
			if (statAndConfig.length == 2 && statAndConfig[1].contains("help")) {
				System.err.println("Help for statCollector:" + statName + "\n" + parser.longUsage());
				return;
			}
			String[] args = new String[statAndConfig.length - 1];
			System.arraycopy(statAndConfig, 1, args, 0, args.length);
			// System.out.println("StatsARGS:"+Arrays.toString(args));
			parser.processArguments(args, stat);
			// System.out.println("Object:"+stat+"\t"+parser.longUsage());
		} catch (CmdLineBuildException e) {
			throw new RuntimeException(statName + " does not take options or we have an error", e);
		} catch (CmdLineParseException e) {
			throw new RuntimeException("Error With stats options:" + statName, e);
		}
		collectionStats.add(stat);

	}

	@CLNames(names = { "-keep" })
	@CLDescription("Keep this many \"best of\" simulations. Based on euclidain distance")
	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}

	public int getSampleSize() {
		return sampleSize;
	}

	@CLNames(names = { "-reps" })
	@CLDescription("Total number of iterations/simulations to carry out")
	public void setReps(int reps) {
		this.reps = reps;
	}

	public int getReps() {
		return reps;
	}

	@CLNames(names = { "-pReductions", "-pR" })
	@CLDescription("Unproven faster convergance method. Should still contain the MAP with high probablity. NOT TESTED")
	public void setReductions() {
		this.reductions = true;
	}

	@CLNames(names = { "-write" })
	@CLDescription("Simulate data with these options. Overwrites -data file")
	public void setWriteDataTrue(String[] writeArgs) {
		this.writeData = true;
		this.writeArgs = writeArgs;
	}

	@CLNames(names = { "-hackSample" })
	@CLDescription("No idea what this does. don't use it")
	public void setHackSampleTrue() {
		this.hackSample = true;
	}

	@CLNames(names = { "-priorUpdateInterval", "-pui" })
	@CLDescription("How long to adjust the bounds of the sampler? ie sample the AABB of the current points.")
	public void setPriorUpdate(int priorUpdate) {
		this.priorUpdate = priorUpdate;
	}

	public static void main(String[] args) {
		NewABC abc = new NewABC();
		CmdLineParser<NewABC> parser = null;
		try {
			parser = new CmdLineParser<NewABC>(NewABC.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println("ARGS:\n" + Arrays.toString(args));
			return;
		}

		try {
			parser.processArguments(args, abc);
			abc.run();
		} catch (Exception e) {
			System.err.println(parser.longUsage());
			System.out.println("ARGS:\n" + Arrays.toString(args));
			e.printStackTrace();

		}
	}

	private static class ClassNameOrder implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			String name1 = o1.getClass().getName();
			String name2 = o2.getClass().getName();
			return name1.compareTo(name2);
		}
	}

}
