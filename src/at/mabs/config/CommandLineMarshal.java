/*
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
package at.mabs.config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.MSLike;
import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CLUsage;
import at.mabs.cmdline.CmdLineBuildException;
import at.mabs.cmdline.CmdLineParseException;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.cmdline.InitFinishParserObject;
import at.mabs.coalescent.EventTracker;
import at.mabs.model.ConstantPopulation;
import at.mabs.model.DemeJoinEvent;
import at.mabs.model.DemeSplitEvent;
import at.mabs.model.ExpPopulationEvent;
import at.mabs.model.FrequencyState;
import at.mabs.model.MigrationChangeEvent;
import at.mabs.model.MigrationMatrixEvent;
import at.mabs.model.ModelEvent;
import at.mabs.model.PopulationSizeEvent;
import at.mabs.model.SampleConfiguration;
import at.mabs.model.selection.BasicFrequencyCondition;
import at.mabs.model.selection.DefaultSelectionSimulator;
import at.mabs.model.selection.FrequencyCondition;
import at.mabs.model.selection.NullSelectionSimulator;
import at.mabs.model.selection.RestartCondition;
import at.mabs.model.selection.SelectionEndTimeEvent;
import at.mabs.model.selection.SelectionSimulator;
import at.mabs.model.selection.SelectionStrengthEvent;
import at.mabs.model.selection.SelectionStrengthModel;
import at.mabs.model.selection.SelectionTimeConditionEvent;
import at.mabs.segment.FixedBitSet;
import at.mabs.simulators.ReaderSelection;
import at.mabs.simulators.RedQueenSelection;
import at.mabs.simulators.SinwaveSelection;
import at.mabs.stats.AlleleFrequencySpectrum;
import at.mabs.stats.Fst;
import at.mabs.stats.StatsCollector;
import at.mabs.stats.StringStatsCollector;
import at.mabs.stats.ThetaEstimators;
import at.mabs.util.Util;

/**
 * the new command line object. More monolithic... but also easer to extend and work with.
 * 
 * @author greg
 * 
 */
public class CommandLineMarshal implements InitFinishParserObject{
	private static final ThreadLocal<CmdLineParser<CommandLineMarshal>> parserCache=new ThreadLocal<CmdLineParser<CommandLineMarshal>>(){
		@Override
		protected CmdLineParser<CommandLineMarshal> initialValue() {
		
			CmdLineParser<CommandLineMarshal> clm=null;
			try {
				clm = new CmdLineParser<CommandLineMarshal>(CommandLineMarshal.class);
			} catch (CmdLineBuildException e) {
				e.printStackTrace();
			}
			
			return clm;
		}
	};
	
	public static String[] HACK_PARAMS;//For hacked tests and options. Typically unused.
	private List<ModelEvent> events =new LinkedList<ModelEvent>();

	private List<StringStatsCollector> stringStatsCollectors =new LinkedList<StringStatsCollector>();
	private List<StatsCollector> statsCollectors =new LinkedList<StatsCollector>();

	/*
	 * no need to repeat what was working so well in the last case..
	 */
	
	private boolean hasOutgroup = true;
	
	private boolean isPhased = true;
	private double N0 =Integer.MAX_VALUE;// the base scale for everything.
	

	private int repeats;
	
	/**
	 * do we have a -I or -IT option?
	 */
	private boolean flagI;
	
	private SampleConfiguration sampleConfig;
	
	private EventTracker eventTracker;

	private double defaultMigration;

	private double theta;
	private double recombinationRate;

	private long recombinationCutSites;
	private double forwardMutationRate;

	private double backwardMutationRate;
	private double sAA;
	private double saA;

	private double saa;

	private double allelePostion;
	private int seedDeme=-1;
	
	private boolean noSequence;
	//private boolean summaryStats;

	private boolean originCount;

	private boolean trackTrees;

	private boolean help;

	private double[] nLoci = { 0, 1 };

	private boolean timeOption;

	private boolean noCoalescent;

	private boolean frequencyTrace;
	private long seed;

	private boolean isSeedSet;
	private boolean isCondtionalMutation;

	private double segSiteCount;

	private FrequencyCondition stoppingCondition=new BasicFrequencyCondition(-1, -1, 1);//fixation is good

	private boolean markSelectedMutatations;

	private int threadCount;

	private SelectionSimulator selectionSimulator =new DefaultSelectionSimulator();

	private boolean batchMode =false;

	private boolean selection =false;

	private boolean timeInvarient =true;
	
	private boolean printTableStats=true;
	
	private RestartCondition restartCondition=new RestartCondition.Default();
	
	private boolean foldMutations=false;
	
	private boolean weightedMutations=false;
	
	private double maxRecombinationRate=Double.MAX_VALUE;
	
	public CommandLineMarshal() {
	}

	@Override
	public void init() {
		
	}
	
