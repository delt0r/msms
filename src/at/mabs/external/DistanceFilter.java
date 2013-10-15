package at.mabs.external;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.TreeSet;


import at.mabs.cmdline.CLDescription;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.util.Util;

public class DistanceFilter {
	private ArrayList<Integer> simIndexBuild=new ArrayList<Integer>();
	private int[] simIndex;
	
	private ArrayList<Integer> dataIndexBuild=new ArrayList<Integer>();
	private int[] dataIndex;
	private double[] data;
	
	private int keep=1000;
	
	private String datafileName;//null means first line of the input. 
	
	@CLNames(names={"-simRange","-sr"})
	@CLDescription("A range in the simulation file that is used for distance calulations. Inclusive!")
	public void addSimRange(int start,int end){
		for(int i=start-1;i<=end-1;i++){
			simIndexBuild.add(i);
		}
	}
	
	@CLNames(names={"-simIndex","-si"})
	@CLDescription("A range in the simulation file that is used for distance calulations. Inclusive!")
	public void addSimIndex(int index){
		simIndexBuild.add(index-1);
	}
	
	
	@CLNames(names={"-dataRange","-dr"})
	@CLDescription("A range in the data file that is used for distance calulations. Inclusive!")
	public void addDataRange(int start,int end){
		for(int i=start-1;i<=end-1;i++){
			dataIndexBuild.add(i);
		}
	}
	
	@CLNames(names={"-dataIndex","-di"})
	@CLDescription("A range in the data file that is used for distance calulations. Inclusive!")
	public void addDataIndex(int index){
		dataIndexBuild.add(index-1);
	}
	
	@CLNames(names={"-data","-d","-datafile","-df"})
	public void dataFileName(String fileName){
		datafileName=fileName;
	}

	@CLNames(names={"-keep","-k"})
	public void setKeep(int k){
		keep=k;
	}
	
	public void run(){
		//first get source index
		simIndex=Util.toArrayPrimitiveInteger(simIndexBuild);
		if(dataIndexBuild.isEmpty()){
			dataIndex=simIndex;
			System.err.println("WARNING: no data indices specified. Using source indices instead. This is probably wrong.");
		}else{
			dataIndex=Util.toArrayPrimitiveInteger(dataIndexBuild);
		}
		
		TreeSet<double[]> bestOf=new TreeSet<double[]>(new Comparator<double[]>() {
			@Override
			public int compare(double[] o1, double[] o2) {
				if(o1[o1.length-1]>o2[o2.length-1])
					return 1;
				return -1;
			}
		});
		
		//data file.
		try{
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(datafileName)));
			String dataLine=br.readLine();
			br.close();
			
			ArrayList<String> tokens=new ArrayList<String>();
			StringTokenizer st=new StringTokenizer(dataLine," \t,");
			while(st.hasMoreTokens()){
				tokens.add(st.nextToken());
			}
			
			data=new double[dataIndex.length];
			for(int i=0;i<dataIndex.length;i++){
				data[i]=Double.parseDouble(tokens.get(dataIndex[i]));
			}
			System.err.println("Using data elements:"+Arrays.toString(data));
			//we are now data happy.
			
			
			br=new BufferedReader(new InputStreamReader(System.in));
			String line=br.readLine();
			st=new StringTokenizer(line); 
			
			double[] simLine=new double[st.countTokens()]; 
			int endIndex=simLine.length-1;
			while(line!=null){
				st=new StringTokenizer(line); 
				for(int i=0;i<simLine.length;i++){
					simLine[i]=Double.parseDouble(st.nextToken());
				}
				//now calulate distance..
				double d2=0;
				for(int i=0;i<simIndex.length;i++){
					double d=simLine[simIndex[i]]-data[i];
					d2+=d*d;
				}
				d2=Math.sqrt(d2);
				simLine[endIndex]=d2;//insert into the end;
				if(bestOf.size()<keep || bestOf.last()[endIndex]>d2){
					bestOf.add(simLine);
					simLine=simLine.clone();
					if(bestOf.size()>keep)
						bestOf.pollLast();
				}
				
				
				line=br.readLine();
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		for(double[] r:bestOf){
			for(double d:r){
				System.out.print(d+"\t");
			}
			System.out.println();
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DistanceFilter df = new DistanceFilter();
		CmdLineParser<DistanceFilter> parser = null;
		try {
			parser = new CmdLineParser<DistanceFilter>(DistanceFilter.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println("ARGS:\n" + Arrays.toString(args));
			return;
		}

		try {
			parser.processArguments(args, df);
			df.run();
		} catch (Exception e) {
			System.err.println(parser.longUsage());
			System.out.println("ARGS:\n" + Arrays.toString(args));
			e.printStackTrace();

		}

	}
	
	

}
