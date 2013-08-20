package at.mabs.external;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.jws.Oneway;

import at.mabs.abc.SGA;
import at.mabs.cmdline.CLNames;
import at.mabs.cmdline.CmdLineParser;
import at.mabs.util.Bag;
import at.mabs.util.random.RandomGenerator;
import cern.jet.random.Binomial;
import cern.jet.random.Poisson;

public class SlowerForward {
	private Binomial bin = RandomGenerator.getBinomial();
	private Random rand = RandomGenerator.getRandom();
	private Poisson poi = RandomGenerator.getPoisson();

	private int N = 1000;
	private double overN = 1.0 / N;
	private int gens = 8 * N;
	private double s = 0e-1;
	private double mu = 0e-3;
	// private double notmu = 1 - mu;

	private int sampleSize = 20;
	private int reps = 1000;
	private int jreps = 1;

	private String abcin;
	private String abcout;
	private int keep = 1000;
	private int iters = 1000000;
	private int minN=900;
	private int maxN = 1100;
	private double minS = 1e-4;
	private double maxS = .25;
	

	private boolean abcDataOut = false;
	private boolean abc=false;

	private Bag<MutationClass> mutations = new Bag<SlowerForward.MutationClass>();

	public SlowerForward() {

		MutationClass zero = new MutationClass();
		zero.trace[0] = 1;
		mutations.add(zero);
	}

	private void step(int gen) {
		int genp = gen + 1;
		// mutation ... rare aka 1 or less per gen. or in fact less than one per gen? perhaps relax that
		// new freq are now in gen+1
		int size = mutations.size();
		for (int i = 0; i < size; i++) {// ignore new stuff added to the end. order is only not preserved on remove.
			MutationClass mc = mutations.get(i);
			mc.trace[gen + 1] = mc.trace[gen];
			double p = mc.trace[gen] * mu * N;
			// int n = poi.nextInt(p);
			// for (int j = 0; j < n; j++) {
			if (p >= rand.nextDouble()) {
				MutationClass nmc = new MutationClass();
				nmc.parent = mc;
				nmc.mutCount = mc.mutCount + 1;
				nmc.trace[genp] = overN;
				mc.trace[genp] -= overN;
				nmc.startTime = genp;
				mutations.add(nmc);
				// System.out.println("added:"+p);
			}
		}
		// now selection.
		size = mutations.size();
		double sum = 0;
		for (int i = 0; i < size; i++) {
			MutationClass mc = mutations.get(i);
			double a = (1 - Math.min(mc.mutCount * s, 1)) * mc.trace[genp];
			sum += a;
			mc.trace[genp] = a;
		}
		// printGen(genp);
		// drift and remove zeros.
		int n = N;
		double p = 0;
		for (int i = 0; i < mutations.size(); i++) {
			MutationClass mc = mutations.get(i);
			double np = mc.trace[genp] / sum;
			// System.out.println(sum+"\t"+p);
			int nn = bin.generateBinomial(n, np / (1 - p));
			n -= nn;
			p += np;
			mc.trace[genp] = (double) nn / N;
			// System.out.println(nn+"\t"+mutations.size()+"\t"+i);
			if (nn == 0 && mc.mutCount>0) {
				//mutations.remove(i);
				//i--;
			}
		}
	}

	public void reset() {
		mutations.clear();
		MutationClass zero = new MutationClass();
		// MutationClass two = new MutationClass();
		zero.trace[0] = 1;
		Arrays.fill(zero.trace, 1);
		// two.trace[0]=.5;
		// Arrays.fill(two.trace, .5);
		mutations.add(zero);
		// mutations.add(two);
	}

	public void forwardSim() {

		reset();
		if (mu == 0)
			return;
		for (int i = 0; i < gens - 1; i++) {
			step(i);
			// printGen(i + 1);
			// System.out.println(mutations.size());
		}
		// printGen(gens-1);
		// System.out.println(mutations);

	}

	public void printGen(int gen) {
		for (MutationClass mc : mutations) {
			System.out.print(mc.mutCount + "|" + mc.trace[gen] + "\t");
		}
		System.out.println();
	}

