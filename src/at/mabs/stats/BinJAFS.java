package at.mabs.stats;

import at.mabs.segment.InfinteMutation;
import java.util.*;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
/**
 * a jafs but we bin things as per D Metzla scheme. Note that you can customize the bining. 
 * @author bob
 *
 */
public class BinJAFS extends StatsCollectorAdapter {
//	private FixedBitSet mask;
//	private FixedBitSet mask2;
	private String name;
	private String binConfig="1,x,1";
	
	private transient int[][] binIndexs;
	private transient int binCount=0;
	private transient int[] acolIndex;
	private transient int[] bcolIndex;
	
	//private transient MeanVarManager mvm=new MeanVarManager(); 
	
	public BinJAFS() {
		name="jafs";
	}
	
	public BinJAFS(String name) {
		this.name=name;
	}
	/**
	 * 
	 * @param name
	 * @param binConfig comma delimited bin config spec. 
	 * example: 1,x,1 means one from first edge 1 from second edge. rest in the middle.
	 * example: 1,1,2,x,1,1,2 etc....
	 */
	public BinJAFS(String name,String binConfig) {
		this.name=name;
		this.binConfig=binConfig;
	}
	
	private void parseBinConfig(){
		List<Integer> firstEdge=new ArrayList<Integer>();
		List<Integer> secondEdge=new ArrayList<Integer>();
		StringTokenizer st=new StringTokenizer(binConfig,",");
		while(st.hasMoreTokens()){
			String token=st.nextToken();
			if(token.equals("x"))
				break;
			firstEdge.add(Integer.parseInt(token));
		}
		while(st.hasMoreTokens()){
			String token=st.nextToken();
			secondEdge.add(Integer.parseInt(token));
		}
		//now for bin count
		binCount=(firstEdge.size()+secondEdge.size()+1);
		binCount*=binCount;
		int asize=mask.countSetBits();
		int bsize=mask2.countSetBits();
		
		int totalSum=0;
		for(int i:firstEdge)
			totalSum+=i;
		for(int i:secondEdge)
			totalSum+=i;
		
		
		acolIndex=new int[firstEdge.size()+1+secondEdge.size()];
		bcolIndex=new int[acolIndex.length];
		int sum=0;
		for(int i=0;i<firstEdge.size();i++){
			sum+=firstEdge.get(i);
			acolIndex[i]=sum;
			bcolIndex[i]=sum;
		}
		int sumb=bsize-totalSum+sum+1;
		sum=asize-totalSum+sum+1;
		acolIndex[firstEdge.size()]=sum;
		bcolIndex[firstEdge.size()]=sum;
		int fep1=firstEdge.size()+1;
		for(int i=0;i<secondEdge.size();i++){
			sum+=secondEdge.get(i);
			sumb+=secondEdge.get(i);
			acolIndex[i+fep1]=sum;
			bcolIndex[i+fep1]=sumb;
		}
		//System.out.println(Arrays.toString(acolIndex)+"\n"+Arrays.toString(bcolIndex));
		//int normBinCount=(asize+1)*(bsize+1);
		binIndexs=new int[asize+1][bsize+1];
		int acol=0;
		for(int a=0;a<binIndexs.length;a++){
			if(acolIndex[acol]<=a )
				acol++;
			int bcol=0;
			for(int b=0;b<binIndexs[0].length;b++){
				if(bcolIndex[bcol]<=b)
					bcol++;
				//System.out.println("BCol:"+bcol+"\t"+b+"\t"+binIndexs[0].length);
				binIndexs[a][b]=acol+bcol*(acolIndex.length);
			}
			//System.out.println("Bindex:"+Arrays.toString(binIndexs[a]));
		}
		
	}
	
	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {
		if(binCount<=0)
			parseBinConfig();
		double[] result=new double[binCount];
		//double delta=1.0;///recorder.getTotalMutationCount();
		for(InfinteMutation m:recorder.getMutationsUnsorted()){
			int a=m.leafSet.countSetBitsMask(mask);
			int b=m.leafSet.countSetBitsMask(mask2);
			result[binIndexs[a][b]]+=m.weight;
		}
		
		return result;
	}
	
//	
//	@Override
//	public void setLeafMask(FixedBitSet mask) {
//		mask=mask;
//		//System.out.println("Mask1:"+mask);
//	}
//
//	@Override
//	public void setSecondLeafMask(FixedBitSet mask) {
//		mask2=mask;
//		//System.out.println("Mask2:"+mask);
//	}

	@Override
	public String[] getStatLabels() {
		if(binCount<=0)
			parseBinConfig();
		String[] names=new String[binCount];
		int count=0;
		int lasta=0;
		
		for(int a:acolIndex){
			int lastb=0;
			for(int b:bcolIndex){
				names[count++]=name+"["+lasta+","+a+"),["+lastb+","+b+")";
				lastb=b;
			}
			lasta=a;
		}
		return names;
	}
}