	@Override
	public void finished() {
		//System.out.println("StartEvents:"+events);
		events.addAll(sampleConfig.getAllNewSampleEvents());
		//System.out.println("EndEvents:"+events);
		//Collections.sort(events);
		//System.out.println("SortedEvents:"+events);
	}
	
	
	@CLNames(names ={ "-oAFS", "-oSFS" }, rank =1)
	@CLUsage("[jAFS] [onlySummary]")
	@CLDescription("Collect allele frequency spectrum data. Also includes joint Allele frequencys and only summary options")
	public void addAFSStatsCollector(String[] args) {
		boolean jAFS =false;
		boolean onlySummary =false;
		if (args != null) {
			for (String s : args) {
				if ("jAFS".equals(s)) {
					jAFS =true;
				} else if ("onlySummary".equals(s)) {
					onlySummary =true;
				} else {
					System.err.println("Warning:" + s + " not used in -oAFS option");
				}
			}
		}
		stringStatsCollectors.add(new AlleleFrequencySpectrum(jAFS, true, onlySummary, sampleConfig));
	}
	
	

	@CLNames(names ={ "-ej" },rank=1)
	@CLUsage("t demeI demeJ")
	@CLDescription("Join deme I to J. The outward migration rates of deme I are set to zero. Population parameters are left unchanged.")
	public void addDemeJoin(double time, int demeI, int demeJ) {
		if (!flagI) {
			throw new RuntimeException("setting deme parameters when no deme sampling has been specified (ie -I option)");
		}
		events.add(new DemeJoinEvent(Math.round(time * 4 * N0), demeI - 1, demeJ - 1));
		if(time!=0)
			timeInvarient =false;
	}

	@CLNames(names ={ "-es" })
	@CLUsage("t deme p")
	@CLDescription("split the deme into 2 demes at the given time, the new deme is labeled npop. Lineages move to the new deme with probablity 1-p")
	public void addDemeSplit(double time, int deme, double keepP) {
		if (!flagI) {
			throw new RuntimeException("setting deme parameters when no deme sampling has been specified (ie -I option)");
		}
		events.add(new DemeSplitEvent(Math.round(time * 4 * N0), deme - 1, keepP, N0));
		if(time!=0)
			timeInvarient =false;
	}

	@CLNames(names ={ "-G" }) 
	@CLUsage("alpha")
	@CLDescription("Set the expontail growth rate in forward time to alpha. N/N_i = exp{-alpha*t}")
	public void addExpEvent(double alpha) {
		addExpEvent(0, 0, alpha);// zero for all demes
		
	}

	@CLNames(names ={ "-eG" })
	@CLUsage("t alpha")
	@CLDescription("Set the expontail growth rate in forward time to alpha for all demes from time t. N/N_i = exp{-alpha*t}")
	public void addExpEvent(double time, double alpha) {
		addExpEvent(time, 0, alpha);// zero for all demes
		
	}


	@CLNames(names ={ "-eng2s" })
	@CLDescription("set a deme to have exponetial growth, parameterized start size and by final size. This acts as a -en and -eg option")
	@CLUsage("t deme startSize finalT finalSize")
	public void addExpEventSize(double time, int deme, double startSize,double finalT,double finalSize) {
		if(finalT<=time){
			throw new RuntimeException("Can't have a final time less pastward than time!:"+time+"\t"+finalT);
		}
		double alpha=-Math.log(finalSize/startSize)/(finalT-time);
		//now we just apply the 2 real options.
		//System.err.println("OUR ALPHA##:"+alpha);
		addSizeChangeEvent(time, deme,startSize);
		addExpEvent(time,deme,alpha);
	}
	
	
	@CLNames(names ={ "-eg" })
	@CLDescription("set a deme to have exponetial growth in forward time starting from time t. N/N_i = exp{-alpha*t}")
	@CLUsage("t deme alpha")
	public void addExpEvent(double time, int deme, double alpha) {
		events.add(new ExpPopulationEvent(Math.round(time * 4 * N0), deme - 1, alpha / (4 * N0)));
		timeInvarient =false;
	}

	@CLNames(names ={ "-g" })
	@CLUsage("deme alpha")
	@CLDescription("Set the exponetail growth rate in forward time to alpha from time 0. N/N_i = exp{-alpha*t}")
	public void addExpEvent(int deme, double alpha) {
		addExpEvent(0, deme, alpha);
	}

	@CLNames(names ={ "-oCL", "-oClassLoader" })
	@CLUsage("className [constructorArgs ...]")
	@CLDescription("Loads a stats collector class via the class loader, ie class must be in the class path. Arguments parsed to the contructor are all strings")
	public void addGenericStatsCollector(String classString, String[] args) {
		try {
			Class c =Class.forName(classString);
			Class[] parms =new Class[0];
			if (args != null)
				parms =new Class[args.length];
			Arrays.fill(parms, String.class);
			Constructor creator =c.getConstructor(parms);
			StringStatsCollector sc =(StringStatsCollector) creator.newInstance(args);
			stringStatsCollectors.add(sc);
		} catch (Exception e) {
			// e.printStackTrace();
			throw new RuntimeException("Error parsing and/or creating stats collector ", e);
		}
	}

