package at.mabs.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.*;

import com.esotericsoftware.yamlbeans.YamlWriter;

import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CLUsage;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.cmdline.Example;
import at.mabs.config.CommandLineMarshal;
import at.mabs.model.SampleConfiguration;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.stats.MSStats;
import at.mabs.stats.StatsCollector;
import at.mabs.util.Bag;
import at.mabs.util.Util;
import at.mabs.util.random.Random64;


public class ConverterDadiMsms {
	private File file=null;
	private Boolean foldUnfold=null;
//	private Boolean region = null;
	private int numSNPs =0;
	private int numReps =0;
	private String[] lineElements = null;
	private String[] lineElementsFalse = null;
	private ArrayList<String[]> ArrayLines = new ArrayList<String[]>();
	private int countLineComment =0;
	private int numLines = 0;
	private int posA1 = 0;
	private int posA2 = 0;
	private String line = null;
    private SampleConfiguration sampleConfig;
	private List<StatsCollector> statsCollectors =new LinkedList<StatsCollector>();
	private int[] samples = null;
	private int nA1=0;
	private int nA2=0;
	private int sum = 0;
	private int[][] arrNA1=null;
	private int[][] arrNA2=null;
	private String out=null;
	private int numCol=0;
	private int demeCount = 0;

    
	public ConverterDadiMsms() {}

