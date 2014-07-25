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
package at.mabs.model.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


import at.mabs.cern.jet.random.Binomial;
import at.mabs.model.FrequencyState;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;
import at.mabs.model.PopulationSizeModel;
import at.mabs.util.random.RandomGenerator;

/**
 * This is the basic frequency and cumulative frequency data. Note that *all*
 * data is discrete in generations.
 * 
 * We store frequency as doubles. However they are always rational in that you
 * get an integer when Multiplying by N.
 * 
 * Each model has a Selection Data object if selection happens for that model.
 * Models know how to create them. Unlike Models Selection Data is *not*
 * finalised and can be reset or reused. They have the same start and end times
 * as there parent model...
 * 
 * Time increases into the past.
 * 
 * We put the method here too for now. So there won't be a "selectionCalculator"
 * 
 * @author bob
 */
public class SelectionData {
	private final Model parent;
	

	private final Binomial binomial = RandomGenerator.getBinomial();
	private final Random random = RandomGenerator.getRandom();

	private SelectionStrengthModel[] selectionStrength;

	private SuperFrequencyTrace frequencys;

	/*
	 * indicates that we shift the frequencys to match start time... not the
	 * other way round.
	 * 
	 * In this case indexOffset is adjust each simulation and frequencys can be
	 * reallocated if its too small
	 */
	// private boolean timeRescalling;
	private boolean forwardOnly;

	// private int generations;

	/*
	 * note creating this with a model with an end time at Double.MAX_VALUE may
	 * be a really bad idea.
	 */
	public SelectionData(Model model) {
		//System.err.println("Kreating SelectionData:"+model+'\t'+model.hashCode()+"Demes:"+model.getDemeCount());
		// start and end on integer generations.
		assert model.getStartTime() < model.getEndTime() : model.getStartTime();
		// if(true)throw new
		// RuntimeException(""+model.getStartTime()+"\t"+model.getEndTime());
		parent = model;
		ModelHistroy modelHistory = model.getModelHistory();
		forwardOnly = model.isForwardOnly();
		// if (!model.isFinalized())
		// throw new RuntimeException("Parent Model Not Finalized");

		// indexOffset =(int) model.getStartTime();
		// int gens =(int) (model.getEndTime()) - indexOffset;
		// System.out.println("Gens:"+gens+"\tms:"+model.getStartTime()+"\t"+model.getEndTime());

		// if(frequencys==null)
		// frequencys = new
		// FrequencyTrace((int)model.getStartTime(),(int)model.getStartTime()+gens,model.getDemeCount(),2);

		selectionStrength = new SelectionStrengthModel[model.getDemeCount()];
		//System.err.println("CREATING SSM:"+modelHistory.getSAA());
		if (model.getSelectionData() != null) {
			//System.err.println("NOt Null");
			selectionStrength = model.getSelectionData().selectionStrength.clone();

		}if(model.getParent()!=null && model.getParent().getSelectionData()!=null){
			SelectionStrengthModel[] previous=model.getParent().getSelectionData().selectionStrength; 
			for(int i=0;i<previous.length && i<selectionStrength.length;i++){
				selectionStrength[i]=previous[i];
			}
			//if there are extra demes. Set them to the normal defaults.
			SelectionStrengthModel ssm = new SelectionStrengthModel.Simple(modelHistory.getSaa(), modelHistory.getSaA(), modelHistory.getSAA());
			for(int i=previous.length;i<selectionStrength.length;i++){
				selectionStrength[i]=ssm;
			}
			
		} else {
			//System.err.println("Null");
			SelectionStrengthModel ssm = new SelectionStrengthModel.Simple(modelHistory.getSaa(), modelHistory.getSaA(), modelHistory.getSAA());

			for (int i = 0; i < selectionStrength.length; i++) {
				selectionStrength[i] = ssm;// shared instance
			}
		}
	//	System.err.println("SettingSelection:"+modelHistory.getSaa()+"\t"+modelHistory.getSAA()+"\t"+model);
		initFrequencyData();
	}