	@CLNames(names ={ "-m" })
	@CLUsage("demeI demeJ mrate")
	@CLDescription("Set migration rate between deme I & J")
	public void addMigrationChangeEvent(int deme1, int deme2, double rate) {
		if (!flagI) {
			throw new RuntimeException("setting deme parameters when no deme sampling has been specified (ie -I option)");
		}
		addTimeMigrationChangeEvent(0, deme1, deme2, rate);
	}

	@CLNames(names ={ "-ma" })
	@CLUsage("m11 m12 ... m1npop ... m21 ...")
	@CLDescription("Set the migration matrix. Note that diagonal entries are ignored, but are required.")
	public void addMigrationMatrix(String[] mat) {
		if (!flagI) {
			throw new RuntimeException("setting deme parameters when no deme sampling has been specified (ie -I option)");
		}
		addTimeMigrationMatrix(0, sampleConfig.getDemeCount(), mat);
	}

	

	@CLNames(names ={ "-Sc" })
	@CLUsage("t deme [SAA SaA Saa] | [SA]")
	@CLDescription("Set selection parameters for a deme at a given time. Note that using a deme of -1 will set the selection parameters across all demes")
	public void addSelectionChange(double time, int deme, double[] coeffecients) {
		double n2 =2 * N0;
		double saa=0;
		double saA=coeffecients[0];
		double sAA=2*coeffecients[0];
		//System.out.println("double:"+Arrays.toString(coeffecients));
		if(coeffecients.length>1){
			sAA=coeffecients[0];
			saA=coeffecients[1];
			saa=coeffecients[2];
		}
		SelectionStrengthModel ssm =new SelectionStrengthModel.Simple(saa / n2, saA / n2, sAA / n2);
		long g=Math.round(time * 4 * N0);
		
			
		events.add(new SelectionStrengthEvent(g, deme - 1, ssm));
		if(time!=0)
			timeInvarient=false;
	}

	@CLNames(names ={ "-SI" })
	@CLUsage("time npop freq1 freq2 ...")
	@CLDescription("The start time of selection, together with the \"inital\" condtions. Initial conditions are the frequencies of the benifical alleles in the current demes. Note you need to specifiy the number of demes at the time.")
	public void addSelectionEndEvent(double time, int npop, double[] freq) {
		if(freq==null){
			throw new RuntimeException("missing and argument to -SI");
		}
		if (freq.length != npop) {
			throw new RuntimeException("number of frequency arguments must match npop");
		}
		FrequencyState state =new FrequencyState(npop, npop);
		for (int i =0; i < npop; i++) {
			state.setFrequency(i, 1, freq[i]);
			state.setFrequency(i, 0, 1 - state.getFrequency(i, 1));
		}
		events.add(new SelectionEndTimeEvent(Math.round(time * 4 * N0), state,false));
		selection =true;
		// System.out.println(events.get(events.size()-1));
		restartCondition=null;//clear restart condiction unless specifically set.
	}
	
	@CLNames(names={"-SForceKeep","-SFC"},rank=1)
	@CLDescription("Force a restart condition, for use with the -SI option. Will restart the simualtion when ever the frequency drops to zero, note inital conditions must not have zero frequency or we can hang. Also not this is not the same as conditioning on the allele present at sampling time.")
	public void setRestartCondition(){
		restartCondition=new RestartCondition.Default();
	}
	
	@CLNames(names ={ "-SF" },rank=1)
	@CLUsage("")
	public void addSelectionFixation(double time) {
		addSelectionFixation(time, 0, 0);
	}

	@CLNames(names ={ "-SF" },rank=1)
	@CLUsage("")
	public void addSelectionFixation(double time, double freq) {
		addSelectionFixation(time, 0, freq);
	}

	@CLNames(names ={ "-SF" },rank=1)
	@CLUsage("time [deme] [freq]")
	@CLDescription("Sets the time of fixation or the time of a frequency of derived allele from present. The model must me time independant for this to work. There is no checks to see if a model can produce the condtion.")
	public void addSelectionFixation(double time, int deme, double freq) {
		// System.out.println("SelectionFixationCondtions:"+time+"\t"+deme+"\t"+freq+"\t"+events.size());
		if (!timeInvarient) {
			throw new RuntimeException("Cannot condition on fixation times when model is not time invaraient (constant in time). Please use -SI instead.");
		}
		FrequencyCondition fc =new BasicFrequencyCondition(Math.round(time * 4 * N0), deme - 1, freq);
		if (deme == 0 && freq == 0)
			fc =new BasicFrequencyCondition(Math.round(time * 4 * N0), -1, 1);
		stoppingCondition =fc;
		events.add(new SelectionTimeConditionEvent(Math.round(time * 4 * N0), fc));
		selection =true;
	}

	@CLNames(names ={ "-eN" })
	@CLUsage("time scaledSize")
	@CLDescription("Set the population size of all demes at time t")
	public void addSizeChangeEvent(double time, double size) {
		events.add(new PopulationSizeEvent(Math.round(time * 4 * N0), new ConstantPopulation(size * N0)));
		if(time!=0)	
		timeInvarient =false;
	}