	@CLNames(names ={ "-SNPfile" }, rank=-6)
	@CLUsage("file - full path")
	@CLDescription("SNP file should follow the structure specified in dadi's manual")
    public void setSnpFile(String filename){
        //Get SNP file and scan it
		file = new File(filename);	
        
        try {
	        Scanner scanner = new Scanner(file);
	        
	        while (scanner.hasNextLine()) {
	        	line = scanner.nextLine();
	        	lineElementsFalse = line.split(" ");
	        	//count comment lines
	        	if (lineElementsFalse[0].equals("#")) {
	        		countLineComment = countLineComment + 1;
	       		}
	        	lineElements = line.split("	");
	        	//ArrayLines is an arraylist of all lineElements, String arrays of line elements
	        	ArrayLines.add(lineElements);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    	}
        
        //count lines, print lines
        for(String[] p: ArrayLines) {
			numLines = numLines +1;
		}
        
        //Get header, find Allele 1, 2
		String[] fly = ArrayLines.get(countLineComment);
    	numCol = fly.length;
    	
        for (int i=0; i<fly.length; i++) {
        	if (fly[i].equals("Allele1")) {
        		posA1 = posA1 + i;
        	}
        	if (fly[i].equals("Allele2")) {
        		posA2 = posA2 + i;
        	}
        }
        
        //Find number of demes, maximum number of samples per deme, keep track of number of samples per allele per deme
        demeCount = posA2-posA1-1;
		samples= new int[demeCount];
		int[][] arrNA1=new int[numLines][demeCount];
	    int[][] arrNA2=new int[numLines][demeCount];
		
        for (int i=0; i<demeCount; i++) {
   	    	nA1 = Integer.parseInt(ArrayLines.get(countLineComment+1)[i + posA1 + 1]);
			nA2 = Integer.parseInt(ArrayLines.get(countLineComment+1)[i + posA2 + 1]);
			sum=sum+nA1+nA2;
			samples[i]= nA1+nA2;
			
			arrNA1[countLineComment+1][i] = nA1;
			arrNA2[countLineComment+1][i] = nA2;
		    for (int j=(countLineComment + 1); j<numLines; j++) {
		    	arrNA1[j][i] = Integer.parseInt(ArrayLines.get(j)[i + posA1 + 1]);
		    	arrNA2[j][i] = Integer.parseInt(ArrayLines.get(j)[i + posA2 + 1]);

				if (samples[i] < arrNA1[j][i] + arrNA2[j][i]) {
			   	    samples[i]= (arrNA1[j][i] + arrNA2[j][i]);
				}
		    }
        }
        
	    sampleConfig=new SampleConfiguration(Util.sum(samples));
		sampleConfig.setDemeSamples(samples);
    }
    
    @CLNames(names ={ "-foldUnfold" }, rank=-5)
	@CLUsage("true - false")
    @CLDescription("true: consider outgroup as ancestral - false: no difference between ancestral and derived")
    public void setFoldUnfold(Boolean fold){
    	foldUnfold=fold;
    	
    }

//    	@CLNames(names ={ "-gene" })
//		@CLUsage("")
//    	public void setGene(Boolean gene){
//   		region=gene;
//    	}

    @CLNames(names ={ "-numSNPs" }, rank=-4)
    @CLUsage("int")
    @CLDescription("To apply the -stat package, will build windows of SNP number of SNPs")
    public void setNumSNP(int SNP){
    	numSNPs = SNP;
    	
    }
    
    @CLNames(names ={ "-random" }, rank =-3)
    @CLUsage("int")
    @CLDescription("If you set -random you will average the -stats over Reps windows of SNP SNPs starting from random positions, overlap permitted")
    public void setNumReps(int Reps){
    	numReps = Reps;
    	
    }
    
 /**   
    @CLNames(names ={ "-I" }, rank =-2)
	@CLUsage("noDemes samplesDeme1 samplesDeme2 ... ")
	@CLDescription("Sets the number of demes and the sample configuration. The sum of samples must match the total samples. The number \"sampleDeme\" arguments must equal the number of demes.")
	
    public void setSampleConfigs(int demeCount, int[] samples) {
		if (!((demeCount == samples.length) || (demeCount == (samples.length + 1)))) {
			throw new RuntimeException("-I option error: Deme count and sample count don't match:" + samples.length);
		}
		sampleConfig=new SampleConfiguration(Util.sum(samples));
		sampleConfig.setDemeSamples(samples);
		//System.out.println("Samples:"+Arrays.toString(samples)+"\n\t"+sampleConfig.getAllNewSampleEvents());
		
	}
**/    
    @CLNames(names ={ "-stat" }, rank=-2)
	@CLDescription("add a generic stat to the list of stats... experimental--No help yet")
	@CLUsage("-stat name [deme ...] -StatType[help for list] [extra options]")
	public void setStat(String[] args) {
    	
    	System.out.println(sampleConfig.getDemeCount());
    	
    	//Create fixedbitset mask -1 to pops before stat name, 0 others
		FixedBitSet mask =new FixedBitSet(sampleConfig.getMaxSamples());
		FixedBitSet[] demeMasks=sampleConfig.getMasks();
		//System.out.println("DemeMasks:"+Arrays.toString(demeMasks));
		int count =0;
		while (count < args.length && args[count] != null && args[count].matches("\\d*")) {
			int demeId =Integer.parseInt(args[count]) - 1;
			mask.or(demeMasks[demeId]);
			count++;
		}
		
		//Get name of stat
		String statName =args[count++];
		statName=statName.substring(1);
		
    	//Create fixedbitset nextMask - 1 to pops after stat name, 0 others
		int countnext =count;
		FixedBitSet nextMask =new FixedBitSet(sampleConfig.getMaxSamples());
		while (countnext < args.length && args[countnext] != null && args[countnext].matches("\\d*")) {
			int demeId =Integer.parseInt(args[countnext]) - 1;
			nextMask.or(demeMasks[demeId]);
			countnext++;
		}
		
		//Case for all pops included
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
    
    @CLNames(names ={ "-out" }, rank=-1)
	@CLUsage("Introduce output file")
    public void setOutput(String fileOutname) {
    	out=fileOutname;
    	
    }
    
    private static void saveStats(Writer writer, List<StatsCollector> stats) throws IOException {
		YamlWriter yamlWriter = new YamlWriter(writer);
		yamlWriter.getConfig().setPrivateFields(true);
		yamlWriter.getConfig().setPrivateConstructors(true);
		yamlWriter.write(stats);
		yamlWriter.close();
	}

    
    public void run() {
		try {
	        Scanner scanner = new Scanner(file);
	        
	        while (scanner.hasNextLine()) {
	        	line = scanner.nextLine();
	        	lineElements = line.split("	");
	        	ArrayLines.add(lineElements);
	        	System.out.println("line is: " + Arrays.toString(lineElements));
	        }
	        
	        System.out.println("Number of lines is: " + numLines);
			System.out.println("Number of comment lines is: " + countLineComment);
			System.out.println("Number of samples per population is: " + Arrays.toString(samples));
	        int demeCount = posA2-posA1-1;
	        System.out.println("Position of Allele1 is: " + posA1);
	        System.out.println("Position of Allele2 is: " + posA2);
	        System.out.println("Number of demes is: " + demeCount);
	        
	        /**		String geneList = ArrayLines.get(countLineComment + 1)[posA2+demeCount+1];
	        
			if (region==false) {
	        	System.out.println("Now chose the gene among these: ");
	    		System.out.println(geneList);
	    		
			    for (int j=(countLineComment + 2); j<numLines; j++) {
	    			geneList = ArrayLines.get(j)[posA2+demeCount+1];
	    			
	        		if (geneList.equals(ArrayLines.get(j - 1)[posA2+demeCount+1])) {
	        		} else {
	        			System.out.println(geneList);
	        		}
			    }
	        }
	        **/        
			double pos = 0;	    
		    int posInt=0;
			String[] outGroup = null;
			int[][] arrNA1=new int[numLines][demeCount];
		    int[][] arrNA2=new int[numLines][demeCount];
		    
	    	ArrayList<InfinteMutation> mutations = new ArrayList<InfinteMutation>();
			samples= new int[demeCount];
			
					
	    	for (int i=0; i<demeCount; i++) {
	   	    	nA1 = Integer.parseInt(ArrayLines.get(countLineComment+1)[i + posA1 + 1]);
				nA2 = Integer.parseInt(ArrayLines.get(countLineComment+1)[i + posA2 + 1]);
				samples[i]= nA1+nA2;
				arrNA1[countLineComment+1][i] = nA1;
				arrNA2[countLineComment+1][i] = nA2;
			    for (int j=(countLineComment + 1); j<numLines; j++) {
			    	arrNA1[j][i] = Integer.parseInt(ArrayLines.get(j)[i + posA1 + 1]);
			    	arrNA2[j][i] = Integer.parseInt(ArrayLines.get(j)[i + posA2 + 1]);

					if (samples[i] < arrNA1[j][i] + arrNA2[j][i]) {
				   	    samples[i]= (arrNA1[j][i] + arrNA2[j][i]);
					}
			    }
	    	}
	    	
	    	//Here begins the matter...

	   	    int[] mask = new int[numLines];
	   	    
		    for (int j=(countLineComment + 1); j<numLines; j++) {
		    	int intSet[] = new int[sum];
		    	
		    	 //Just control for missing data damn 'em
		    	
		    	for (int i=0; i<demeCount; i++) {
		    		if (samples[i] > (arrNA1[j][i] + arrNA2[j][i])) {
		    			mask[j] = 0;
		    			System.out.println("The SNP at line: " + (j) + " has missing data in population: " + i + ", eliminated! " + Arrays.toString(ArrayLines.get(j)));
		    			break;
		    		} else {
		    			mask[j] = 1;
		    		}
		    	}
			    outGroup = ArrayLines.get(j)[1].split("");

			    if (mask[j]==1) {
			    	if (foldUnfold==true) {
			    		if ((outGroup[2].equals("-")) || (ArrayLines.get(j)[1].equals("-"))) {
			    			System.out.println("The SNP at line (counting from line 0, comments eliminated!): " + (j-countLineComment) + " has no known outgroup.. eliminated! " + Arrays.toString(ArrayLines.get(j)));
			    			continue;
			    		}
			        	for (int k=0; k<sum; k++) {
				    		if (ArrayLines.get(j)[posA2].equals(outGroup[2])) {
				    			for (int i=0; i<demeCount; i++) {
				    				for (int h=0; h<arrNA1[j][i]; h++) {
						   	    		intSet[k+h] = 1;
						   	    	}
					   	    		k=k+arrNA1[j][i];
					   	    		
						   	    	for (int h=0; h<arrNA2[j][i]; h++) {
						   	    		intSet[k+h] = 0;
						   	    	}
					   	    		k=k+arrNA2[j][i];
				    			}
				    		} else {
				    			if (ArrayLines.get(j)[posA1].equals(outGroup[2])) {
					    			for (int i=0; i<demeCount; i++) {
					    				for (int h=0; h<arrNA2[j][i]; h++) {
							   	    		intSet[k+h] = 1;
							   	    	}
						   	    		k=k+arrNA2[j][i];
						   	    		
							   	    	for (int h=0; h<arrNA1[j][i]; h++) {
							   	    		intSet[k+h] = 0;
							   	    	}
						   	    		k=k+arrNA1[j][i];
					    			}
					    		}
				    		}
				    	}
			    	}
			    	if (foldUnfold==false) {
			    		for (int k=0; k<sum; k++) {
				    		for (int i=0; i<demeCount; i++) {
				    			for (int h=0; h<arrNA1[j][i]; h++) {
					   	    		intSet[k+h] = 1;
					   	    	}
				   	    		k=k+arrNA1[j][i];
				   	    		
				   	    		for (int h=0; h<arrNA2[j][i]; h++) {
						   	   		intSet[k+h] = 0;
						   	   	}
					   	    	k=k+arrNA2[j][i];
				    		}
				    	}
			    	}
			    } else {
			    	continue;
			    }
		    	posInt = Integer.parseInt(ArrayLines.get(j)[numCol-1]);
				pos = Double.valueOf(posInt);
				
			    FixedBitSet leafset = new FixedBitSet(sum-1);    	
		    	for (int k=0; k<sum; k++) {
		    		if (intSet[k] == 1) {
						leafset.set(k);
		    		}
		    	}
			    InfinteMutation im0 = new InfinteMutation(pos, leafset);
			    mutations.add(im0);
	 		}
		    
	    	Double posMax = mutations.get(0).position;
		    for (int i=0; i<mutations.size(); i++) {
		    	if (mutations.get(i).position>posMax) {
			    	posMax = mutations.get(i).position;
		    	}
		    }
		    
		    for (int i = 0; i < mutations.size(); i++) {
		    	InfinteMutation im = mutations.get(i);
				mutations.set(i, new InfinteMutation(im.position / posMax, im.leafSet));
			}
		    System.out.println("posMax: " + posMax);	
		    
	/**   	System.out.println("mask[] is " + Arrays.toString(mask));	**/
		    for (int i=0; i<mutations.size(); i++) {
		    	System.out.println("position is: " + mutations.get(i).position + mutations.get(i).leafSet);
		    }
		    if (numReps!=0) {
				Random rand = new Random64();
				for (int i=0; i<numReps; i++) {
					int start = rand.nextInt(mutations.size());
					List<InfinteMutation> sublist = mutations.subList(start, Math.min(start + numSNPs, mutations.size() - 1));
					SegmentEventRecoder ser = new SegmentEventRecoder(new Bag(sublist), true, false);
		    		for (StatsCollector sc : statsCollectors) {
		    			sc.collectStats(ser);
		    		}
				}
	    		
		    }
		    if (numReps==0) {
		    	for (int j=0; j<(int)(mutations.size()/numSNPs); j++) {
		    		List<InfinteMutation> sublist = mutations.subList((j*numSNPs), ((j*numSNPs)+numSNPs));
		    		SegmentEventRecoder ser = new SegmentEventRecoder(new Bag(sublist), true, false);
		    		for (StatsCollector sc : statsCollectors) {
		    			sc.collectStats(ser);
		    		}
		    	}
	    		System.out.println("Please bare in mind! if total number of SNPs divided by -numSNPs has a rest (say, r), the last (r) SNPs will not be included in -stat calculations");
			}
		    
			Writer writer = new FileWriter(out);
			saveStats(writer, statsCollectors);
	    
		
		
		} catch (Exception e) {
	        e.printStackTrace();
	    	}
	   }
			

    public static void main(String[] args) {
    	CmdLineParser cmp=null;
    	try {
    		ConverterDadiMsms converterDadiMsms= new ConverterDadiMsms();
			cmp =new CmdLineParser(ConverterDadiMsms.class);
			cmp.processArguments(args, converterDadiMsms);
			
			converterDadiMsms.run();
			
    	} catch (Exception e) {
    		e.printStackTrace();
    		System.out.println(cmp.longUsage());
    	}
    }
}


    