	private void initFrequencyData() {
		if (parent.getEndTime() == Long.MAX_VALUE) {
			// gens =8 * (int)
			// model.getPopulationSizeModels()[0].populationSize(model.getStartTime());
			// timeRescalling =true;
			frequencys = new SuperFrequencyTrace(parent.getDemeCount(), parent.getStartTime());
		} else // if (forwardOnly){
				// frequencys=new
				// FrequencyTrace((int)model.getStartTime(),(int)model.getStartTime()+1,model.getDemeCount(),2);
		// }else{
		{
			// System.err.println("About to crate:"+parent+"\t"+parent.hashCode());
			frequencys = new SuperFrequencyTrace(parent.getDemeCount(), parent.getStartTime(), (int) (parent.getEndTime() - parent.getStartTime() + 1));
			// System.err.println("kreated:"+parent+"\t"+frequencys.hashCode());
		}
		//System.err.println("freqs? "+frequencys);
	}

	public Model getParent() {
		return parent;
	}

	void setSelectionStrength(SelectionStrengthModel ssm) {
		Arrays.fill(selectionStrength, ssm);
	}

	void setSelectionStrength(int deme, SelectionStrengthModel ssm) {
		selectionStrength[deme] = ssm;
	}

	/**
	 * runs the simulation over this frequency block. We assume that the oldest
	 * time is set to the correct values. That is the "bridging" has been done
	 * between here and the older model. Older as in futher into the past.
	 * 
	 * However this is *not* done when we have a "stretchFreqeuncy" state... in
	 * this case we assume that the original state is all zeros or some initial
	 * state.
	 * 
	 * We need to be able to add events back to the ModelHistroy in a effecient
	 * way. Since this model is a single block and we know that other events
	 * don't need to be triggered over this range. We could have a simple
	 * inserting iterator...
	 * 
	 * These events also need to be removed for the next run. It makes sense
	 * that the calling method must deal with that.
	 * 
	 * we return a List(ordered) since Java Collections framework is a little
	 * suxor. ListIterator don't permit inserts... So the returned thing needs
	 * to be merged/sorted into the event list.
	 * 
	 * Note that previous elements added need to be removed. ie the volitile
	 * @return more events or null if need a restart aka rejection condition meet. 
	 */
	public List<ModelEvent> runSelectionSimulation() {
		// frequencys.reset();
		// frequencys.setIndexMostPastward();
		List<ModelEvent> events =null;
		//while(events==null){
		ModelHistroy modelHistory=parent.getModelHistory();
			events = modelHistory.getSelectionSimulator().forwardSimulator(parent, modelHistory, selectionStrength, frequencys);
		//}
		// frequencys.setUsed(true);
		// System.out.println("SelectionEvents"+events);
		return events;
	}

	public double getSweepTime() {
		return Double.NaN;// frequencys.getEndTime()-frequencys.getStartTime();
	}

	// private int resizeFrequency() {
	// if (RESIZE_CALL_LIMIT <= resizeCount) {
	// throw new
	// RuntimeException("Resized too many times. Perhaps Selection parameters preclude stoping conditions?");
	// }
	// resizeCount++;
	// // assume the normal doubling rule.
	// int oldL =frequencys[0][0].length;
	// int newL =frequencys[0][0].length * 2;
	// double[][][] newFreq =new double[frequencys.length][2][newL];
	// for (int d =0; d < frequencys.length; d++) {
	// System.arraycopy(frequencys[d][0], 0, newFreq[d][0], newL - oldL, oldL);
	// System.arraycopy(frequencys[d][1], 0, newFreq[d][1], newL - oldL, oldL);
	// }
	// frequencys =newFreq;
	// return newL - oldL;
	// }

	public void addDeme() {
		int size = selectionStrength.length;
		SelectionStrengthModel[] newSS = new SelectionStrengthModel[size + 1];
		System.arraycopy(selectionStrength, 0, newSS, 0, size);
		newSS[size] = selectionStrength[size - 1];// TODO could be better...
		selectionStrength = newSS;
		//if(frequencys!=null)
		//	throw new RuntimeException("Firetruck+"+frequencys);
		 //frequencys.addDeme();
	}

