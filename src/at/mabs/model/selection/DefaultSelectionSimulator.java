package at.mabs.model.selection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


import at.mabs.cern.jet.random.Binomial;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;
import at.mabs.model.PopulationSizeModel;
import at.mabs.util.random.RandomGenerator;

/**
 * The basic simulator. Its getting more rewrites than you can shake a stick at. What is next? Well
 * we need to simplify the dam frequency trace stuff. Its too fragile and is using too much memory.
 * Push back to a single dimension array and do index arithmetic is a good start. However still have
 * lots of "conditions". For example we have stopping conditions. There is also restart conditions.
 * Restart conditions could go over model boundaries. So this needs to go outside this classes
 * scope. finally there are time shift Conditions when we use fixation time for finishing a sweep.
 * Also there is the situation where we may want the sweep to continue after such a condition is
 * meet. We also have a contract to add the correct start/stop selection events for this coalsecent
 * pass.
 * 
 * So even with the move to a simple single dim array that will save memory and avoid cache
 * pollution. I don't know how fix the other problems in a clean fashion. Consider that many loci
 * code will be added soon! Then there will be haplotypes and alleles.
 * 
 * For now lets consider than time information is *not* in frequency trace but in the model.
 * 
 * So now we have a "iteration" of selection. Check for boundary conditions. Resets are bubbled up
 * the call stack and must deal with removing any Cevent.
 * 
 * @author bob
 * 
 */

public class DefaultSelectionSimulator implements SelectionSimulator {
	private Binomial binomial =RandomGenerator.getBinomial();
	// private Random random=RandomGenerator.getRandom();
	private int ndeme;
	private PopulationSizeModel[] sizes;// =model.getPopulationSizeModels();
	private double[] totalMRates;// =model.getTotalMigrationRates();
	private int[][] migrationDirections;// =model.getMigrationDirectionsByDeme();
	private double[][] migrationRates;// =model.getMigrationRatesByDeme();
	private double mu;// =modelHistory.getForwardAlleleMutationRate();
	private double nu;// =modelHistory.getBackAlleleMutationRate();
	private double[] afterSelectionA;// =new double[ndeme];
	private double[] afterSelectiona;// =new double[ndeme];

	

	public DefaultSelectionSimulator() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ModelEvent> init(ModelHistroy modelHistory) {
		return new ArrayList<ModelEvent>();
	}