	@CLNames(names ={ "-en" })
	@CLUsage("t deme scaledSize")
	@CLDescription("set the population size of a deme to N*scaledSize at time t(in 4N generations). Change takes effect pastward. Note that this is always relative to the global N")
	public void addSizeChangeEvent(double time, int deme, double size) {
		if (!flagI) {
			throw new RuntimeException("setting deme parameters when no deme sampling has been specified (ie -I option)");
		}
		PopulationSizeEvent pse=new PopulationSizeEvent(Math.round(time * N0 * 4), deme - 1, new ConstantPopulation(size * N0));
		//System.out.println(pse);
		events.add(pse);
		if(time!=0)
		timeInvarient =false;
	}

	@CLNames(names ={ "-n" })
	@CLUsage("deme scaledSize")
	@CLDescription("set the population size of a deme to N*scaledSize. Note that this is always relative to the global N")
	public void addSizeChangeEvent(int deme, double size) {
		addSizeChangeEvent(0, deme, size);
	}

	@CLNames(names ={ "-oTPi", "-oTW" }, rank =1)
	@CLUsage("Deprecated. Use -stat options instead")
	public void addThetaWPiStatsCollector() {
		addThetaWPiStatsCollector(1, 1, null);
	}

	@CLNames(names ={ "-oTPi", "-oTW" }, rank =1)
	@CLUsage("Deprecated. Use -stat options instead")
	public void addThetaWPiStatsCollector(double wSize, double sSize) {
		addThetaWPiStatsCollector(wSize, sSize, null);
	}

	@CLNames(names ={ "-oTPi", "-oTW" }, rank =1)
	@CLUsage("[winSize stepSize] [onlySummary]")
	@CLDescription("Collected Windowed Wattersons Theta, Pi and TjD estimates. Deprecated. Use -stat options instead")
	public void addThetaWPiStatsCollector(double wSize, double sSize, String onlyS) {
		boolean onlySummary =false;
		if (onlyS != null) {
			onlySummary =true;
		}
		// note that any sample config option must be invoked first.
		stringStatsCollectors.add(new ThetaEstimators(sampleConfig, wSize, sSize, onlySummary));
	}

	@CLNames(names ={ "-oTPi", "-oTW" }, rank =1)
	@CLUsage("Deprecated. Use -stat options instead")
	public void addThetaWPiStatsCollector(String onlyS) {
		addThetaWPiStatsCollector(1, 1, onlyS);
	}

	@CLNames(names ={ "-em" })
	@CLUsage("t demeI demeJ mrate")
	@CLDescription("Set the migration rate between deme I & J at time t")
	public void addTimeMigrationChangeEvent(double time, int deme1, int deme2, double rate) {
		events.add(new MigrationChangeEvent(Math.round(time * 4 * N0), deme1 - 1, deme2 - 1, rate / (4 * N0)));
		if(time!=0)
			timeInvarient =false;
	}

	@CLNames(names ={ "-eM" })
	@CLUsage("t rate")
	@CLDescription("Set all entries in the migration matrix too rate at time t")
	public void addTimeMigrationChangeEvent(double time, double rate) {
		events.add(new MigrationChangeEvent((long) Math.round(time * 4 * N0), rate / (4 * N0)));
		timeInvarient =false;
	}

