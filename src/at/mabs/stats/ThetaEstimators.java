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

import at.mabs.model.SampleConfiguration;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.FixedBitSet;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

/**
 * collects and sumerises a windowed watterson theta esitmator. bins start at
 * zero--or are aliended so zero is the cut between bins.
 * 
 * @author greg
 * 
 */
public class ThetaEstimators implements StringStatsCollector {

	private final double windowSize;
	private final double stepSize;
	private final boolean summaryOnly;
	private final double[] harmonicNumbers;
	private final SampleConfiguration sampleConfig;
	private final int leafCount;

	private final FixedBitSet[] demeMasks;
	private  int demeCount;
	
	private double lastTjD=0;
	private double lastTW=0;
	private double lastPI=0;

	private TreeMap<Bin, Bin> bins = new TreeMap<Bin, Bin>();
	private int collectedSamples;
	private double sumtEstimates;
	private double sumtEstimates2;
	private double sumpEstimates;
	private double sumpEstimates2;
	private double sumtjdEstimates;
	private double sumtjdEstimates2;

	public ThetaEstimators(SampleConfiguration sampleConfig, double windowSize, double stepSize, boolean summaryOnly) {
		this.windowSize = windowSize;
		this.stepSize = stepSize;
		this.summaryOnly = summaryOnly;
		this.sampleConfig = sampleConfig;
		demeCount=sampleConfig.getDemeCount();
		if(demeCount==1)
			demeCount=0;
		int totalSamples = sampleConfig.getMaxSamples();
		harmonicNumbers = new double[demeCount + 1];
		for (int i = 0; i < demeCount; i++) {
			if (sampleConfig.getTotalSamplesInDeme(i) >= 2) {
				harmonicNumbers[i] = Util.haromicNumber(sampleConfig.getTotalSamplesInDeme(i) - 1);
			}
		}
		//System.out.println("Config:"+Arrays.toString(sampleConfig));
		harmonicNumbers[demeCount] = Util.haromicNumber(totalSamples - 1);
		demeMasks=sampleConfig.getMasks();
		
		leafCount=sampleConfig.getMaxSamples();
	}
	

	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		// we do the sliding window is the simplest way. With a back iterator
		// and a front iterator. We step till we
		// get past the valid element...this way large overlaps don't hit
		// performance at all.
		collectedSamples++;
		List<InfinteMutation> segsites = recorder.getMutationsUnsorted();
		// we start with the first mutation. but windows are zero allined.
		if (segsites.isEmpty())
			return;
		double gloabalTW = segsites.size() / harmonicNumbers[harmonicNumbers.length-1];
		lastTW=gloabalTW;
		double gloabalPi=0;
		
		for(InfinteMutation im:segsites){
			gloabalPi+=im.leafSet.getPairwiseDifference();
		}
		gloabalPi/=leafCount*(leafCount-1)/2;
		
		lastPI=gloabalPi;
		
		double gloabalTd=Util.tjD(gloabalTW, gloabalPi, leafCount);
		lastTjD=gloabalTd;
		
		sumtEstimates += gloabalTW;
		sumtEstimates2+=gloabalTW*gloabalTW;
		sumpEstimates+=gloabalPi;
		sumpEstimates2+=gloabalPi*gloabalPi;
		sumtjdEstimates+=gloabalTd;
		sumtjdEstimates2+=gloabalTd*gloabalTd;
		if (!summaryOnly) {
			builder.append("ThetaW estimates: " + Util.defaultFormat.format(gloabalTW) +" "+Util.defaultFormat.format(gloabalPi)+ " "+Util.defaultFormat.format(gloabalTd)+"\n");
		}

		double windowStart = Math.floor(segsites.get(0).position / windowSize) * windowSize;

		int[] segCount = new int[demeCount+1];
		int[] piSum=new int[demeCount+1];
		
		double[] tlocalResults=new double[demeCount+1];
		double[] plocalResults=new double[demeCount+1];
		double[] tdlocalResults=new double[demeCount+1];
		
		Iterator<InfinteMutation> backIterator = segsites.iterator();
		InfinteMutation backElement = backIterator.next();
		
		Iterator<InfinteMutation> frontIterator = segsites.iterator();
		InfinteMutation frontElement = frontIterator.next();

