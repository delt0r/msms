package at.mabs.stats;

import java.util.*;

import at.mabs.segment.FixedBitSet;
import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.Util;

public class Omega extends StatsCollectorAdapter {
	private int bins = 10;
	private int minW=30;
	private int maxW=2000;
	public Omega() {

	}

	public Omega(String name) {

	}

	public Omega(String name, String bins) {
		this.bins = Integer.parseInt(bins);
	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {

		List<InfinteMutation> mutations = recorder.getMutationsSorted();
		if (mutations.size() < minW)
			return new double[bins];// same size always... right!
		double[] data = new double[bins];

		// init with the first 2.
		BufferManager manager = new BufferManager();
		for (int i = 0; i < minW/2+1; i++) {
			manager.slideRight(mutations.get(i));
			//System.out.println(manager);
		}
		//now widen!
		while(manager.getWidth()<minW){
			manager.widen(mutations.get(manager.index - manager.getWidth()-1), mutations.get(manager.index));
		}

		while (manager.index < mutations.size()) {
			double o = maxOmega(manager, mutations);
			//System.out.println(manager + "\t" + o);
			double p = manager.getPosition();
			int b = (int) (p * bins);
			data[b] = Math.max(o, data[b]);

			// now move right... if we can't narrow first.
			if (manager.index == mutations.size()-1 && manager.getWidth() > minW) {
				manager.narrow();
			}
			if (manager.index < mutations.size()) {
				manager.slideRight(mutations.get(manager.index));
			}
		}

		return data;
	}

	private double maxOmega(BufferManager manager, List<InfinteMutation> mutations) {
		double o = manager.getOmega();
		double l=0;
		while (manager.index < mutations.size() && manager.index - manager.getWidth() > 0 && manager.getWidth()<maxW) {
			// first lets widen as far as we can. note we must also consider the
			// left edge.
			manager.widen(mutations.get(manager.index - manager.getWidth() - 1), mutations.get(manager.index));
			double no = manager.getOmega();
			if (no < o ) {
				//break;
			} else {
				o = no;
				l=manager.getWidth();
			}
		}
		// now try making it smaller!
		while (manager.getWidth() > minW) {
			manager.narrow();
			double no = manager.getOmega();
			if (no < o) {
				//break;
			} else {
				o = no;
				l=manager.getWidth();
			}
		}
		// now we are done. o contains the best score

		return l;
	}
	
	

	private class BufferManager {
		RFILOBuffer left = new RFILOBuffer();
		RFILOBuffer right = new RFILOBuffer();
		RFILOBuffer all = new RFILOBuffer();
		int index = 0;

		public void slideRight(InfinteMutation m) {
			if (left.size() > 0) {
				left.removeEnd();
				all.removeEnd();
			}
			if (right.size() > 0) {
				InfinteMutation rr = right.removeEnd();
				left.addFront(rr);
			}
			right.addFront(m);
			all.addFront(m);
			index++;
		}

		public void widen(InfinteMutation l, InfinteMutation r) {
			assert !left.mutationQue.contains(l);
			left.addEnd(l);
			right.addFront(r);
			all.addEnd(l);
			all.addFront(r);
			index++;
		}

		public void narrow() {
			if (all.size() < 2)
				return;// noop
			left.removeEnd();
			right.removeFirst();
			all.removeEnd();
			all.removeFirst();
			index--;
		}

		public int getWidth() {
			return all.size();
		}

		public double getSWidth(){
			return all.mutationQue.getFirst().position-all.mutationQue.getLast().position;
		}
		
		public double getPosition() {
			return (left.firstPosition() + right.endPosition()) / 2;
		}

		public double getOmega() {
			if(all.size()<4 || right.size()<2 || left.size()<2)
				return 0;
			double bottom = all.sum - left.sum - right.sum;
			double l = left.size();
			double r = right.size();

			double topNorm = l * (l - 1) / 2 + r * (r - 1) / 2;
			double bottomNorm = l * r;

			double omega = ((left.sum + right.sum) / topNorm) / (bottom / bottomNorm);
			return omega;
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "BfM:" + index + "," + left.size() + "," + right.size() + "," + all.size();
		}
	}

	private class RFILOBuffer {
		double sum;
		Deque<InfinteMutation> mutationQue = new LinkedList<InfinteMutation>();

		public void addFront(InfinteMutation m) {

			mutationQue.addFirst(m);

			sum += sumR2(m);
		}

		public void addEnd(InfinteMutation m) {
			mutationQue.addLast(m);

			sum += sumR2(m);
		}

		public double sumR2(InfinteMutation m) {
			FixedBitSet maskedSet = new FixedBitSet(m.leafSet);
			maskedSet.and(mask);
			double scount = mask.countSetBits();
			double r2sum = 0;
			double p = maskedSet.countSetBits() / scount;
			for (InfinteMutation n : mutationQue) {
				if (n == m)
					continue;
				double q = n.leafSet.countSetBitsMask(mask) / scount;

				double x11 = n.leafSet.countSetBitsMask(maskedSet) / scount;

				double D = x11 - p * q;
				double r2 = D * D / (p * q * (1 - p) * (1 - q));
				// System.out.println("r2:"+r2+"\tx11:"+x11+"\tp:"+p+"\tq:"+q
				// +"\t"+maskedSet+" "+n.leafSet);
				r2sum += r2;
			}
			return r2sum;
		}

		public double endPosition() {
			return mutationQue.getLast().position;
		}

		public double firstPosition() {
			return mutationQue.getFirst().position;
		}

		public InfinteMutation removeEnd() {

			InfinteMutation m = mutationQue.removeLast();
			sum -= sumR2(m);
			return m;
		}

		public InfinteMutation removeFirst() {
			// numberQue.removeLast();
			InfinteMutation m = mutationQue.removeFirst();
			sum -= sumR2(m);
			return m;
		}

		public int size() {
			return mutationQue.size();
		}

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return "B:" + mutationQue.getLast().position + " to " + mutationQue.getFirst().position + "," + size();
		}
	}

}