	public double[] coalescentTimes(int samples, int startGen) {
		double[] outs = new double[samples * 2];
		outs[0] = mutations.size();// System.out.print(mutations.size()+"\t");
		Bag<MutationClass> cmuts = new Bag<SlowerForward.MutationClass>();
		int n = samples;
		double p = 0;
		int length = 0;
		int[] sfs = new int[samples - 1];
		for (int i = 0; i < mutations.size(); i++) {
			MutationClass mc = mutations.get(i);
			double np = mc.trace[startGen];
			int nn = bin.generateBinomial(n, np / (1 - p));
			n -= nn;
			p += np;
			// System.out.println(nn);
			mc.nodes = null;
			if (nn > 0) {
				mc.lines = nn;
				cmuts.add(mc);
				mc.nodes = new Bag<SlowerForward.Node>();
				for (int j = 0; j < nn; j++) {
					mc.nodes.add(new Node());
				}
			}
		}
		// slow version first. one gen at a time...
		int totalSamples = samples;
		int gen = startGen - 1;
		int gencount = 0;
		// int
		while (totalSamples > 1 && gen > 0) {
			// System.out.println(cmuts+"\t"+totalSamples);
			for (int i = 0; i < cmuts.size(); i++) {
				MutationClass mc = cmuts.get(i);
				int l = mc.lines;
				double crate = l * (l - 1) / (N * 2 * mc.trace[gen]);
				// System.out.println("CRate:"+crate);
				if (crate >= rand.nextDouble()) {
					mc.lines--;
					totalSamples--;
					Node a = mc.nodes.remove(mc.nodes.size() - 1);
					Node b = mc.nodes.remove(mc.nodes.size() - 1);

					int branchLa = gencount - a.time;
					int branchLb = gencount - b.time;
					int indexa = a.leafCount - 1;
					int indexb = b.leafCount - 1;
					// System.out.println("\n:"+indexa+"\t"+indexb);
					sfs[indexa] += branchLa;
					sfs[indexb] += branchLb;
					length += branchLa + branchLb;

					Node newNode = new Node(a, b, gencount);

					mc.nodes.addRandom(newNode, rand);
					// System.out.print(gencount+"\t");
					outs[samples - totalSamples] = gencount;
				}
				if (mc.startTime == gen) {
					// System.out.println("Mutation@ "+gen);
					MutationClass parent = mc.parent;
					parent.lines += mc.lines;
					if (!cmuts.contains(parent)) {
						cmuts.set(i, parent);
						parent.nodes = new Bag<SlowerForward.Node>();
						parent.lines = mc.lines;
					} else {
						cmuts.remove(i);
						i--;
					}
					parent.nodes.addAll(mc.nodes);
				}
			}
			gen--;
			gencount++;
		}
		if (gen == 0) {
			// System.err.println("Warning, past end:"+totalSamples+"\t"+cmuts);
			for (int i = 0; i < totalSamples - 1; i++) {
				// System.out.print(gencount+"\t");
			}
		}
		// System.out.print(length+"\t");
		outs[samples] = length;
		for (int i = 0; i < sfs.length; i++) {
			// System.out.print(sfs[i]+"\t");
			outs[samples + i + 1] = sfs[i];
		}
		// System.out.println();
		return outs;
	}

	/**
	 * unique mutation pattern. has a parent expect for the zero one.
	 * 
	 * @author bob
	 * 
	 */
	private class MutationClass {
		int mutCount = 0;
		MutationClass parent = null;

		double[] trace = new double[gens];
		int startTime;
		int lines;
		Bag<Node> nodes;

		@Override
		public String toString() {

			return "[" + mutCount + "," + lines + "]->" + parent;
		}
	}

	private class Node {
		public Node(Node a, Node b, int t) {
			time = t;
			leafCount = a.leafCount + b.leafCount;
		}

		public Node() {

		}

		int time = 0;
		int leafCount = 1;
	}
	
	private class Params implements Comparable<Params>{
		int N;
		double s;
		double distance;
		
		public Params(int N,double s,double d) {
			this.N=N;
			this.s=s;
			this.distance=d;
		}
		
		@Override
		public int compareTo(Params o) {
			if(distance>o.distance)
			return 1;
			
			return -1;
				
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return N+"\t"+s+"\t"+distance;
		}
	}

	@CLNames(names = { "-g" }, rank = 1)
	public void setGens(int gens) {
		this.gens = gens;
	}

	@CLNames(names = { "-mu" })
	public void setMu(double mu) {
		this.mu = mu;
	}