		while (backElement != null) {
			double windowEnd = windowStart + windowSize;
			// first move the front iterator to the right place.
			while (backElement != null && backElement.position < windowStart) {
				for(int i=0;i<demeCount;i++){
					if(backElement.leafSet.bitsOverlap(demeMasks[i])){
						segCount[i]--;
						piSum[i]-=backElement.leafSet.getPairwiseDifferenceMask(demeMasks[i]);
					}
				}
				segCount[demeCount]--;
				piSum[demeCount]-=backElement.leafSet.getPairwiseDifference();
				if (backIterator.hasNext()) {
					backElement = backIterator.next();
				} else {
					backElement = null;
				}
			}
			// now the front iterator
			while (frontElement != null && frontElement.position <= windowEnd) {
				for(int i=0;i<demeCount;i++){
					if( frontElement.leafSet.bitsOverlap(demeMasks[i])){
						segCount[i]++;
						piSum[i]+=frontElement.leafSet.getPairwiseDifferenceMask(demeMasks[i]);
						
					}
				}
				segCount[demeCount]++;
				piSum[demeCount]+=frontElement.leafSet.getPairwiseDifference();
				if (frontIterator.hasNext()) {
					frontElement = frontIterator.next();
				} else {
					frontElement = null;
				}
				
			}
			// funny rounding method. But works not matter what the step size
			// is.
			Bin bin = new Bin(windowStart);
			// System.out.println(bin+"\t"+windowStart/stepSize);
			Bin current = bins.get(bin);
			if (current == null) {
				bins.put(bin, bin);
				current=bin;
			} 
			
			for(int i=0;i<demeCount+1;i++){
				double thetaW = segCount[i] / (harmonicNumbers[i] * windowSize);
				int n=leafCount;
				if(i<demeCount)
					n=sampleConfig.getTotalSamplesInDeme(i);
				double pi=2*piSum[i]/(windowSize*(n-1)*n);
				double td=Util.tjD(thetaW, pi, n);
				current.addSample(i, thetaW,pi,td);
				
				tlocalResults[i]=thetaW;
				plocalResults[i]=pi;
				tdlocalResults[i]=td;
			}
			if(!summaryOnly){
				builder.append(Util.defaultFormat.format(current.getPostion()));
				builder.append(' ');
				for(int i=0;i<demeCount+1;i++){
					builder.append(Util.defaultFormat.format(tlocalResults[i]));
					builder.append(' ');
				}
				for(int i=0;i<demeCount+1;i++){
					builder.append(Util.defaultFormat.format(plocalResults[i]));
					builder.append(' ');
				}
				for(int i=0;i<demeCount+1;i++){
					builder.append(Util.defaultFormat.format(tdlocalResults[i]));
					builder.append(' ');
				}
				
				builder.deleteCharAt(builder.length()-1);
				builder.append('\n');
			}
			
			windowStart += stepSize;
		}
		builder.append('\n');
	}

	public double getThetaW(){
		return sumtEstimates / collectedSamples;
	}
	
	public double getThetaWStd(){
		double m=sumtEstimates / collectedSamples;
		return Math.sqrt((sumtEstimates2 / collectedSamples)-m*m);
	}
	
	public double getPi(){
		return sumpEstimates / collectedSamples;
	}
	
	public double getPiStd(){
		double m=sumpEstimates / collectedSamples;
		return Math.sqrt(sumpEstimates2/collectedSamples-m*m);
	}
	
	public double getTjD(){
		return sumtjdEstimates / collectedSamples;
	}
	
	public double getTjDStd(){
		double m= sumtjdEstimates / collectedSamples;
		return Math.sqrt(sumtjdEstimates2/collectedSamples-m*m);
	}
	
	public double[][] getWindowedData(){
		if(bins==null || bins.size()<1)
			return new double[0][0];
		double[][] data=new double[bins.size()-1][7];
		int index=0;
		Iterator<Bin> iterator = bins.keySet().iterator();
		int sumIndex=demeCount;
		while(iterator.hasNext() && index<bins.size()-1){
			Bin bin=iterator.next();
			data[index][0]=bin.getPostion();
			data[index][1]=bin.getThetaWEstimate(sumIndex);
			data[index][2]=Math.sqrt(bin.getThetaWVar(sumIndex));
			data[index][3]=bin.getPiEstimate(sumIndex);
			data[index][4]=Math.sqrt(bin.getPiVar(sumIndex));
			data[index][5]=bin.getTdEstimate(sumIndex);
			data[index][6]=Math.sqrt(bin.getTdVar(sumIndex));
			index++;
		}
		return data;
	}
	
	@Override
	public void summary(StringBuilder builder) {
		builder.append("ThetaW Estimate Summaray: " + Util.defaultFormat.format(sumtEstimates / collectedSamples) + " ");
		builder.append(Util.defaultFormat.format(sumpEstimates / collectedSamples) + " ");
		builder.append(Util.defaultFormat.format(sumtjdEstimates / collectedSamples) + "\n");
		Iterator<Bin> iterator = bins.keySet().iterator();
		if(bins.isEmpty())
			return;//we just ignore emptys.
		double last = bins.firstKey().postion * stepSize;
		while (iterator.hasNext()) {
			Bin bin = iterator.next();
			last += stepSize;
			while (last < bin.postion * stepSize) {
				// builder.append(Util.defaultFormat.format(last)).append(" 0\n");
				last += stepSize;
			}
			builder.append(Util.defaultFormat.format(bin.getPostion()));
			builder.append(' ');
			for(int i=0;i<demeCount+1;i++){
				builder.append(Util.defaultFormat.format(bin.getThetaWEstimate(i)));
				builder.append(' ');
				builder.append(Util.defaultFormat.format(Math.sqrt(bin.getThetaWVar(i))));
				builder.append(' ');
			}
			for(int i=0;i<demeCount+1;i++){
				builder.append(Util.defaultFormat.format(bin.getPiEstimate(i)));
				builder.append(' ');
				builder.append(Util.defaultFormat.format(Math.sqrt(bin.getPiVar(i))));
				builder.append(' ');
			}
			for(int i=0;i<demeCount+1;i++){
				builder.append(Util.defaultFormat.format(bin.getTdEstimate(i)));
				builder.append(' ');
				builder.append(Util.defaultFormat.format(Math.sqrt(bin.getTdVar(i))));
				builder.append(' ');
			}
			builder.deleteCharAt(builder.length()-1);
			builder.append('\n');
			
		}
		builder.append('\n');
		// builder.append(bins.toString());
	}
	
	public double getLastPI() {
		return lastPI;
	}
	
	public double getLastTjD() {
		return lastTjD;
	}
	
	public double getLastTW() {
		return lastTW;
	}
	
	

	private class Bin implements Comparable<Bin> {
		private final int postion;
		private final double[] theight;//theta
		private final double[] pheight;//pi
		private final double[] tjd;
		
		private final double[] theight2;//theta
		private final double[] pheight2;//pi
		private final double[] tjd2;
		
		
		public Bin(double startP) {
			this.postion=(int) Math.floor(stepSize / 2 + startP / stepSize);
			theight = new double[demeCount + 1];// demes 0-n-1 and
			pheight = new double[demeCount + 1];
			tjd = new double[demeCount + 1];// totals.
			
			theight2 = new double[demeCount + 1];// demes 0-n-1 and
			pheight2 = new double[demeCount + 1];
			tjd2 = new double[demeCount + 1];// totals.
			
			
		}

		public void addSample(int d, double h,double p,double td) {
			theight[d] += h;
			theight2[d]+=h*h;
			pheight[d]+=p;
			pheight2[d]+=p*p;
			tjd[d]+=td;
			tjd2[d]+=td*td;
			
		}
		
		public double getThetaWEstimate(int i){
			
			return theight[i]/collectedSamples;
		}
		
		public double getPiEstimate(int i){
			return pheight[i]/collectedSamples;
		}
		
		public double getTdEstimate(int i){
			return tjd[i]/collectedSamples;
		}
		
		public double getThetaWVar(int i){
			double mean=theight[i]/collectedSamples;
			return theight2[i]/collectedSamples-mean*mean;
		}
		
		public double getPiVar(int i){
			double mean=pheight[i]/collectedSamples;
			return pheight2[i]/collectedSamples-mean*mean;
		}
		
		public double getTdVar(int i){
			double mean=tjd[i]/collectedSamples;
			return tjd2[i]/collectedSamples-mean*mean;
		}

		@Override
		public int compareTo(Bin o) {

			return postion - o.postion;
		}

		public double getPostion() {
			return postion*stepSize;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Bin other = (Bin) obj;
			if (postion != other.postion)
				return false;
			return true;
		}

		public String toString() {
			return "Bin[" + postion + " " + theight + "]";
		}

	}

}
