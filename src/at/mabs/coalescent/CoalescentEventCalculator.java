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
package at.mabs.coalescent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import at.mabs.model.Model;
import at.mabs.model.ModelHistroy;
import at.mabs.model.PopulationSizeModel;
import at.mabs.model.selection.SelectionData;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.segment.SegmentSetFactory;
import at.mabs.util.Util;
import at.mabs.util.random.RandomGenerator;

/**
 * 
 * 
 * @author bob
 * 
 */
public class CoalescentEventCalculator {
	private LineageState state;

	private Random random =RandomGenerator.getRandom();

	private final ModelHistroy modelHistroy;
	//private final int[] samples;

	private EventTracker eventTracker;

	public CoalescentEventCalculator(ModelHistroy modelHistroy) {
		this.modelHistroy =modelHistroy;
		//this.samples =modelHistroy.getSampleConfiguration();

	}

	private CoalescentEvent nextEvent(Model model) {
		// System.out.println("NormalEventsGeneration");
		// //so we produce the shortest event we can...
		// //
		// // //we are always unselected here... so we start with coalescent
		// times.
		CoalescentEvent cevent =nextCoalescent(model);
		CoalescentEvent nevent =recombinationEvent(model);
		if (nevent.dt < cevent.dt) {
			cevent =nevent;
		}
	//	System.out.println("NextEventTime:"+cevent);
		nevent =migrationEvent(model);
		if (nevent.dt < cevent.dt) {
			cevent =nevent;
		}

		return cevent;
	}

	/*
	 * Note that the order is important when frequency drop to zero abruptly. The idea here is that
	 * mutation goes before migration. Migration when both freqeuncys are zero don't work well
	 */
	private CoalescentEvent nextEventSelection(Model model) {
		double time =state.getCurrentTime();
		CoalescentEvent cevent =recombinationEvent(model);
		double maxTime =Math.min(cevent.dt + state.getCurrentTime(), state.getCurrentMaxTime());
		CoalescentEvent nevent =nextCoalescentSelection(model, maxTime);
		if (nevent.dt < cevent.dt && (nevent.dt + time) < maxTime) {
			cevent =nevent;
			maxTime =time + cevent.dt;
		}
		nevent =alleleMutationEvent(model, maxTime);
		if (nevent.dt < cevent.dt && (nevent.dt + time) < maxTime) {
			cevent =nevent;
			maxTime =time + cevent.dt;
		}

		nevent =migrationEventSelection(model, maxTime);
		if (nevent.dt < cevent.dt && (nevent.dt + time) < maxTime) {
			cevent =nevent;
			maxTime =time + cevent.dt;
		}
		// System.out.println("eventS:"+cevent);
		return cevent;
	}
	
	/**
	 * back to a more brute force model. May not be slower.... 
	 * @param model
	 * @return
	 */
	private CoalescentEvent nextEventSelectionStep(Model model){
		//first we get this times frequency. then we just do the plain rate thing. Events that
		//happen with rate>1 always happen!
		
		double currentTime =state.getCurrentTime();
		int deme =-1;
		PopulationSizeModel[] popSizes =model.getPopulationSizeModels();
		SelectionData selectionData =model.getSelectionData();
		//first consider constraints... 
		
		//ok now the typical just test each thing to see if it produces an event. 
		// to create the correct extreme cases where the coalescent sort of breaks down, we use exp random varables 
		// and reject when min(a,b,c...)>1. ie no event. This produces better behavour in the limiting cases.  
		for(int d=0;d<popSizes.length;d++){
			
		}
		
		return null;
	}