	// private Binomial binomial=RandomGenerator.getBinomial();
//	@Override
//	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace frequencys) {
//		// first init.
//
//		if (model.getEndTime() == Long.MAX_VALUE) {
//			// for now no initial frequencys are set.
//			initFrequencys(model, frequencys);
//		}
//		List<ModelEvent> events =new ArrayList<ModelEvent>();
//		int ndeme =model.getDemeCount();
//		// int end=frequencys[0][0].length - 1;
//		// int currentGen =frequencys[0][0].length - 1;// we count down: Always.
//		// int genCount=0;
//
//		double[] afterSelectionA =new double[ndeme];
//		double[] afterSelectiona =new double[ndeme];
//
//		double[] totalMRates =model.getTotalMigrationRates();
//		int[][] migrationDirections =model.getMigrationDirectionsByDeme();
//		double[][] migrationRates =model.getMigrationRatesByDeme();
//		double mu =modelHistory.getForwardAlleleMutationRate();
//		double nu =modelHistory.getBackAlleleMutationRate();
//		PopulationSizeModel[] sizes =model.getPopulationSizeModels();
//
//		boolean zeroFlag =true;
//		boolean fixFlag =true;
//		boolean addedSelectionStart =false;
//		boolean localTimeRescalling =model.getEndTime() == Long.MAX_VALUE;
//
//		FrequencyCondition stopping =modelHistory.getTimedCondition();
//		// System.out.println("Stopping:"+stopping);
//		frequencys.reset();
//		// frequencys.setIteratorToEnd();
//		while (false && localTimeRescalling) {
//
//			// System.out.println("In Loop:"+frequencys.getTime());
//			// first get all the selection amounts in each deme before
//			// migration.
//
//			// note this with be Integer.MAX_VALUE for fixation time invariant boundry conditions
//			int time =frequencys.getTime();
//			double[][] thisFreq =frequencys.current();
//			double[][] nextFreq =frequencys.previous(localTimeRescalling);
//			for (int d =0; d < ndeme; d++) {
//				if (sizes[d] == PopulationSizeModel.NULL_POPULATION)
//					continue;
//				double sAA =ssm[d].getStrength(1, 1, time);
//				double saA =ssm[d].getStrength(0, 1, time);
//				double saa =ssm[d].getStrength(0, 0, time);
//
//				double x =thisFreq[d][1];// frequencys[d][1][currentGen];// note the 2 allele
//				// assumption
//				double ax =1.0 - x;
//
//				if (x > 0)
//					zeroFlag =false;
//				if (x < 1.0)
//					fixFlag =false;
//
//				afterSelectionA[d] =x * (1 + ax * saA + x * sAA);
//				afterSelectiona[d] =ax * (1 + x * saA + ax * saa);
//			}
//
//			// now we deal with migration and update the next generations
//			// frequencys.
//
//			for (int d =0; d < ndeme; d++) {
//				if (sizes[d] == PopulationSizeModel.NULL_POPULATION) {
//					nextFreq[d][1] =0;// frequencys[d][1][nextGen] =0;
//					nextFreq[d][0] =0;// frequencys[d][0][nextGen] =0;
//					continue;
//				}
//				// first self migration.
//				double rate =1 - totalMRates[d];
//				double nA =rate * afterSelectionA[d];
//				double na =rate * afterSelectiona[d];
//
//				int[] directions =migrationDirections[d];
//				double[] rates =migrationRates[d];
//				for (int count =0; count < directions.length; count++) {
//					int j =directions[count];
//					rate =rates[count];
//					nA +=rate * afterSelectionA[j];
//					na +=rate * afterSelectiona[j];
//				}
//				// now put it together for xp
//				double xp =(nA * (1 - nu) + mu * na) / (nA + na);
//				// we keep N in the sample for now...
//				int N =(int) sizes[d].populationSize(time) * 2;//
//				double f =(double) binomial.generateBinomial(N, xp) / N;
//				// update
//				nextFreq[d][1] =f;
//				nextFreq[d][0] =1 - f;
//				// System.out.println(f+" "+selectionStrength[d].getStrength(1,
//				// 1, time));
//				// TIME is wrong because we may shift time...
//
//			}
//
//			// hard to read and not clear at all IMO.
//			// better ideas welcome
//			if (localTimeRescalling) {
//				if (zeroFlag) {
//					// reset
//					// System.out.println("RESET");
//					frequencys.setIteratorToEnd();
//				} else if (fixFlag) {
//					// we shift time and break
//					// first we want indexOffset to point to currentGen
//					frequencys.shiftAndSet((int) model.getStartTime(), (int) model.getStartTime());
//					events.add(new SelectionEndTimeEvent(frequencys.getEndTime()));
//					events.add(new SelectionStartEvent(frequencys.getStartTime(), null));
//					addedSelectionStart =true;
//					// do we have enough size?
//					localTimeRescalling =false;
//					// System.out.println("IffixFlagTrueRescalling:"+time+"\t"+frequencys.getStartTime());
//					break;
//				} else if (stopping != null && stopping.isMeet(nextFreq)) {
//					frequencys.shiftAndSet((int) stopping.getTime(), (int) model.getStartTime());
//					events.add(new SelectionEndTimeEvent(frequencys.getEndTime()));
//					// do we have enough size?
//					localTimeRescalling =false;
//					// System.out.println("OtherStopping:"+time);
//				}
//			} else if (fixFlag) {
//				// copy fixed to end and break
//				// System.out.println("MakeStart:"+time);
//				frequencys.shiftAndSet(frequencys.getTime(), frequencys.getTime());
//				// System.out.println("IffixFlagTrue:"+time);
//				events.add(new SelectionStartEvent(time, null));
//				addedSelectionStart =true; // dirty hack to stop adding a SelectionStart event after
//											// loop exit
//				// frequencys.setStart();
//				// for (int d =0; d < ndeme; d++) {
//				// Arrays.fill(frequencys[d][0], 0, currentGen, 0.0);
//				// Arrays.fill(frequencys[d][1], 0, currentGen, 1.0);
//				// }
//
//				break;
//			}
//			fixFlag =true;
//			zeroFlag =true;
//			// System.out.println("Iterate:"+toString(frequencys.current())+"\tTime:"+time+"\tft:"+frequencys.getTime());
//		}
//		// if we
//		if (!addedSelectionStart) {
//
//			// need to start the selection phase when we are not fixed.
//			double[] freq =new double[ndeme];
//			double[][] local =frequencys.current();
//			for (int i =0; i < freq.length; i++) {
//				freq[i] =local[i][1];
//			}
//			// System.out.println("notFixed "+Arrays.toString(freq)+"\t"+frequencys.getTime()+"\t"+model.getStartTime()+"\n\t"+events);
//			events.add(new SelectionStartEvent(frequencys.getTime(), freq));
//		}
//		// System.out.println("DoneSelection");
//		// so we are *almost* done...
//		return events;
//
//	}

	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace frequencys) {
		// first init.
		//System.err.println("Did we even call me?");
//		if (model.getEndTime() == Long.MAX_VALUE) {
//			// for now no initial frequencys are set.
//			initFrequencys(model, frequencys);
//		}
		List<ModelEvent> events =new ArrayList<ModelEvent>();
		ndeme =model.getDemeCount();
		// int end=frequencys[0][0].length - 1;
		// int currentGen =frequencys[0][0].length - 1;// we count down: Always.
		// int genCount=0;

		afterSelectionA =new double[ndeme];
		afterSelectiona =new double[ndeme];

		totalMRates =model.getTotalMigrationRates();
		migrationDirections =model.getMigrationDirectionsByDeme();
		migrationRates =model.getMigrationRatesByDeme();
		mu =modelHistory.getForwardAlleleMutationRate();
		nu =modelHistory.getBackAlleleMutationRate();
		sizes =model.getPopulationSizeModels();

		FrequencyCondition stopping =modelHistory.getTimedCondition();
		RestartCondition restartCondition=modelHistory.getRestartCondtion();
		//System.err.println("Stop:"+stopping+"\tstart:"+restartCondition);
		// boolean zeroFlag =true;
		// boolean fixFlag =true;

		// so we have 2 basic "modes" Condtion on fixation in time homegenous mode. Start with
		// defined conditions.
		// in the first case time is not important and we can assume time always ==
		// model.getStartTime();
		// in the second case we are bounded on the interval start time to end time. We assume that
		// our lovely
		// frequency trace data struct is allocated in such a way that this will work in both cases.
		// in both cases we have just one important difference. We don't add a selection end event
		// till we know about the
		// selelction startEvent for the first case. Otherwise we do everything quite linear like.
		// for now lets just fork to 2 different methods
		// right here....
		frequencys.reset();
		boolean fixationConidtion =model.getEndTime() == Long.MAX_VALUE;
		if (fixationConidtion) {
			//System.err.println("We are doing it baby");
			double time =model.getStartTime();// we use this time for the whole thing. Time
												// invaraint right!
			frequencys.setIndexMostPastward();// start at the very back.
			initFrequencys(model, frequencys);
			double[] thisGen =frequencys.getFrequencys(null);
			double[] nextGen =new double[thisGen.length];
			while (true) {
				//System.err.println("Oh yea!"+Arrays.toString(thisGen));
				selectionSimulationStep(thisGen, nextGen, ssm, time);
				if (restartCondition.isMeet(nextGen, ndeme)) {
					//System.out.println("Restart?");
					return null;//bubble up the call stack to where we need it. 
				}
				// copy data back.
				frequencys.moveForward();
				frequencys.setFrequencys(nextGen);
				// check if we are done.
				//System.err.println("Stopping?"+stopping.isMeet(nextGen));
				if (stopping.isMeet(nextGen)) {
					//System.err.println("We Stoped Doing it:"+Arrays.toString(nextGen)+"\t"+stopping);
					// shift and stop
					frequencys.setCurrentIndexAsStart();// good luck with that!
					// add events
					//System.err.println("TRACE:"+frequencys);
					events.add(new SelectionEndTimeEvent(frequencys.getTimeMostPastward()));
					events.add(new SelectionStartEvent(stopping.getTime(), nextGen));//well we already new That!
					frequencys.setEndTime();
					//System.err.println(events+"\t"+frequencys.getEndTime()+"\tPastward:"+frequencys.getTimeMostPastward());
					return events;//get me out here.
				}
				if (restartCondition.isMeet(nextGen, ndeme)) {
					frequencys.setIndexMostPastward();// start at the very back.
					initFrequencys(model, frequencys);
					frequencys.getFrequencys(thisGen);
					//System.err.println("START AGAIN:"+Arrays.toString(nextGen)+"\t"+stopping);
					continue;
				}
				double[] tmp =thisGen;
				thisGen =nextGen;// no need for a copy
				nextGen =tmp;// copied over. We hope!
			}

		}
		//so now we have a much simpler task of starting and finishing things at fixed times.
		//first we assume that inital conditions are specified and already done. 
		frequencys.setIndexMostPastward();
		long time=frequencys.getIndexTime();
		
		assert time==model.getEndTime():time+"\t"+model.getEndTime();
		double[] thisGen=frequencys.getFrequencys(null);
		double[] nextGen=new double[thisGen.length];
		//System.out.println("StartLoop 2 "+restartCondition.isMeet(thisGen, ndeme));
		while(frequencys.hasMoreForward()){
			selectionSimulationStep(thisGen, nextGen, ssm, time);
			frequencys.moveForward();
			frequencys.setFrequencys(nextGen);
			time=frequencys.getIndexTime();

			//System.out.println("restart.."+restartCondition.isMeet(nextGen, ndeme)+"\t"+Arrays.toString(nextGen)+"\t"+ndeme+"\ttime:"+time);
			if (restartCondition!=null && restartCondition.isMeet(nextGen, ndeme)) {
				return null;//bubble  the restart up the stack. 
			}
			//System.err.println("Stopping?"+stopping.isMeet(nextGen));
			if(stopping!=null && stopping.isMeet(nextGen)){
				events.add(new SelectionStartEvent(time, thisGen));
				//System.out.println("Stop:"+events+"\t"+Arrays.toString(nextGen)+"\t"+time);
				frequencys.setEndTime();
				return events;
			}
			if (restartCondition!=null && restartCondition.isMeet(nextGen, ndeme)) {
				frequencys.setIndexMostPastward();// start at the very back.
				frequencys.getFrequencys(thisGen);
				continue;
			}
			
			
			double[] tmp =thisGen;
			thisGen =nextGen;// no need for a copy
			nextGen =tmp;// copied over. We hope!
			//now what about stopping conditions?
			//meh..just add a start for now.
		}
		assert time==model.getStartTime():time+"\t"+model.getStartTime();
		if(time==0)
			events.add(new SelectionStartEvent(model.getStartTime(), thisGen));
		//System.out.println("FALLThrough:"+events);
		return events;
	}

	/*
	 * side effect. Uses class variables rather than "passing them" bit "doggy" but saves a sill
	 * number of arguments.
	 */
	private void selectionSimulationStep(double[] previous, double[] next, SelectionStrengthModel[] ssm, double time) {

		boolean fixFlag =true;
		boolean zeroFlag =true;
		// lots of class v set from the calling function.

		for (int d =0; d < ndeme; d++) {
			if (sizes[d] == PopulationSizeModel.NULL_POPULATION)
				continue;
			double x =previous[d];
			// assumption
			double ax =1.0 - x;
			if (x > 0)
				zeroFlag =false;
			if (x < 1.0)
				fixFlag =false;
			double sAA =ssm[d].getStrength(1, 1, time);
			double sAa =ssm[d].getStrength(1, 0, time);
			double saa =ssm[d].getStrength(0, 0, time);
			//System.err.println("SAA @ time:"+time+"\t"+sAA+"\t"+sAa+"\t"+saa);
			//System.err.println("x's:"+x+"\t"+ax);
			afterSelectionA[d] =x * (1 + ax * sAa + x * sAA);
			afterSelectiona[d] =ax * (1 + x * sAa + ax * saa);
		}

		for (int d =0; d < ndeme; d++) {
			if (sizes[d] == PopulationSizeModel.NULL_POPULATION) {
				next[d] =0;// frequencys[d][1][nextGen] =0;
				// nextFreq[d][0]=0;//frequencys[d][0][nextGen] =0;
				continue;
			}
			// first self migration.
			double rate =1 - totalMRates[d];
			double nA =rate * afterSelectionA[d];
			double na =rate * afterSelectiona[d];
			//for(int[] dier:migrationDirections)
			//	System.out.println(Arrays.toString(dier));
			int[] directions =migrationDirections[d];
			double[] rates =migrationRates[d];
			for (int count =0; count < directions.length; count++) {
				int j =directions[count];
				rate =rates[count];
				//System.err.println("After!"+Arrays.toString(afterSelectionA)+"\t"+j+"\tD:"+Arrays.toString(directions));
				nA +=rate * afterSelectionA[j];
				na +=rate * afterSelectiona[j];
			}
			// now put it together for xp
			
			double xp =(nA * (1 - nu) + mu * na) / (nA + na);
			// we keep N in the sample for now...
			int N =(int)Math.ceil( sizes[d].populationSize(time) * 2);//
			//System.err.println("CallBin "+N+" "+xp+"\t"+sizes[d].populationSize(time)+"\t"+nA+"\t"+na);
			double f =(double) binomial.generateBinomial(N, xp) / N;
			//System.err.println("return bin "+f);
			//what if N is zero!
			if(N==0){
				f=0;//throw new RuntimeException("NaN, PopulationSize is probably zero! "+N);
			}
			// update
			next[d] =f;
		}

	}

	private String toString(double[][] freqs) {
		String s ="";
		for (int i =0; i < freqs.length; i++) {
			s +=freqs[i][1] + "\t";
		}
		return s;
	}

	private static void initFrequencys(Model parent, SuperFrequencyTrace frequencys) {
		// assume frequecys is set to end.
		PopulationSizeModel[] sizes =parent.getPopulationSizeModels();
		double total =0;
		for (int d =0; d < sizes.length; d++) {
			total +=sizes[d].populationSize(0);// assuming homogenous time
												// model.
		}
		double pick =RandomGenerator.getRandom().nextDouble() * total;
		int d =0;
		total =0;
		for (int i =0; i < sizes.length; i++) {
			total +=sizes[d].populationSize(0);
			if (total >= pick) {
				d =i;
				break;
			}
		}
		int choosen=parent.getModelHistory().getSeedDeme();
		if(choosen>-1)
			d=choosen;
		double[] freqs =new double[sizes.length];// demeCount;
		freqs[d] =1.0 / (parent.getPopulationSizeModels()[d].populationSize(parent.getEndTime()));// FIXME

		frequencys.setFrequencys(freqs);
		//System.out.println("INITF:"+Arrays.toString(freqs));
	}

	

	// public static void initFrequencys(Model model,FrequencyTrace frequencys) {
	// double[][] freqs =frequencys.getEnd();
	// PopulationSizeModel[] sizes =model.getPopulationSizeModels();
	// double total =0;
	// for (int d =0; d < freqs.length; d++) {
	// freqs[d][1] =0;
	// freqs[d][0] =1;
	// total +=sizes[d].populationSize(0);// assuming homogenous time model.
	//
	// }
	// double pick =RandomGenerator.getRandom().nextDouble() * total;
	// int d =0;
	// total =0;
	// for (int i =0; i < sizes.length; i++) {
	// total +=sizes[d].populationSize(0);
	// if (total >= pick) {
	// d =i;
	// break;
	// }
	// }
	// freqs[d][1] =1.0 /
	// (model.getPopulationSizeModels()[d].populationSize(frequencys.getEndTime()) * 4);
	// freqs[d][0] =1.0 - freqs[d][1];
	//
	// }
}
