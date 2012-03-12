package at.mabs.abc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
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

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

/**
 * same idea as ABC. Use a annotated command line and a few parameters to add.
 * However output is much smaller. There is a Trace and just the output
 * parameters. File and std out are the bulk options. Still a lot of cut/paste
 * from NewABC. Need to refactor.
 * 
 * @author bob
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SGA {
	private static final double MINLOG=Math.log(Double.MIN_VALUE);
	// private double gammac =20;

	private double[] gammacs = { 50, 50, 50 };

	// private double gammaa =100e5;
	private double[] gammaas;

	//private int n = 400;
	// private int bootstrap =100;

	private String[] anotatedCmdLine;

	private String dataFileName = "data";

	private boolean writeData;// good for simulations.
	private String[] writeArgs;

	private List<StatsCollector> collectionStats = new ArrayList<StatsCollector>();
	private List<StatsCollector> dataStats = new ArrayList<StatsCollector>();
	private double[] data;

	private Random random = new Random64();

	// private Kernal kernal = new Kernal.Gaussian();

	public void run() {
		List<PriorDensity> priors = new ArrayList<PriorDensity>();
		int realParamCount = 0;
		for (int i = 0; i < anotatedCmdLine.length; i++) {
			String arg = anotatedCmdLine[i];
			if (arg.contains("%") && !arg.startsWith("%")) {
				priors.add(new PriorDensity(arg, i));
				realParamCount++;
			} else if (arg.startsWith("%")) {
				int code = Integer.parseInt(arg.substring(1));
				priors.add(new CopyPriorDensity(priors.get(code), i));
			}
		}
		// now we have our parameter ranges...
		// next we bootstrap for our inital value.
		String[] args = anotatedCmdLine.clone();
		paste(args, priors, true);
		initStatCollectors(args);

		if (writeData) {
			int argIndex = 0;
			for (PriorDensity pd : priors) {
				args[pd.getArgIndex()] = writeArgs[argIndex++];
			}
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			writeDataStats(collectionStats);
			System.out.println("Wrote out Stats:" + Arrays.toString(args));
			// return;
		}

		initDataFile();

		//densityGrid(args, priors, 25, 20);
		double[] start=randomPoint(priors);
		genericKWAlgo(args, priors, start, 10, 10000, new GradFunction());
	}
	
	private double[] randomPoint(List<PriorDensity> priors) {
		double[] p=new double[priors.size()];
		for(int i=0;i<p.length;i++){
			p[i]=priors.get(i).next();
		}
		return p;
	}

	/**
	 * basic kw algo from a single point till stopping conditions are meet. 
	 * @param args
	 * @param priors
	 * @param n
	 * @param maxK
	 */
	private void genericKWAlgo(String[] args,List<PriorDensity> priors,double[] start,int n,int maxK, GradFunction grad){
		double alpha=.602;
		double gamma=.101;
		int n2=100;
		//first we need to get a c starting value;
		double sum=0;
		double sum2=0;
		//paste(args,priors,start);
		for(int i=0;i<n2;i++){
			double d=(density(args, priors, start, n));
			if(Double.isInfinite(d) || Double.isNaN(d)){
				d=Double.MIN_VALUE;//MINLOG;
			}
			sum+=d;
			sum2+=d*d;
		}
		double mean=sum/n2;
		
		double c=Math.sqrt((sum2/n2)-mean*mean);
		
		int A=maxK/10;
		
		//now for a
		double[] nabla=grad.grad(args, priors, start, c, n);
		System.out.println("Nab:"+Arrays.toString(nabla));
		//assume a b_i==1;
		double a=Double.MAX_VALUE;
		for(int i=0;i<nabla.length;i++){
			a=Math.min(a, 10*Math.pow(A+1,alpha)/Math.abs(nabla[i]));
		}
		if(a<=0){
			a=1; //just a basic sanity check, should barf
		}
		//now we have a.
		System.out.println("a:"+a+"\tc:"+c);
		System.out.println("init:"+Arrays.toString(start));
		double[] x=start.clone();
		for(int k=0;k<maxK;k++){
			double a_k=a/Math.pow(k+A+1,alpha);
			double c_k=c/Math.pow(k+1,gamma);
			System.out.println("k, a_k & c_k\t"+k+"\t"+a_k+"\t"+c_k);
			
			nabla=grad.grad(args, priors, x, c_k, n);
			mul(nabla, a_k, nabla);
			add(x,nabla,x);
			clamp(x,priors);
			System.out.println(Arrays.toString(x));
		}
		
	}

	private void densityGrid(String[] args, List<PriorDensity> priors, int n, int gridPoints) {
		double[] x = new double[priors.size()];
		double[] delta = new double[priors.size()];
		for (int i = 0; i < x.length; i++) {
			PriorDensity pd = priors.get(i);
			x[i] = pd.getMin();
			delta[i] = (pd.getMax() - pd.getMin()) / gridPoints;
		}
		double stopV = priors.get(priors.size() - 1).getMax();
		double[] bestEst = new double[priors.size()];
		double best = Double.NEGATIVE_INFINITY;
		while (x[x.length - 1] < stopV) {

			double density = density(args, priors, x, n);
			if (density > best) {
				best = density;
				System.arraycopy(x, 0, bestEst, 0, x.length);
			}
			for (int i = 0; i < x.length; i++) {
				System.out.print(x[i] + "\t");
			}
			System.out.println(Math.log(density));

			x[0] += delta[0];
			// ripple counter
			for (int i = 0; i < x.length - 1; i++) {
				if (x[i] < priors.get(i).getMax())
					break;
				x[i] = priors.get(i).getMin();
				x[i + 1] += delta[i + 1];
			}
		}
		System.out.println("\nBestEstimate:" + Arrays.toString(bestEst) + " @" + Math.log(best));
	}

	private double[] gradApprox(String[] args, List<PriorDensity> priors, double[] x, double delta, int n) {
		double[] xp = x.clone();
		double[] nabla = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			xp[i] = x[i] + delta;
			
			paste(args, priors, xp);// need to set n....
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			double[] sp = collectStatitics(collectionStats);
			
			xp[i] = x[i] - delta;

			paste(args, priors, xp);// need to set n....
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			double[] sn = collectStatitics(collectionStats);

			nabla[i]=(norm2(sp,data)-norm2(sn,data))/(2*delta);
			xp[i] = x[i];
		}
		return nabla;
	}
	
	private double norm2(double[] a,double[] b){
		double acc=0;
		for(int i=0;i<a.length;i++){
			double d=a[i]-b[i];
			acc+=d*d;
		}
		return acc;
	}

	private double[] gradFull(String[] args, List<PriorDensity> priors, double[] x, double delta, int n) {
		double[] xp = x.clone();
		double[] nabla = new double[x.length];
		for (int i = 0; i < xp.length; i++) {
			xp[i] = x[i] + delta;
			double lp = density(args, priors, xp, n);
			xp[i] = x[i] - delta;
			double ln = density(args, priors, xp, n);
			nabla[i] = (Math.log(lp) - Math.log(ln)) / (2 * delta);
			xp[i] = x[i];
		}
		return nabla;
	}

	/*
	 * the gradent in a single line. ie +-delta
	 */
	private double[] gradSlice(String[] args, List<PriorDensity> priors, double[] x, double[] delta, double deltaNorm, int n) {
		// generate a l esitmate for x+delta and x-delta;
		// the result is delta* (l(x+delta)-l(x-delta))/2||delta||
		double[] xp = x.clone();
		add(x, delta, xp);
		double lplus = density(args, priors, xp, n);
		sub(x, delta, xp);
		double lneg = density(args, priors, xp, n);

		double nabla = (Math.log(lplus) - Math.log(lneg)) / (2 * deltaNorm);
		mul(delta, nabla, xp);
		return xp;
	}

	private void add(double[] x, double[] y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] + y[i];
		}
	}

	private void sub(double[] x, double[] y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] - y[i];
		}
	}

	private void mul(double[] x, double y, double[] r) {
		for (int i = 0; i < r.length; i++) {
			r[i] = x[i] * y;
		}
	}

	private double density(String[] args, List<PriorDensity> priors, double[] x, int n) {
		// esitmate the denstiy at x
		assert n > 1;
		double[][] stats = new double[n][0];
		
		
		paste(args, priors, x);// need to set n....
		for (int i = 0; i < n; i++) {
			//System.out.println("ARGS:"+Arrays.toString(args));
			MSLike.main(args, null, (List<? extends StatsCollector>) collectionStats, new NullPrintStream(), null);
			stats[i] = collectStatitics(collectionStats);
			// System.out.println("CollectedStats:"+Arrays.toString(stats[i]));

		}
		double[] stds = new double[stats[0].length];
		double[] means = new double[stds.length];
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			for (int j = 0; j < means.length; j++) {
				double d = data[j] - stat[j];
				stds[j] += d * d;
				means[j] += d / n;
			}
		}
		double nfactor2 = Math.pow(n, -2 / (4 + data.length));// note it is
																// squared
		double sigmaNorm = 1;
		for (int i = 0; i < stds.length; i++) {
			stds[i] = stds[i] / n - means[i] * means[i] * nfactor2;
			sigmaNorm *= Math.sqrt(stds[i]);
			// System.out.println("BW:"+Math.sqrt(stds[i]));
		}
		// now stds has bw^2 for everything.
		double ksum = 0;
		for (int i = 0; i < n; i++) {
			double[] stat = stats[i];
			double sum = 0;
			for (int j = 0; j < data.length; j++) {
				double d = data[j] - stat[j];
				sum += d * d / stds[j];
			}
			ksum += Math.exp(-.5 * sum);
		}
		double dens= ksum / (sigmaNorm * Math.pow(2 * Math.PI, data.length / 2));
		if(dens==0 || Double.isNaN(dens))
			return Double.MIN_VALUE;
		return dens;
	}

	private void clamp(double[] x,List<PriorDensity> priors){
		for(int i=0;i<x.length;i++){
			PriorDensity pd=priors.get(i);
			pd.setLastValue(x[i]);
			x[i]=pd.getLastValue();
		}
	}
	
	private void paste(String[] args, List<PriorDensity> priors, double[] values) {
		clamp(values,priors);
		for (int i = 0; i < priors.size(); i++) {
			PriorDensity pd = priors.get(i);
			double value = values[i];//
			pd.setLastValue(value);
			value = pd.getLastValue();
			args[pd.getArgIndex()] = "" + value;
		}
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
		assert s.length == d.length;

		double dis = 0;
		for (int i = 0; i < s.length; i++) {
			delta[i] = s[i] - d[i];

		}
		return delta;
	}

	private void paste(String[] args, List<PriorDensity> priors, boolean rand) {
		for (int i = 0; i < priors.size(); i++) {
			PriorDensity pd = priors.get(i);
			double value = pd.getLastValue();
			if (rand)
				value = pd.next();
			args[pd.getArgIndex()] = "" + value;
		}
	}

	private void initStatCollectors(String[] msmsArgs) {
		CommandLineMarshal msmsparser = new CommandLineMarshal();
		try {
			CmdLineParser<CommandLineMarshal> marshel = new CmdLineParser<CommandLineMarshal>(msmsparser);
			marshel.processArguments(msmsArgs);
			SampleConfiguration sampleConfig = msmsparser.getSampleConfig();
			// for (StatsCollector stat : collectionStats)
			// stat.init(sampleConfig); //FIXME
			List<StatsCollector> defaultCollectors = msmsparser.getStatsCollectors();

			this.collectionStats.addAll(defaultCollectors);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
			List<Double> rawData = new ArrayList<Double>();
			for (StatsCollector sc : dataStats) {
				double[] dd = sc.summaryStats();
				for (double d : dd) {
					rawData.add(d);
				}
			}
			this.data = Util.toArrayPrimitiveDouble(rawData);
			System.out.println(Arrays.toString(data));
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

	private void loadStats(BufferedReader reader, List<StatsCollector> stats) throws IOException {
		YamlReader yamlReader = new YamlReader(reader);
		yamlReader.getConfig().setPrivateFields(true);
		yamlReader.getConfig().setPrivateConstructors(true);
		List<StatsCollector> readStats = (List<StatsCollector>) yamlReader.read();
		if (readStats != null)
			stats.addAll(readStats);
		yamlReader.close();
	}

	private void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

	@CLNames(names = { "-msms", "-MSMS" }, rank = 2, required = true)
	@CLDescription("Set the command line for msms in a anontated format. The anotation is FromValue%toValue[%lg] for all parameters that are extimated.")
	@CLUsage("-msms {any valid command line}")
	public void setMSMSAnotatedCmdLine(String[] anotatedCmdLine) {
		this.anotatedCmdLine = anotatedCmdLine;
	}

	@CLNames(names = { "-abcstat", "-STAT" })
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
			CmdLineParser<StatsCollector> parser = new CmdLineParser<StatsCollector>(stat);
			if (statAndConfig.length == 2 && statAndConfig[1].contains("help")) {
				System.err.println("Help for statCollector:" + statName + "\n" + parser.longUsage());
				return;
			}
			String[] args = new String[statAndConfig.length - 1];
			System.arraycopy(statAndConfig, 1, args, 0, args.length);
			// System.out.println("StatsARGS:"+Arrays.toString(args));
			parser.processArguments(args);
			// System.out.println("Object:"+stat+"\t"+parser.longUsage());
		} catch (CmdLineBuildException e) {
			throw new RuntimeException(statName + " does not take options or we have an error", e);
		} catch (CmdLineParseException e) {
			throw new RuntimeException("Error With stats options:" + statName, e);
		}
		collectionStats.add(stat);

	}

	@CLNames(names = { "-write" })
	public void setWriteDataTrue(String[] writeArgs) {
		this.writeData = true;
		this.writeArgs = writeArgs;
	}

	public static void main(String[] args) {
		// Matrix matrix=new Matrix(new double[] {-.75,.25,.25,.25
		// ,.25,-.75,.25,.25 ,.25,.25,-.75,.25 ,.25,.25,.25,-.75},4);
		// long time=System.nanoTime();
		// double[] d=null;
		// Matrix m=null;
		// EigenvalueDecomposition evd=matrix.eig();
		// Matrix vt=evd.getV().transpose();
		// for(int i=0;i<1000000;i++){
		// //d=evd.getRealEigenvalues();
		// m=evd.getV().times(evd.getD()).times(vt);
		// }
		// System.out.println("EIGEN:"+(System.nanoTime()-time)*1e-9);
		// m.print(m.getRowDimension(), m.getColumnDimension());
		SGA sga = new SGA();
		CmdLineParser<SGA> parser = null;
		try {
			parser = new CmdLineParser<SGA>(sga);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {
			parser.processArguments(args);
			sga.run();
		} catch (Exception e) {
			System.err.println(parser.longUsage());
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
	
	public  class GradFunction {
		public double[] grad(String[] args, List<PriorDensity> priors, double[] x, double delta, int n){
			return gradFull(args, priors, x, delta, n); 
		}
	}
}
