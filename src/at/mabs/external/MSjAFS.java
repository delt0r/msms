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
package at.mabs.external;

import java.io.*;
import java.util.Arrays;
import java.util.BitSet;
import java.util.StringTokenizer;

import at.mabs.util.Util;

/**
 * turn ms output into a summary jAFS.  The idea is to run this once
 * or twice and then we have a DB of "correct" results that we can test agaist.
 * 
 * @author bob
 * 
 */
public class MSjAFS {
	public static void main(String[] args) {
		try {
			// first parse the "command line" searching for the -I option.
			LineNumberReader lnr =new LineNumberReader(new InputStreamReader(System.in));
			String firstLine =lnr.readLine();
			
			System.out.println(firstLine);
			
			StringTokenizer tokens=new StringTokenizer(firstLine," \t");
			
			while(tokens.hasMoreElements()){
				String t=tokens.nextToken();
				if(t==null)
					throw new RuntimeException("Its null for crying out loud");
				if(t.equals("-I"))
					break;
			}
			//so we are at -I..The next token should be the number of demes.
			int ndemes=Integer.parseInt(tokens.nextToken());
			
			int[] sampleConfig=new int[ndemes];
			for(int i=0;i<ndemes;i++){
				sampleConfig[i]=Integer.parseInt(tokens.nextToken());
			}
			int totalSampleCount=Util.sum(sampleConfig);
			
			int jafsCount=ndemes*(ndemes-1)/2;
			int[][][] jafs=new int[jafsCount][0][0];
			int count=0;
			for(int i=0;i<ndemes;i++){
				for(int j=i+1;j<ndemes;j++){
					jafs[count++]=new int[sampleConfig[i]+1][sampleConfig[j]+1];
				}
			}
			
			//so we know what to look for now...
			//we assume there is no -T option and just skip explicit lines once we find a //
			String line=lnr.readLine();
			main:
			while(line!=null){
				while(line!=null && !line.equals("//")){
					line=lnr.readLine();
				}
				if(line==null)
					break;
				//now skip segsites and postions.
				String segsites=lnr.readLine();
				if(segsites.equals("segsites: 0")){
					line=lnr.readLine();
					continue;
				}
				lnr.readLine();
				
				
				//now we have real sequences to deal with. 
				int alleleCount=0;
				BitSet[] bitSets=createBitSets(totalSampleCount);
				for(int i=0;i<totalSampleCount;i++){
					line=lnr.readLine();
					BitSet set=bitSets[i];
					//System.out.println("count:"+i+"\t"+line);
					if(line==null)
						break main;
					for(int j=0;j<line.length();j++){
						if(line.charAt(j)=='1'){
							set.set(j);
						}else{
							set.clear(j);
						}
					}
					//System.out.println(line+"\t"+set);
					alleleCount=line.length();
					
				}
				addJointAFS(bitSets, sampleConfig, jafs, alleleCount);
				
			}
			count=0;
			for(int i=0;i<ndemes;i++){
				for(int j=i+1;j<ndemes;j++){
					System.out.println("jAFS "+i+"\t"+j);
					int[][] afs=jafs[count++];
					for(int x=0;x<afs.length;x++){
						for(int y=0;y<afs[x].length;y++){
							System.out.print(afs[x][y]+" ");
						}
						System.out.println();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private static void addJointAFS(BitSet[] bits,int[] sampleConfig,int[][][] counts, int alleleCount){
		for(int allele=0;allele<alleleCount;allele++){
			int count=0;
			int demes=sampleConfig.length;
			int fromI=0;
			int fromJ=0;
			for(int i=0;i<demes-1;i++){
				int icount=alleleCount(bits, fromI, fromI+sampleConfig[i], allele);
				fromI+=sampleConfig[i];
				fromJ=fromI;
				for(int j=i+1;j<demes;j++){
					int jcount=alleleCount(bits,fromJ,fromJ+sampleConfig[j],allele);
					//assert !(icount==0 && jcount==0);
//					if(jcount==0 && icount==0){
//						for(BitSet s:bits){
//							System.out.println(s);
//						}
//						throw new RuntimeException();
//					}
					fromJ+=sampleConfig[j];	
					counts[count++][icount][jcount]++;
				}
			}
			
			
		}
	}
	
	private static int alleleCount(BitSet[] bits,int from,int to,int allele){
		
		int c=0;
		for(int i=from;i<to;i++){
			if(bits[i].get(allele))
				c++;
		}
		//System.out.println(from+"\t"+to+"\t"+allele+"\t"+c);
		return c;
	}
	
	private static BitSet[] createBitSets(int number){
		BitSet[] bits=new BitSet[number];
		for(int i=0;i<bits.length;i++){
			bits[i]=new BitSet();
		}
		return bits;
	}
}
