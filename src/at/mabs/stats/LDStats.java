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
package at.mabs.stats;

import java.util.*;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;
import at.mabs.util.cuckoo.*;
/**
 * simple grid based pair wise LD stats. Note that we don't have summarys yet. 
 * @author greg
 * @deprecated
 */
public class LDStats implements StringStatsCollector {
	public static final int D_PRIME=0,D=1,R_SQUARED=2;
	
	private final int type;
	private final double windowSize;
	
	private Map<Bin, Bin> xybins=new CHashMap<Bin, Bin>();
	private Map<Bin, Bin> rowbins=new CHashMap<Bin, Bin>();
	private TreeSet<Integer> used=new TreeSet<Integer>();
	
	
	
	
	public LDStats(int type) {
		this.type = type;
		this.windowSize=.02;
	}
	
	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		List<InfinteMutation> mutations=recorder.getMutationsSorted();
		if(mutations.isEmpty())
			return;
		//builder.append("LD matrix:\n");
		double sampleSize=mutations.get(0).leafSet.size();//FIXME
		for(int i=0;i<mutations.size();i++){
			InfinteMutation mutA=mutations.get(i);
			double fA=mutA.leafSet.countSetBits()/sampleSize;
			int acount=mutA.leafSet.countSetBits();
			if(acount<sampleSize*.05 || acount>sampleSize*.95)
				continue;
			for(int j=i+1;j<mutations.size();j++){
				InfinteMutation mutB=mutations.get(j);
				double fB=mutB.leafSet.countSetBits()/sampleSize;
				acount=mutB.leafSet.countSetBits();
				if(acount<sampleSize*.05 || acount>sampleSize*.95)
					continue;
				double fAB=mutA.leafSet.countSetBitsMask(mutB.leafSet)/sampleSize;
//				double fAb=mutA.leafSet.countSetBitsComplementMask(mutB.leafSet)/sampleSize;
//				double faB=mutB.leafSet.countSetBitsComplementMask(mutA.leafSet)/sampleSize;
//				double fab=1-fAB-fAb-faB;
				double d=fAB-fA*fB;
//				double dp=fAB*fab-fAb*faB;
				//System.out.println(d-dp);
				double v=0;
				switch(type){
				case D:
					v=d;
					break;
				case D_PRIME:
					if(d>0){
						v=d/Math.min(fA*(1-fB), fB*(1-fA));
					}else{
						v=d/Math.max(-fA*fB, -(1-fA)*(1-fB));
					}
					break;
				case R_SQUARED:
					v=(d*d)/(fA*(1-fA)*fB*(1-fB));
					break;
				}
				addStat(mutA.position, mutB.position, v);
				//builder.append(Util.defaultFormat.format(v)).append(' ');
				
				
			}
			//builder.deleteCharAt(builder.length()-1);
			//builder.append('\n');
		}
		//builder.append('\n');
	}
	
	private void addStat(double px,double py,double ld){
		int x=(int)(px/windowSize);
		int y=(int)(py/windowSize);
		Bin key=new Bin(x,y);
		//System.out.println(x+"\t"+y+"\t"+bins.size());
		Bin bin=this.xybins.get(key);
		if(bin==null){
			bin=key;
			xybins.put(bin, bin);
		}
		bin.addStat(ld);
		
		used.add(x);
		used.add(y);
		
		key=new Bin(x,x);
		bin=this.rowbins.get(key);
		if(bin==null){
			bin=key;
			rowbins.put(bin, bin);
		}	
		bin.addStat(ld);
		
		key=new Bin(y,y);
		bin=this.rowbins.get(key);
		if(bin==null){
			bin=key;
			rowbins.put(bin, bin);
		}
		bin.addStat(ld);
	}
	
	@Override
	public void summary(StringBuilder builder) {
		builder.append("LD mat:\n");
		
		int[] aused=new int[used.size()];
		int counter=0;
		//first the x axis:
		builder.append("postions: ");
		for(int x:used){
			double p=x*windowSize;
			builder.append(Util.defaultFormat.format(p)).append(' ');
			aused[counter++]=x;
		}
		builder.append('\n');
		builder.append("row average:\n");
		for(int i=0;i<aused.length;i++){
			Bin key=new Bin(aused[i],aused[i]);
			Bin bin=rowbins.get(key);
			if(bin==null){
				builder.append("NA ");
			}else{
				builder.append(Util.defaultFormat.format(bin.getAverage())).append(' ');
			}
		}
		builder.append("\n\n");
		//now the y part with the data. 
		for(int r=0;r<aused.length;r++){
			//builder.append(aused[r]).append(' ');
			for(int c=0;c<aused.length;c++){
				int x=aused[r];
				int y=aused[c];
				Bin key=new Bin(x,y);
				Bin bin=xybins.get(key);
				if(bin==null){
					builder.append("NA ");
				}else{
					builder.append(Util.defaultFormat.format(bin.getAverage())).append(' ');
				}
			}
			builder.append('\n');
		}
		builder.append('\n');
	}
	
	private class Bin implements Comparable<Bin>{
		final int x;
		final int y;
		private final int hashCode;
		private double ldSum;
		private double ldSum2;
		private int count;
		
		public Bin(int x,int y) {
			this.x=x;
			this.y=y;
			hashCode=((x*37)+y)*13;
			
		}
		@Override
		public int compareTo(Bin o) {
			if(x>o.x)
				return 1;
			if(x<o.x)
				return -1;
			if(y>o.y)
				return 1;
			if(y<o.y)
				return -1;
			return 0;
		}
		
		@Override
		public int hashCode() {
			return hashCode;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Bin))
				return false;
			Bin o=(Bin)obj;
			return x==o.x && y==o.y;
		}
		
		public void addStat(double ld){
			ldSum+=ld;
			ldSum2+=ld*ld;
			count++;
		}
		
		public double getAverage(){
			return ldSum/count;
		}
		
		public String toString(){
			return "LDBin["+x+","+y+"]"+hashCode;
		}
		
	}

}
