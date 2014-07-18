package at.mabs.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import cern.colt.Arrays;

import com.esotericsoftware.yamlbeans.YamlWriter;

import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.config.CommandLineMarshal;
import at.mabs.model.SampleConfiguration;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.stats.StatsCollector;
import at.mabs.util.Bag;

/**
 * convert any MS output into something all -stats will work with. Some header info however must be provided explicity.
 * 
 * @author bob
 * 
 */
public class MStoStats {
	private CommandLineMarshal msmsparser;
	private SampleConfiguration sampleConfig;
	private List<StatsCollector> collectors;
	private boolean plainText = false;
	private boolean help = false;

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
	
	public void run(){
		
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
			//read till we have a //
			String line=br.readLine();
			while(line!=null){
				if(!line.startsWith("//")){
					line=br.readLine();
					continue;
				}
				//read segsites: splat. 
				line=br.readLine();
				StringTokenizer st=new StringTokenizer(line);
				st.nextToken();//dump segsites. 
				int segSites=Integer.parseInt(st.nextToken());
				
				double weight=1;
				
				Bag<InfinteMutation> muts=new Bag<InfinteMutation>(segSites);
				line=br.readLine();
				st=new StringTokenizer(line);
				st.nextElement();//dump "positions:"
				
				for(int i=0;i<segSites;i++){
					double pos=Double.parseDouble(st.nextToken());
					muts.add(new InfinteMutation(pos,new FixedBitSet(sampleConfig.getMaxSamples()), weight));
				}
				//now we add the mutations. 
				for(int i=0;i<sampleConfig.getMaxSamples();i++){
					line=br.readLine();
					for(int j=0;j<segSites;j++){
						InfinteMutation im=muts.get(j);
						if(line.charAt(j)=='1'){
							im.leafSet.set(i);
						}
					}
				}
				//since some inputs have invalid seg sites we have to recount the number of sites! 
				int dCount=0;
				for(InfinteMutation im:muts){
					int c=im.leafSet.countSetBits();
					if(c==0 || c==sampleConfig.getMaxSamples()){
						segSites--;
						dCount++;
						//System.out.println(im.leafSet);
					}
				}
				//System.out.println("Delete! "+dCount +"\tleft "+segSites);
				if(msmsparser.isWeightedMutations()){
					weight=msmsparser.getSegSiteCount()/segSites;
				}
				Bag<InfinteMutation> finalMuts=new Bag<InfinteMutation>(segSites);
				for(InfinteMutation im:muts){
					int c=im.leafSet.countSetBits();
					if(c!=0 && c!=sampleConfig.getMaxSamples()){
						finalMuts.add(new InfinteMutation(im.position,im.leafSet, weight));
					}
				}
				
				//create the stat object
				SegmentEventRecoder ser = new SegmentEventRecoder(finalMuts, msmsparser.getFoldMutations(), msmsparser.getFoldMutations());
				//apply
				for (StatsCollector sc : collectors) {
					sc.collectStats(ser);
				}
				
				line=br.readLine();
			}
			
			//now we spit out the results. 
			Writer writer =  new OutputStreamWriter(System.out);
			
			if (plainText) {
				plainTextOut(writer);
				writer.flush();
				writer.close();
			} else {
				saveStats(writer, collectors);
			}
			
		}catch(Exception e){
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

	public static void main(String[] args) {
		MStoStats rse = new MStoStats();
		CmdLineParser<MStoStats> parser = null;
		try {
			parser = new CmdLineParser<MStoStats>(MStoStats.class);
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