	@CLNames(names = { "-N" })
	public void setN(int n) {
		N = n;
		gens = 8 * N;
		this.overN = 1.0 / N;
	}

	@CLNames(names = { "-s" })
	public void setS(double s) {
		this.s = s;
	}

	@CLNames(names = { "" })
	public void setSampleSizeReps(int sampleSize, int reps) {
		this.sampleSize = sampleSize;
		this.reps = reps;
	}

	@CLNames(names = { "-jreps" })
	public void setJreps(int jreps) {
		this.jreps = jreps;
	}

	@CLNames(names = { "-abcin" })
	public void setAbcin(String abcin) {
		this.abcin = abcin;
	}
	
	@CLNames(names = { "-abco" })
	public void setAbcout(String abcout) {
		this.abcout = abcout;
	}
	
	@CLNames(names = { "-abcSimOut" })
	public void setAbcDataOutTrue() {
		this.abcDataOut = true;
	}

	@CLNames(names = { "-abc" })
	public void setAbcTrue() {
		this.abc = true;
	}
	
	public double[] run() {
		double[] abcout = new double[sampleSize * 2 - 2];// sfs with var.
		for (int i = 0; i < reps; i++) {
			forwardSim();
			for (int j = 0; j < jreps; j++) {
				double[] outs = coalescentTimes(sampleSize, gens - 1);
				if (!abcDataOut && !abc) {
					for (int k = 0; k < outs.length; k++) {
						System.out.print(outs[k] + "\t");

					}
					System.out.println();
				}
				for (int k = 0; k < sampleSize - 1; k++) {
					double sfs = outs[sampleSize + k + 1];
					abcout[k] += sfs;
					abcout[sampleSize + k - 1] += sfs * sfs;
				}

			}
		}
		for (int i = 0; i < abcout.length / 2; i++) {
			double c = (reps * jreps);
			double mu = abcout[i] / c;
			double std = (abcout[i + sampleSize - 1] / c) - mu * mu;
			abcout[i + sampleSize - 1] = Math.sqrt(std);
		}
		return abcout;
	}

	public void abc() {
		try {
			// data...
			double[] data = new double[sampleSize * 2 - 2];

			BufferedReader br = new BufferedReader(new FileReader(this.abcin));
			String line=br.readLine();
			br.close();
			StringTokenizer st=new StringTokenizer(line);
			for(int i=0;i<data.length;i++){
				data[i]=Double.parseDouble(st.nextToken());
			}
			
			TreeSet<Params> keepers=new TreeSet<SlowerForward.Params>();
			
			for(int i=0;i<iters;i++){
				int N=this.N;//(int)(minN+rand.nextDouble()*(maxN-minN));
				//this.setN(N);
				double s=minS+rand.nextDouble()*(maxS-minS);
				this.setS(s);
				double[] stats=run();
				Params p=new Params(N,s,distance(data, stats));
				keepers.add(p);
				if(keepers.size()>this.keep){
					keepers.pollLast();
				}
				
				if(i%10==0){
					Writer write=new OutputStreamWriter(new FileOutputStream(abcout));
					for(Params pam:keepers){
						write.write(pam+"\n");
					}
					write.close();
					System.out.println(i+"\trange:"+keepers.first().distance+" <-> "+keepers.last().distance);
				}
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	
	private double distance(double[] a,double[] b){
		double sum=0;
		for(int i=0;i<a.length;i++){
			double d=a[i]-b[i];
			sum+=d*d;
		}
		return sum;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SlowerForward sf = new SlowerForward();
		CmdLineParser<SlowerForward> parser = null;
		try {
			parser = new CmdLineParser<SlowerForward>(SlowerForward.class);
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}

		try {
			parser.processArguments(args, sf);
			if(!sf.abc){
			double[] out = sf.run();
			for(int i=0;i<sf.gens;i+=5){
				for(MutationClass mc:sf.mutations){
					System.out.print(mc.trace[i]+"\t");
				}
				System.out.println();
			}
			System.exit(1);
			
			if (sf.abcDataOut) {
				for (int i = 0; i < out.length; i++) {
					System.out.print(out[i] + "\t");
				}
				System.out.println();
			}
			}else{
				sf.abc();
			}
		} catch (Exception e) {
			System.err.println(parser.longUsage());
			e.printStackTrace();

		}

	}

}