	public void getFrequencysFromStart(FrequencyState state) {
		// state.setCurrentDemeCount(frequencys.getDemeCount());
		// double[][] freqs = frequencys.getStart();
		// for (int i = 0; i < freqs.length; i++) {
		// state.setFrequency(i, 0, freqs[i][0]);
		// state.setFrequency(i, 1, freqs[i][1]);
		// }

		state.setCurrentDemeCount(parent.getDemeCount());
		// System.err.println(parent.getStartTime());
		frequencys.setIndexToStart();// setIndexTime(parent.getStartTime());
		double[] freqs = frequencys.getFrequencys(null);
		for (int i = 0; i < freqs.length; i++) {
			state.setFrequency(i, 0, 1 - freqs[i]);
			state.setFrequency(i, 1, freqs[i]);
		}
	}

	public SuperFrequencyTrace getFrequencys() {
		return frequencys;
	}

	/*
	 * only for internal use in modelHistory. this sets the default bridge
	 * between adjacent SelectionData in forward time. End in coalescent clock.
	 * 
	 * @param data
	 */
	public void setFrequencyToEnd(FrequencyState data) {
		assert data != null;
		// double[][] freqs = frequencys.getEnd();
		// for (int d = 0; d < freqs.length; d++) {
		// // System.out.println(d+" "+lastIndex);
		// freqs[d][0] = data.getFrequency(d, 0);
		// freqs[d][1] = data.getFrequency(d, 1);
		// }
		frequencys.setIndexMostPastward();
		double[] freqs = new double[parent.getDemeCount()];// frequencys.getFrequencys(null);
		//System.err.println("Doing Freq:"+parent.getDemeCount()+"\t"+data);
		for (int d = 0; d < freqs.length; d++) {
		// System.err.println("Freqs:"+d+" "+Arrays.toString(freqs));

			freqs[d] = data.getFrequency(d, 1);
		}
		frequencys.setFrequencys(freqs);
	}

	public double getFrequency(int deme, int allele, double time) {
		assert (time >= parent.getStartTime()) : time + ">=" + parent.getStartTime();
		assert (time <= parent.getEndTime()) : time + "<" + parent.getEndTime();
		// System.err.println("Getting Freq for C sim:"+deme+"\t"+allele+"\t"+time+"\t"+frequencys);
		// if(true)throw new RuntimeException("BORK");
		frequencys.setIndexTime((int) time);
		// System.err.println("time:"+time+"\t"+frequencys.getIndexTime()+"\t"+frequencys.getIndex()+"\tpStart:"+parent.getStartTime());
		// System.err.println(parent);
		double[] f = frequencys.getFrequencys(null);
		// System.err.println("OutPUT:"+f[0]);
		if (allele == 0)
			return 1 - f[deme];
		return f[deme];
	}

	public double coalescentCumulantIntegration(int deme, int allele, double time, double maxTime, double residue, int n) {
		//System.err.println("CALLED");
		if (time >= maxTime) {
			// System.out.println("Bork");
			return 0;
		}
		double sum = 0;
		PopulationSizeModel size = parent.getPopulationSizeModels()[deme];
		// System.out.println("IntSize:"+size.populationSize(time));
		// deal with the first partial sum.
		double f = getFrequency(deme, allele, time) * size.populationSize(time);
		// System.out.println("F:"+f+"\t"+n);
		if (f <= n) {
			// System.out.println("Zero!");
			return 0;
		}
		double delta = 1 - time + (int) time;
		sum += delta / f;
		double g = delta;// already dealt with the first increment.
		//System.err.println("gTime "+(g+time)+"\tMaxT:"+maxTime);
		while (sum < residue && (time + g) <= maxTime) {
			f = getFrequency(deme, allele, time + g) * size.populationSize(time + g);
			//System.err.println("loop:"+size.populationSize(time + g)+"\t"+f);
			if (f <= n) {
				// special case... more linages that population size. Force a C
				// event here...
				return g - delta;
			}
			if (f > 0) {
				sum += 1.0 / f;
			} else {
				// System.out.println("Zero");
				return maxTime - time;// fail...
			}
			// System.out.println("Fsum:"+sum+"\t"+f+"\t"+g);
			g++;
		}
		if (sum < residue) {
			// rounding errors has ment this is the most robust method.
			// we are saying that we failed to find an event.
			return Double.MAX_VALUE;
		}
		// now for the last increment. We have gone over with the last f. so we
		// need to subtract
		// some dt.

		double dr = sum - residue;
		// System.out.println("C:"+(time+g)+"\t"+dr);
		g -= dr * f;// dr/rate where rate=1.0/f;
		// System.out.println("C:"+(time+g));
		return g;
	}