	@CLNames(names ={ "-ema" })
	@CLUsage("time npop m11 m12 ...")
	@CLDescription("set the migration matrix at a given time. Effects propergate pastward. Note that the correct number of demes for the time must be specified")
	public void addTimeMigrationMatrix(double time, int npop, String[] mat) {
		if (!flagI) {
			throw new RuntimeException("setting deme parameters when no deme sampling has been specified (ie -I option)");
		}
		if (npop * npop != mat.length) {
			throw new RuntimeException("not enough arguments for migration matrix. Remeber that diagonal entrys are required, but ignored");
		}
		long t =Math.round(time * 4 * N0);
		double[][] rates =new double[npop][npop];
		int index =0;
		for (int i =0; i < npop; i++) {
			for (int j =0; j < npop; j++) {
				String s =mat[index++];
				if (i == j)
					continue;
				if(s.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$"))//double regex...matchs floats and doubles
					rates[i][j] =Double.parseDouble(s) / (4 * N0);
			}
		}
		events.add(new MigrationMatrixEvent(t, rates));
		if(time!=0)
		timeInvarient =false;
	}

	public double getAllelePostion() {
		return allelePostion;
	}

	public double getBackwardMutationRate() {
		return backwardMutationRate / (4 * N0);
	}

	public double getDefaultMigration() {
		return defaultMigration / (4 * N0);
	}

	public int getDemeCount() {
		return sampleConfig.getDemeCount();
	}

	public List<ModelEvent> getEvents() {
		return events;
	}

	public double getForwardMutationRate() {
		return forwardMutationRate / (4 * N0);
	}

	public double getN() {
		return N0;
	}

	public double[] getNeutralLoci() {
		return nLoci;
	}

	public long getRecombinationCutSites() {
		return recombinationCutSites;
	}
	
	public boolean getHasOutgroup()
	{
		return hasOutgroup;
	}
	
	public boolean getIsPhased()
	{
		return isPhased;
	}
	
	

	public double getRecombinationRate() {
		return recombinationRate / (4 * N0);
	}

	public int getRepeats() {
		return repeats;
	}

	public double getSaa() {
		return saa / (2 * N0);
	}

	public double getSaA() {
		return saA / (2 * N0);
	}

	public double getSAA() {
		return sAA / (2 * N0);
	}

	public int getMaxSamples() {
		return sampleConfig.getMaxSamples();
	}

	public long getSeed() {
		return seed;
	}

	public double getSegSiteCount() {
		return segSiteCount;
	}

	public SelectionSimulator getSelectionSimulator() {
		return selectionSimulator;
	}

	public List<StringStatsCollector> getStringStatsCollectors() {
		return stringStatsCollectors;
	}

	public List<StatsCollector> getStatsCollectors() {
		return statsCollectors;
	}

	public FrequencyCondition getStoppingCondition() {
		return stoppingCondition;
	}

	public double getTheta() {
		return theta / (4 * N0);
	}

	public int getThreadCount() {

		return threadCount;
	}

	public boolean isCondtionalMutation() {
		return isCondtionalMutation;
	}

	public boolean isFrequencyTrace() {
		return frequencyTrace;
	}

	public boolean isHelp() {
		return help;
	}

	public boolean isMarkSelectedMutatations() {
		return markSelectedMutatations;
	}

	public boolean isNoCoalescent() {
		return noCoalescent;
	}

	public boolean isNoSequence() {
		return noSequence;
	}

	public boolean isOriginCount() {
		return originCount;
	}

	public boolean isSeedSet() {
		return isSeedSet;
	}

	public boolean isTimeOption() {
		return timeOption;
	}

	public boolean isTrackTrees() {
		return trackTrees;
	}

	public void reset() {
		events.clear();
		stringStatsCollectors.clear();
		flagI=false;

	}
	
//	@CLNames(names ={})
//	@CLDescription("if flag is specified then it is assumed that there is no outgroup. The most frequent mutation is 0")
//	public void setOutgroup()
//	{
//		this.hasOutgroup = false;
//	}
	
	@CLNames(names ={ "-oUnPhased", "-oUnphased"})
	@CLDescription("if flag is specified then it is assumed that sequences are unphased in pairs")
	public void setIsPhased()
	{
		this.isPhased = false;
	}
	
	@CLNames(names ={ "-Snu" })
	@CLDescription("The mutation rate from derived type to wild type in units of 4*N*nu")
	@CLUsage("4Nnu")
	public void setBackwardSelectionMu(double mu) {
		this.backwardMutationRate =mu;
		if(mu>0){
			stoppingCondition=null;//clear the stopiing condiction for this case
		}
	}

	@CLNames(names ={ "-Smu" })
	@CLDescription("The mutation rate from wild type to derived type in units of 4*N*mu")
	@CLUsage("4Nmu")
	public void setForwardSelectionMu(double mu) {
		this.forwardMutationRate =mu;
	}

	@CLNames(names ={ "-oTrace" })
	@CLUsage("")
	@CLDescription("output forward simulation frequencys")
	public void setFrequencyTraceTrue() {
		this.frequencyTrace =true;
	}

	@CLNames(names ={ "-h", "--h", "-H", "--H", "help", "-help", "?", "--help", "-?", "--?" }, rank =-100)
	@CLUsage("")
	@CLDescription("Display this help")
	public void setHelpTrue(String[] devnull) {
		help =true;
		throw new SilentException();
	}
	
	@CLNames(names ={ "-version" }, rank =-100)
	@CLUsage("")
	@CLDescription("Display version details")
	public void setVersionTrue(String[] devnull) {
		String ver = MSLike.class.getPackage().getImplementationVersion();
		System.out.println("msms version "+ver);
		throw new SilentException();
	}

	@CLNames(names ={ "-l", "-loci" })
	@CLDescription("Sets the netrual loci. This must be a even number  ordered real values, where each pair denotes a neutral locus interval. Note that all parameters assume a unit interval")
	@CLUsage("intervalStart1 intervalEnd1 intervalStart2 intervalEnd2 ...")
	public void setLoci(double[] loci) {
		if ((loci.length & 1) == 1) {
			throw new RuntimeException("The -l option has a uneven number of arguments. All intervals must be closed");
		}
		nLoci =loci;
	}

	@CLNames(names ={ "-Smark" })
	@CLUsage("")
	@CLDescription("Marks the derived type as a normal mutation. Note that this breaks the implied contract of segregating sites. This is a \"site\" can now all be a derived type. Multiple origins are also noted with numbers and letters above 1")
	public void setMarkSelectedMutationsTrue() {
		this.markSelectedMutatations =true;
	}

	@CLNames(names ={ "-N" }, rank =-10)
	@CLDescription("Sets the population size when using selection.")
	public void setN0(double n0) {
		N0 =n0;
	}

	@CLNames(names ={ "-NC" })
	@CLDescription("turn off the coalescent pass. Only works when selection is also used.")
	public void setNoCoalescentTrue() {
		noCoalescent =true;
	}

	@CLNames(names ={ "-oSeqOff" })
	@CLUsage("")
	@CLDescription("Turns off default sequence output. Can improve performance")
	public void setNoSequenceTrue() {
		this.noSequence =true;
	}

	@CLNames(names ={ "-oOC" })
	@CLUsage("")
	@CLDescription("Show the number of origins. >1 means a soft sweep")
	public void setOriginCountTrue() {
		this.originCount =true;
	}

	@CLNames(names ={ "-r" })
	@CLUsage("rho [cutsites]")
	@CLDescription("Sets the scaled recombination rate 2*N*rho and also the optional second argument for a \"finite\" cut sites model")
	public void setRecombinationRate(double recombinationRate) {
		this.recombinationRate =recombinationRate;
	}

	@CLNames(names ={ "-r" })
	@CLUsage("")
	// surpress usage printout.
	public void setRecombinationRate(double r, int cutSites) {
		setRecombinationRate(r);
		this.recombinationCutSites =cutSites;
	}

	@CLNames(names ={ "-SredQueen", "-Srq" })
	@CLUsage("Sh Sp mu NOTWORKING ATM")
	@CLDescription("A red queens forward simulator. Must have only 1 deme and use the SI option. Inital frequences are ignored.")
	public void setRedQueenSelection(double sh, double sa, double mu) {
		selectionSimulator =new RedQueenSelection(sh, sa, mu);
	}

	@CLNames(names ={ "-Strace", "-STrace" })
	@CLUsage("filename")
	@CLDescription("Give a filename for a list of frequency traces. '-' specifies standard input. The format is the same as produced for -oTrace. However an entry per generation is not required and frequencys are interpolated.")
	public void setTraceInputSelection(String filename) {
		BufferedReader reader =null;
		if (filename.equals("-")) {
			reader =new BufferedReader(new InputStreamReader(System.in));
		} else {
			try {
				reader =new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Can't find file for Frequency trace", e);
			}
		}

		selectionSimulator =new ReaderSelection(reader);
		selection =true;
	}

	@CLNames(names={"-SseedDeme","-Ssd"})
	@CLDescription("Sets the deme the first mutation starts in with the -SF option. Otherwise it has no effect")
	public void setSeedDeme(int seedDeme) {
		this.seedDeme = seedDeme-1;
	}
	
	@CLNames(names ={ "-SAA" })
	@CLDescription("The selection strength of the Homozygote derived type, alpha=2*N*s")
	public void setsAA(double sAA) {
		this.sAA =sAA;
	}

	@CLNames(names ={ "-SA" })
	@CLDescription("The selection strenght for haploids. Same as setting sAA to 2x this value, and SaA to this value.")
	public void setsA(double sA) {
		this.sAA =sA * 2;
		this.saA =sA;
	}

	@CLNames(names ={ "-Saa" })
	@CLDescription("The selection strength of the Homozygote wild type type, alpha=2*N*s")
	public void setSaa(double saa) {
		this.saa =saa;
	}

	@CLNames(names ={ "-SaA", "-SAa" })
	@CLDescription("The selection strength of the Hetrozygote, alpha=2*N*s")
	public void setSaA(double saA) {
		this.saA =saA;
	}

	/**
	 * we cheat a little here. if someone does not specify a default migration, the last sample is
	 * put into defaultM
	 * 
	 * @param demeCount
	 * @param samples
	 * @param defaultM
	 */
	@CLNames(names ={ "-I" }, rank =-2)
	@CLUsage("noDemes samplesDeme1 samplesDeme2 ... [defaultMigration]")
	@CLDescription("Sets the number of demes and the sample configuration. The sum of samples must match the total samples. The number \"sampleDeme\" arguments must equal the number of demes.")
	public void setSampleConfigs(int demeCount, int[] samples, double defaultM) {
		if (!((demeCount == samples.length) || (demeCount == (samples.length + 1)))) {
			throw new RuntimeException("-I option error: Deme count and sample count don't match:" + samples.length);
		}
		if (demeCount == (samples.length + 1)) {
			if (defaultM - ((int) defaultM) != 0) {
				throw new RuntimeException("-I option error: Deme count and sample count don't match");
			}
			int[] nsamples =new int[samples.length + 1];
			System.arraycopy(samples, 0, nsamples, 0, samples.length);
			nsamples[samples.length] =(int) defaultM;
			samples =nsamples;
			defaultM =0;
		}
		if (Util.sum(samples) != sampleConfig.getMaxSamples()) {
			throw new RuntimeException("-I option error: deme samples don't sum to total samples");
		}
		
		sampleConfig.setDemeSamples(samples);
		//System.out.println("Samples:"+Arrays.toString(samples)+"\n\t"+sampleConfig.getAllNewSampleEvents());
		defaultMigration =defaultM;
		flagI=true;
	}
	
	@CLNames(names={"-IT"},rank=-2)
	@CLUsage("no.TimePoints no.Demes t1 sampleCount@t1&d1 sampleCount@t1&d2 ...  t2 sampleCount@t2&d1 ... [defaultMigration]")
	@CLDescription("Time seperated samples, or serial samples. Typically used for mesurable evolving populations. Time points must be ordered pastward")
	public void setSampleTimeConfig(int timePoints,int demeCount,double[] values){
		int totalEntries=timePoints*demeCount+timePoints;
		if(values.length!=totalEntries && values.length!=totalEntries+1){
			throw new RuntimeException("Incorrect number of parameters for the -IT option.");
		}
		double defaultMRate=0;
		if(values.length==totalEntries+1){
			defaultMRate=values[values.length-1];
		}
		long[] times=new long[timePoints];
		int[][] samples=new int[timePoints][demeCount];
		int scount=0;
		for(int t=0;t<timePoints;t++){
			times[t]=(long)(values[t*(demeCount+1)]*4*N0);
			if(times[t]<0 || (t>0 && times[t-1]>=times[t])){
				throw new RuntimeException("Negative times or later time not further into the past in the -IT option. time points are orderd.");
			}
			for(int d=0;d<demeCount;d++){
				samples[t][d]=(int)values[t*(demeCount+1)+d+1];
				scount+=samples[t][d];
			}
		}
		if(scount!=sampleConfig.getMaxSamples()){
			throw new RuntimeException("Samples don't add up with -IT option. The total number of samples must match!");
		}
		if(times[0]!=0){
			throw new RuntimeException("First time point in -IT option must be zero");
		}
		sampleConfig.setSamples(timePoints, demeCount, samples, times);
		this.defaultMigration=defaultMRate;
		flagI=true;
		
	}

	@CLNames(names ={ "-ms", "" }, rank =-3, required =true)
	@CLDescription("Sets sample size and replicates.")
	@CLUsage(" nsam replicates")
	public void setSamplesRepeats(int samples, int reps) {
		sampleConfig=new SampleConfiguration(samples);
		this.repeats =reps;
	}

	@CLNames(names ={ "-seed" })
	@CLDescription("Set the random seed. Can be decimal or hex (eg 0xC0FFEBABE)")
	@CLUsage("SEED")
	public void setSeed(String seedString) {
		if (seedString.startsWith("0x")) {
			seed =(new BigInteger(seedString.substring(2),16)).longValue();//hack because some things in java are stupid
		} else {
			this.seed =Long.parseLong(seedString);
		}
		isSeedSet =true;
	}

	@CLNames(names ={ "-Sp" })
	@CLDescription("The position of the selected allele")
	@CLUsage("position")
	public void setSelectedAllelePosition(double p) {
		this.allelePostion =p;
	}

	@CLNames(names ={ "-Ssin" })
	@CLUsage("Amplitude period phase shift--Currently not working")
	@CLDescription("Determinitic frequency --a sine wave. Note that all units are *not* scaled with 4N, rather they are in units of generations.")
	public void setSinSelectionSimulator(double amp, double period, double phase, double shift) {
		selectionSimulator =new SinwaveSelection(amp, period, phase, shift);
	}

	@CLNames(names ={ "-t" })
	@CLDescription("Set the scaled mutation rate 4*N*mu")
	public void setTheta(double theta) {
		this.theta =theta;
	}

	@CLNames(names ={ "-threads", "-cores", "-thread" })
	@CLUsage("noThreads")
	@CLDescription("Run on many threads. Useful when on multicore machines. Note however that if the simulations are fast, then this could make its slower! This will produce identical sets of simulations for a given seed. However the order can vary with multiple threads.")
	public void setThreadCount(int threadCount) {
		this.threadCount =threadCount;
	}

	@CLNames(names ={ "-L" })
	@CLUsage("")
	@CLDescription("output tree height and other statistics")
	public void setTimeOptionTrue() {
		this.timeOption =true;
	}

	@CLNames(names ={ "-s" })
	@CLDescription("Condtions on the number of segregating sites. This option will over ride any -t option")
	@CLUsage("segsites")
	public void setTotalSegregatingSites(int n) {
		this.segSiteCount =n;
		isCondtionalMutation =true;
	}

	@CLNames(names ={ "-ws" })
	@CLDescription("Weighted mutations. All edges get a weighted mutations. This reduces shot noise. Most statistics are not weight aware. Not a general user option")
	@CLUsage("segsites")
	public void setWeightedSegregatingSites(double n) {
		this.segSiteCount =n;
		isCondtionalMutation =true;
		weightedMutations=true;
	}
	
	@CLNames(names ={ "-T" })
	@CLUsage("")
	@CLDescription("Output gene trees")
	public void setTrackTreesTrue() {
		trackTrees =true;
	}

	public static CmdLineParser<CommandLineMarshal> getCacheParser(){
		return parserCache.get();
	}
	
	public static void main(String[] args) {
		CommandLineMarshal clm =new CommandLineMarshal();
		try {
			CmdLineParser<CommandLineMarshal> parser =getCacheParser();
			System.out.println("msms " + parser.longUsage());
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	@CLNames(names ={ "-stat" })
	@CLDescription("add a generic stat to the list of stats... experimental--No help yet. ")
	@CLUsage("-stat name [deme ...] -StatType[help for list] [extra options]")
	public void setStat(String[] args) {
		FixedBitSet mask =new FixedBitSet(sampleConfig.getMaxSamples());
		FixedBitSet[] masks=sampleConfig.getMasks();
		//System.out.println("DemeMasks:"+Arrays.toString(demeMasks));
		int count =0;
		while (count < args.length && args[count] != null && args[count].matches("\\d*")) {
			int demeId =Integer.parseInt(args[count]) - 1;
			mask.or(masks[demeId]);
			count++;
		}

		String statName =args[count++];
		statName=statName.substring(1);
		int countnext =count;
		FixedBitSet nextMask =new FixedBitSet(sampleConfig.getMaxSamples());
		while (countnext < args.length && args[countnext] != null && args[countnext].matches("\\d*")) {
			int demeId =Integer.parseInt(args[countnext]) - 1;
			nextMask.or(masks[demeId]);
			countnext++;
		}
		if (count == 0) {
			mask.invert();
		}

		String[] statArgs =new String[args.length - countnext];
		Class[] types=new Class[statArgs.length];
		if (statArgs.length > 0){
			System.arraycopy(args, countnext, statArgs, 0, statArgs.length);
			Arrays.fill(types, String.class);
		}

		// get Mr Stat Class
		Class<? extends StatsCollector> cls =StatsCollector.ClassFactory.getNamedStatsCollector(statName);
		if (cls == null)
			throw new RuntimeException("Could not find " + statName + " statClass");
		StatsCollector collector =null;
		try {
			if (statArgs.length==0) {
				collector =cls.newInstance();
			} else {
				Constructor<? extends StatsCollector> con=cls.getConstructor(types);
				collector=con.newInstance(statArgs);
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not find "+statName+". Perhaps you have a typing error, or the class path is wrong.",e);
		}
		// statsCollectors.add(fst);
		assert collector!=null;
		collector.setLeafMask(mask);
		if(countnext!=count){
			collector.setSecondLeafMask(nextMask);
		}
		statsCollectors.add(collector);
	}

	@CLNames(names={"-oRecombEvents","-oRE","-ore"})
	@CLDescription("")
	public void trackRecombinationEvents(){
		EventTracker et=new EventTracker();
		//stringStatsCollectors.add(et);
		eventTracker=et;
	}
	
	@CLNames(names ={ "-batchMode", "-b" })
	@CLDescription("Suppress text output. Note this suppress all default text output generally. So without -stat options you would get nothing.")
	public void setBatch() {
		batchMode =true;
	}

	public EventTracker getEventTracker() {
		return eventTracker;
	}
	
	public boolean isBatchMode() {
		return batchMode;
	}

	public boolean isSelection() {
		return selection;
	}
	
	public boolean isPrintTableStats() {
		return printTableStats;
	}
	
	public SampleConfiguration getSampleConfig() {
		return sampleConfig;
	}
	
	@CLNames(names={"-oFold", "-oNoOutgroup", "-oNooutgroup"})
	@CLDescription("Folds the mutations before any summarys are carried on the data. Does not affect selected alleles. Same as no outgroup")
	public void setFoldMutationsTrue(){
		foldMutations=true;
	}
	
	public boolean getFoldMutations(){
		return foldMutations;
	}
	
	public int getSeedDeme() {
		return seedDeme;
	}
	
	@CLNames(names={"-oNoStats","-oNS"})
	public void setPrintTableStatsFalse() {
		this.printTableStats =false;
	}
	
	@CLNames(names={"-oFP","-oFormat","-oformat"})
	@CLUsage("Change the decimal printing format used. The format string is as defined in java api docs for DecimalFormat")
	public void setFormatString(String format){
		try {
			DecimalFormat nf=new DecimalFormat(format,Util.dfs);
			Util.defaultFormat=nf;
		} catch (Exception e) {
			throw new RuntimeException("Invalid format string with the -oFP option",e);
		}
	}
	
	public RestartCondition getRestartCondition() {
		return restartCondition;
	}
	
	@CLNames(names="-hack")
	public void setHackParams(String[] p){
		HACK_PARAMS=p;
	}
	
	public double getMaxRecombinationRate() {
		return maxRecombinationRate;
	}
	
	
	@CLNames(names={"-approxr"})
	@CLUsage("Approximate Recombination rate. Should be faster. Made almost no difference at all.")
	public void setMaxRecombinationRate(double maxRecombinationRate) {
		this.maxRecombinationRate = maxRecombinationRate/(2*getN());
	}

	
	public boolean isWeightedMutations() {
		return weightedMutations;
	}
	
}
