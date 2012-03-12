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
import java.util.*;

import at.mabs.util.Util;

/**
 * parse ms and Yuseob Kims output into a windowed theta and TjD estimates.
 * 
 * @author greg
 * 
 */
public class WindowThetaPiEstimator {
	private double[] thetaW;
	private double[] thetaW2;
	private double[] pi;
	private double[] pi2;
	private double[] tjd;
	private double[] tjd2;
	private double singleCount;
	private double singleCount2;
	private double segSites;
	private double segSites2;
	private int samples;
	private double binWidth;
	private double step;

	public WindowThetaPiEstimator(double bw, double step) {
		this.binWidth =bw;
		this.step =step;
		int s =(int) Math.ceil(1.0 / step);
		thetaW =new double[s];
		thetaW2 =new double[s];
		pi =new double[s];
		pi2 =new double[s];
		tjd =new double[s];
		tjd2 =new double[s];
	}

	public void parseInput(InputStream stream) {

		try {
			LineNumberReader lnr =new LineNumberReader(new InputStreamReader(stream));
			List<NavigableSet<Double>> sequence =parseNextSimulation(lnr);
			while (sequence != null) {
				addStatsFromSeq(sequence);

				sequence =parseNextSimulation(lnr);
			}

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public void printTable() {
		for (int i =0; i < 1.0 / step; i++) {
			double p =i * step;

			double thetaAve =thetaW[i] / samples;
			double thetaStd =Math.sqrt(thetaW2[i] / samples - thetaAve * thetaAve);

			double piAve =pi[i] / samples;
			double piStd =Math.sqrt(pi2[i] / samples - piAve * piAve);

			double tjdAve =tjd[i] / samples;
			double tjdStd =Math.sqrt(tjd2[i] / samples - tjdAve * tjdAve);

			System.out.println(p + "\t " + thetaAve + "\t " + thetaStd + "\t " + piAve + "\t " + piStd + "\t " + tjdAve + "\t " + tjdStd + "\t");

		}
		double sAve =singleCount / samples;
		double sVar =(singleCount2 / samples - sAve * sAve);
		
		double segAve=segSites/samples;
		double segVar=segSites2/samples-segAve*segAve;
		System.out.println("SegSites(VAR):" + segAve + "\t" + segVar);
		System.out.println("Singletons(VAR):" + sAve + "\t" + sVar);
	}

	/*
	 * not effecient at all.
	 */
	private void addStatsFromSeq(List<NavigableSet<Double>> seq) {
		if (seq.size() == 0)
			return;

		// we start at 0 and go to one.
		if (binWidth > 0) {
			double harmonic =Util.haromicNumber(seq.size() - 1);
			double inCr =seq.size() * (seq.size() - 1);
			inCr =2.0 / inCr;

			for (int i =0; i < 1.0 / step; i++) {
				double p =i * step;
				// first we do the thetaw then pi
				Set<Double> segSites =new HashSet<Double>();
				int pairwise =0;
				for (NavigableSet<Double> taxa : seq) {
					NavigableSet<Double> sub =taxa.subSet(p, true, p + binWidth, false);
					segSites.addAll(sub);
					for (NavigableSet<Double> taxa2 : seq) {
						if (taxa == taxa2)
							continue;
						NavigableSet<Double> sub2 =taxa2.subSet(p, true, p + binWidth, false);
						Set<Double> subC =new HashSet<Double>(sub);
						Set<Double> subC2 =new HashSet<Double>(sub2);
						subC.removeAll(subC2);
						pairwise +=subC.size();
						// subC2.removeAll(sub);
						// pairwise+=subC2.size();
					}
				}
				double tw =segSites.size() / (harmonic * binWidth);
				thetaW[i] +=tw;
				thetaW2[i] +=tw * tw;
				double pie =pairwise * inCr / binWidth;
				pi[i] +=pie;
				pi2[i] +=pie * pie;
				double td =Util.tjD(tw, pie, seq.size());
				tjd[i] +=td;
				tjd2[i] +=td * td;
			}
		}
		// now for singletons
		Set<Double> single =new HashSet<Double>();
		Set<Double> notSingle =new HashSet<Double>();

		for (NavigableSet<Double> taxa : seq) {
			for (Double site : taxa) {
				if (notSingle.contains(site))
					continue;
				if (single.contains(site)) {
					single.remove(site);
					notSingle.add(site);
				} else {
					single.add(site);
				}
			}
		}
		this.singleCount +=single.size();
		this.singleCount2 +=single.size() * single.size();
		int total=single.size()+notSingle.size();
		segSites+=total;
		segSites2+=total*total;
		this.samples++;
	}

	private List<NavigableSet<Double>> parseNextSimulation(LineNumberReader lnr) throws IOException {
		// first parse till we find a line starting with segsites
		String line =lnr.readLine();
		while (line != null && !line.trim().startsWith("segsites")) {
			line =lnr.readLine();
			// System.out.println("PassLine:"+line);
		}
		if (line == null)
			return null;
		
		List<NavigableSet<Double>> sequences =new ArrayList<NavigableSet<Double>>();
		
		// now we have the postion list starting point... ssw is crap so this is
		// needlessly complex to parse
		// both ssw and ms.

		// could have a single line with doubles on it. or multiple lines
		StringTokenizer tokens =new StringTokenizer(line, " \t:");
		tokens.nextToken();
		int segsites =Integer.parseInt(tokens.nextToken());
		// next line is the postion stuff.
		TreeSet<Double> postions =new TreeSet<Double>();

		line =lnr.readLine();
		tokens =new StringTokenizer(line, " \t");
		// discard postions: if there
		if(tokens.countTokens()==0)
			return sequences;
		String dis =tokens.nextToken();
		// System.out.println("discareded:"+dis);
		while (postions.size() < segsites) {
			while (tokens.hasMoreElements()) {
				String s =tokens.nextToken();
				// System.out.println(s+"\t"+segsites+"\t"+postions.size());
				Double postion =Double.parseDouble(s);
				while (postions.contains(postion)) {
					postion =postion + 1e-9;// dirty hack to avoid problems with duplicates
				}
				postions.add(postion);
			}
			line =lnr.readLine();
			// if(postions.size() < segsites)
			tokens =new StringTokenizer(line, " \t");
		}
		// should have a nice list of tokens...
		// System.out.println("Postions:"+postions+"\n"+line);
		// so now we have sequences... but ssw can have an empty line first!
		if (line.trim().length() == 0)
			line =lnr.readLine();

		
		while (line != null && line.trim().length() > 0) {
			int counter =0;
			TreeSet<Double> taxaSeq =new TreeSet<Double>();
			for (Double p : postions) {
				if (line.charAt(counter) == '1') {
					taxaSeq.add(p);
				}
				counter++;
			}
			sequences.add(taxaSeq);
			line =lnr.readLine();
		}
		// System.out.println(sequences);
		return sequences;
	}

	public static void main(String[] args) {
		double bw =Double.parseDouble(args[0]);
		double step =Double.parseDouble(args[1]);
		WindowThetaPiEstimator wte =new WindowThetaPiEstimator(bw, step);
		wte.parseInput(System.in);
		wte.printTable();
	}

}