	//
	private CoalescentEvent nextCoalescent(Model model) {
		double currentTime =state.getCurrentTime();
		double dt =state.getCurrentMaxTime() - currentTime;
		int deme =-1;
		PopulationSizeModel[] popSizes =model.getPopulationSizeModels();
		for (int d =0; d < popSizes.length; d++) {
			int n =state.getLineageSize(d, 0);
			if (n < 2)
				continue;
			double ncr2 =n * (n - 1) / 4.0;//diploid
			double dtp =popSizes[d].generateWaitingTime(currentTime, ncr2, random);
			assert !Double.isNaN(dtp);
			if (dtp < dt) {
				deme =d;
				dt =dtp;
			}
		}
		if (deme < 0)
			return CoalescentEvent.NO_OP;
		final double ndt =dt;
		final int ndeme =deme;

		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {
				state.coalescentEvent(ndeme, 0, ndt);
			}

			public String toString() {
				return "CoalescentEvent:" + ndeme + "\t" + ndt;
			}

		};

	}

	/*
	 * works for both selection and non selection
	 */
	private CoalescentEvent recombinationEvent(final Model m) {
		double rate =m.getRecombinationRate();
		if (rate <= 0) {
			return CoalescentEvent.NO_OP;
		}
		final double totalWeight =state.getTotalRecombinationWeight();
		// System.out.println(totalWeight);
		double dt =-Math.log(random.nextDouble()) / (totalWeight * rate);
		// System.out.println("Recomb?:"+dt);
		if (state.getCurrentTime() + dt >= state.getCurrentMaxTime()) {
			return CoalescentEvent.NO_OP;
		}
		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {
				double p=state.recombinationEvent(totalWeight, this.dt);
				if(eventTracker!=null)
					eventTracker.recombinationEvent(state.getCurrentTime(), p);
			}

			public String toString() {
				return "Recombination:" + dt;
			}

		};
	}

	private CoalescentEvent migrationEventSlow(Model model) {
		double[][] mrates =model.getMigrationRatesByDeme();
		int[][] mdir =model.getMigrationDirectionsByDeme();
		int i =-1;
		int j =-1;
		double dt =state.getCurrentMaxTime() - state.getCurrentTime();
		for (int deme =0; deme < mdir.length; deme++) {
			for (int toDemeIndex =0; toDemeIndex < mdir[deme].length; toDemeIndex++) {
				double rate =state.getLineageSize(deme, 0) * mrates[deme][toDemeIndex];
				if (rate <= 0)
					continue;
				double ndt =-Math.log(random.nextDouble()) / rate;
				if (ndt < dt) {
					i =deme;
					j =mdir[deme][toDemeIndex];
					dt =ndt;
				}

			}
		}
		if (i < 0)
			return CoalescentEvent.NO_OP;
		final int fi =i;
		final int fj =j;
		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {

				state.migrationEvent(fi, fj, 0, dt);
			}
		};
	}

	private CoalescentEvent migrationEvent(Model model) {
		double[] totalRates =model.getTotalMigrationRates();
		//System.out.println("MigrationRates:"+Arrays.toString(totalRates)+" @time:"+model);
		double dt =state.getCurrentMaxTime() - state.getCurrentTime();
		int ei =-1;
		for (int d =0; d < model.getDemeCount(); d++) {
			double tRate =totalRates[d] * state.getLineageSize(d, 0);
			// System.out.println("MRate:"+tRate);

			if (tRate <= 0)
				continue;
			double ndt =-Math.log(random.nextDouble()) / (tRate);
			// System.out.println("MrateOut:"+ndt);
			if (ndt < dt) {
				// System.out.println("Mig:"+state.getLineageSize(d,
				// 0)+"\t"+ei);
				ei =d;
				dt =ndt;
			}

		}
		if (ei < 0)
			return CoalescentEvent.NO_OP;

		final int fi =ei;
		final double fullRate =totalRates[ei];
		final int[] migrationDirections =model.getMigrationDirectionsByDeme()[ei];
		final double[] rates =model.getMigrationRatesByDeme()[ei];

		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {
				super.completeEvent();
				double rand =random.nextDouble() * fullRate;
				for (int i =0; i < rates.length; i++) {
					rand -=rates[i];
					if (rand < 0) {

						state.migrationEvent(fi, migrationDirections[i], 0, this.dt);
						return;
					}
				}

			}

			public String toString() {
				return "Migration:" + fi + "\t" + dt;
			}
		};
	}