	public double wildToSelectedMutationCumulantIntegration(int deme, int allele, double time, double maxTime, double residue) {
		if (time >= maxTime)
			return 0;

		double sum = 0;
		double g = 0;
		double f = getFrequency(deme, allele, time + g);
		if (f == 0)
			return 0;
		double delta = 1 - time + (int) time;
		sum += delta * (1.0 - f) / f;
		g += delta;
		// System.out.println("g:"+g);
		while (sum < residue && (time + g) < maxTime) {
			f = getFrequency(deme, allele, time + g);
			if (f > 0)
				sum += (1.0 - f) / f;
			else {
				return g;
			}
			g++;
		}
		if (sum < residue)
			return Double.MAX_VALUE;

		double dr = sum - residue;
		assert (1 - f) > 0;
		g -= dr * f / (1 - f);
		return g;
	}

	public double mutationCumulantIntegration(int deme, int fromAllele, int toAllele, double time, double maxTime, double residue) {
		if (time >= maxTime)
			return 0;

		double sum = 0;
		double g = 0;
		double from = getFrequency(deme, fromAllele, time + g);
		double to = getFrequency(deme, toAllele, time + g);
		if (to == 0)
			return 0;
		double delta = 1 - time + (int) time;
		sum += delta * from / to;
		g += delta;
		// System.out.println("g:"+g);
		while (sum < residue && (time + g) < maxTime) {
			from = getFrequency(deme, fromAllele, time + g);
			to = getFrequency(deme, toAllele, time + g);
			if (to > 0)
				sum += from / to;
			else {
				return g;
			}
			g++;
		}
		if (sum < residue)
			return Double.MAX_VALUE;

		double dr = sum - residue;
		assert from > 0;
		g -= dr * to / from;
		return g;
	}

	public double migrationCumulantIntegration(int deme, int allele, int directionIndex, double time, double maxTime, double residue) {
		if (time >= maxTime)
			return 0;

		double sum = 0;
		double g = 0;
		int demeJ = parent.getMigrationDirectionsByDeme()[deme][directionIndex];

		double fi = getFrequency(deme, allele, time + g);
		double fj = getFrequency(demeJ, allele, time + g);
		// deal with patial time.

		if (fi == 0) {

			return fj <= 0 ? Double.MAX_VALUE : 0;// if no event is posible..
													// don't return an event!
		}
		double delta = 1 - time + (int) time;
		sum += delta * fj / fi;
		g += delta;
		while (sum < residue && (time + g) < maxTime) {
			fi = getFrequency(deme, allele, time + g);
			fj = getFrequency(demeJ, allele, time + g);
			if (fi > 0) {// zeros are ok
				sum += fj / fi;
			} else if (fj > 0) {
				// again we ensure we just ignore 0/0 cases.
				return g;
			}
			// System.out.println("Zero");
			// System.out.println("Fsum:"+sum+"\t"+f+"\t"+g);
			g++;
		}
		if (sum < residue)
			return Double.MAX_VALUE;
		// adjust for partial time step.
		double dr = sum - residue;
		assert fi > 0;// should be true since fi/fj should not be zero
		// System.out.println("M:"+(g+time));
		g -= dr * fi / fj;// dr/rate where rate=1.0/f;
		return g;
	}

	@Override
	public String toString() {

		return "SelectionData parent:" + parent;
	}

}