//	private CoalescentEvent nextCoalescentSelectionStep(Model m, double maxTime) {
//		SelectionData selectionData =m.getSelectionData();
//		double time =state.getCurrentTime();
//		int deme =-1;
//		int allele =-1;
//		double dt =maxTime - time;
//		for (int d =0; d < m.getDemeCount(); d++) {
//			for (int a =0; a < 2; a++) {
//				int n =state.getLineageSize(d, a);
//				if (n < 2)
//					continue;
//
//				int ncr2 =n * (n - 1);//FIXME
//				int it =1;
//				while (it <= dt) {// selectionData.getFrequency(d, a, it+time)
//					double cp =ncr2 / (m.getPopulationSizeModels()[d].populationSize(it + time) * selectionData.getFrequency(d, a, it + time));
//					if (cp > random.nextDouble())
//						break;
//					it++;
//				}
//				double ndt =it;
//				if (ndt < dt) {
//					deme =d;
//					allele =a;
//					dt =ndt;
//				}
//
//			}
//		}
//		if (deme < 0) {
//			return CoalescentEvent.NO_OP;
//		}
//		final int fdeme =deme;
//		final int fallele =allele;
//
//		return new CoalescentEvent(dt) {
//			@Override
//			public void completeEvent() {
//				state.coalescentEvent(fdeme, fallele, this.dt);
//			}
//
//			public String toString() {
//				return "StepCL:" + fdeme + " " + fallele + " dt:" + dt;
//			}
//		};
//	}

	private CoalescentEvent nextCoalescentSelection(Model m, double maxTime) {
		SelectionData selectionData =m.getSelectionData();
		assert selectionData!=null;
		double time =state.getCurrentTime();
		int deme =-1;
		int allele =-1;
		PopulationSizeModel[] pops=m.getPopulationSizeModels();
		double dt =maxTime - time;
		for (int d =0; d < m.getDemeCount(); d++) {
			PopulationSizeModel popModel=pops[d];
			for (int a =0; a < 2; a++) {
				int n =state.getLineageSize(d, a);
				if (n < 2)
					continue;
				double ncr2 =n * (n - 1) / 4.0;//diplod

				double effectiveCount=popModel.populationSize(time)*selectionData.getFrequency(d, a, time);
				double U=0;
				//System.err.println("EFF SIZE:"+effectiveCount+"\t"+n);
//				if(effectiveCount<=n){
//					//force a event
//					double maxDt=Math.ceil(time)-time;
//					U=1-random.nextDouble()*(1-Math.exp(-4*maxDt*ncr2/effectiveCount));
//				}else{
//					U=random.nextDouble();
//				}
				
				double residue =-Math.log(random.nextDouble()) / ncr2;
				double ndt =selectionData.coalescentCumulantIntegration(d, a, time, time + dt, residue,n);// residue*m.getPopulationSizeModels()[d].populationSize(time);//
//				if(effectiveCount<=n){
//					ndt=0;
//					deme =d;
//					allele =a;
//					dt =ndt;
//					break;
//				}
				//System.err.println("DT:"+ndt);
				if (ndt < dt) {
					deme =d;
					allele =a;
					dt =ndt;
				}

			}
		}
		
		if (deme < 0) {
			return CoalescentEvent.NO_OP;
		}
		final int fdeme =deme;
		final int fallele =allele;

		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {
				state.coalescentEvent(fdeme, fallele, this.dt);
			}

		};
	}

	private CoalescentEvent migrationEventSelection(Model model, double maxTime) {
		SelectionData selectionData =model.getSelectionData();
		double time =state.getCurrentTime();
		int i =-1;
		int j =-1;
		int allele =-1;
		double dt =maxTime - time;
		int[][] directions =model.getMigrationDirectionsByDeme();
		double[][] rates =model.getMigrationRatesByDeme();
		for (int deme =0; deme < state.getDemeCount(); deme++) {
			for (int dIndex =0; dIndex < directions[deme].length; dIndex++) {
				double thisRate =rates[deme][dIndex];
				for (int a =0; a < 2; a++) {
					int n =state.getLineageSize(deme, a);
					if (n == 0 || thisRate <= 0)
						continue;
					double residue =-Math.log(random.nextDouble()) / (thisRate * n);
					double ndt =selectionData.migrationCumulantIntegration(deme, a, dIndex, time, time + dt, residue);
					if (ndt < dt) {
						i =deme;
						j =directions[deme][dIndex];
						allele =a;
						dt =ndt;
					}

				}
			}
		}
		if (i < 0)
			return CoalescentEvent.NO_OP;
		final int fi =i;
		final int fj =j;
		final int fallele =allele;
		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {

				state.migrationEvent(fi, fj, fallele, dt);
			}
		};
	}

	private CoalescentEvent alleleMutationEvent(Model model, double maxTime) {
		SelectionData selectionData =model.getSelectionData();
		double time =state.getCurrentTime();
		double dt =maxTime - time;
		double rateForward =model.getModelHistory().getForwardAlleleMutationRate();
		double rateBackward =model.getModelHistory().getBackAlleleMutationRate();
		if ((rateForward + rateBackward) <= 0)
			return CoalescentEvent.NO_OP;
		int deme =-1;
		int allele =-1;
		for (int d =0; d < model.getDemeCount(); d++) {
			int n =state.getLineageSize(d, 1);

			// note the odd rate. this is just pulling constant factors out of
			// the sum and
			// putting them into the residue. see soft sweeps 2.
			if (n > 0 && rateForward>0) {
				double residueForward =-Math.log(random.nextDouble()) * (1 - rateForward) / (n * rateForward);
				// needs imporvment.
				// double ndt = selectionData.wildToSelectedMutationCumulantIntegration(d, 1, time, maxTime, residueForward);
				double ndt =selectionData.mutationCumulantIntegration(d, 0, 1, time, time+dt, residueForward);
				//assert Math.abs(ndt-ndt2)<1e-4: ndt+"\t"+ndt2;
				if (ndt < dt) {
					deme =d;
					dt =ndt;
					allele =1;
				}
			}
			n =state.getLineageSize(d, 0);
			if (n > 0 && rateBackward>0) {
				double residueBackward =-Math.log(random.nextDouble()) * (1 - rateBackward) / (n * rateBackward);
				double ndt =selectionData.mutationCumulantIntegration(d, 1, 0, time, time+dt, residueBackward);
				if (ndt < dt) {
					deme =d;
					dt =ndt;
					allele =0;
				}
			}
		}
		if (deme < 0)
			return CoalescentEvent.NO_OP;
		final int fdeme =deme;
		final int fromAllele =allele;
		final int toAllele =allele == 1 ? 0 : 1;// swap
		return new CoalescentEvent(dt) {
			@Override
			public void completeEvent() {
				state.alleleMutationEvent(fdeme, fromAllele, toAllele, dt);

			}
		};
	}

	/**
	 * All the "results" get stored in mutationModel.
	 * 
	 * @param modelHistroy
	 * @param samples
	 * 
	 */
	public void calculateCoalescentHistory(SegmentEventRecoder segmentRecorder) {
		segmentRecorder.clear();
		state =new LineageState(modelHistroy, new SegmentSetFactory(modelHistroy, segmentRecorder));
		

		// now simulate...
		ModelHistroy.BackwardIterator iterator =modelHistroy.backwardIterator();
		Model model =iterator.getCurrentModel();
		
		state.initState(model.getSelectionData());
		
		double time =iterator.getNextEventTime();
		if (time == 0) {
			iterator.nextModel(state);// the inital or zero time events
			//time =iterator.getNextEventTime();
			model =iterator.getCurrentModel();
		}
		time =iterator.getNextEventTime();
		state.setCurrentMaxTime(time);
		double timeBound=1e5*modelHistroy.getN();
		while (state.getLinagesActive() > 1 && state.getCurrentTime()<timeBound) {
			//System.err.println("Lines:"+state.getCurrentTime()+"\t"+time+"\tTB:"+timeBound);
			CoalescentEvent cevent =null;
			//System.out.println("SelectionSTATEs:"+state.isSelection()+"\t"+state);
			if (state.isSelection()) {
				cevent =nextEventSelection(model);
			} else {
				cevent =nextEvent(model);
			}

			//System.err.println("CEvent:"+cevent+"\t"+state.getCurrentTime()+"\t"+model+"\ttimeBound:"+timeBound+"\t"+model.getSelectionData()+"\ttime:"+time+"\t"+state.isSelection());
			// System.out.println("State:"+state);
			if (cevent == CoalescentEvent.NO_OP || (cevent.dt + state.getCurrentTime()) >= time) {
				//System.err.println("NextModel");
				// move to the next event.
				iterator.nextModel(state);
				model =iterator.getCurrentModel();
				time =iterator.getNextEventTime();
				state.setCurrentMaxTime(time);
			} else {
				cevent.completeEvent();
			}
		}
		//there is one thing we need to consider. We may have one lineage and it may have the selected allele. 
		//we need to *finish* all selection end events (or start events for that matter)
//		System.err.println("Almost Perge");
//		while(model.getEndTime()<Long.MAX_VALUE){
//			System.err.println("Perging models:"+model);
//			iterator.nextModel(state);
//			model=iterator.getCurrentModel();
//			time=iterator.getNextEventTime();
//			state.setCurrentMaxTime(time);
//		}
		iterator.finishSelectionEvents(state);
		if(state.getCurrentTime()>=timeBound){
			throw new RuntimeException(" ARG ran past the time bound of 1e5*N generations! Check that there is not complete isolation:"+timeBound+"\t"+state.getCurrentTime());
		}
		//System.out.println("FINISHING!!");
		segmentRecorder.finishRecording();
		// Node root =state.getLastNode();
		// root.mutateSequence(modelHistroy.getNeutralMutationRate());
		// leafs.add(root);

	}

	/**
	 * note this is the total number of selection mutation counts including via hitch hicking. 
	 * @return
	 */
	public int getSelectedAlleleMutationCount() {
		return state.getSelectionMutationCount();
	}

	public void setEventTracker(EventTracker eventTracker) {
		this.eventTracker=eventTracker;
		
	}

	
}
