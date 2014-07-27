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
package at.mabs.model;

import java.io.PrintStream;
import java.util.*;

import at.mabs.coalescent.LineageState;
import at.mabs.model.selection.BasicFrequencyCondition;
import at.mabs.model.selection.DefaultSelectionSimulator;
import at.mabs.model.selection.FrequencyCondition;
import at.mabs.model.selection.FrequencyTrace;
import at.mabs.model.selection.SelectionData;
import at.mabs.model.selection.SuperFrequencyTrace;

import at.mabs.model.selection.SelectionEndTimeEvent;
import at.mabs.model.selection.SelectionSimulator;
import at.mabs.model.selection.SelectionStartEvent;
import at.mabs.model.selection.SelectionTimeConditionEvent;
import at.mabs.model.selection.RestartCondition;
import at.mabs.stats.ForwardStatsCollector;

/**
 * the container for models and events. It describes a history of models and how to map changes between them. Everything has a strict order.
 * 
 * We also provide a place for gloabal parameters. Such as mutation rate, recombination rate and forward/reverse benifical mutation rate.
 * 
 * @author bob
 * 
 */
public class ModelHistroy {
	private double recombinationRate;
	private long recombinationCutSites;

	// wild type to selectied
	private double forwardAlleleMutationRate;

	// selelted to wild type.
	private double backAlleleMutationRate;

	private double neutralMutationRate;
	private double segSiteCount;
	private double alleleLocation;
	private double sAA, saA, saa;
	private double N;
	private SampleConfiguration sampleConfig;
	private double[] lociConfiguration = { 0, 1 };

	private FrequencyCondition timedCondition = new BasicFrequencyCondition(-1, -1, 1);// fixation!
	private RestartCondition restartCondtion = new RestartCondition.Default();// restart on none.
	/*
	 * each model has a set of events between it and the next model. If there is only one model the event set is empty. these need to be both a list and a
	 * Deque... we want constant time insertion and removal. Almost all changes should be done via ListIterators.
	 */
	private LinkedList<Model> models;
	private LinkedList<ModelEvent> events;
	// private LinkedList<Object> modelsAndEvents;

	private boolean selection;

	private SelectionSimulator selectionSimulator = new DefaultSelectionSimulator();

	private ForwardStatsCollector forwardTraceOutput;
	private boolean foldMutations;
	private boolean unphase;
	private boolean weightedMutations;
	private double maxRecombinationRate = Double.MAX_VALUE;
	private int seedDeme;

	/**
	 * creates the models. builds up the finalised models from the events. This will not mutate the events list...
	 * 
	 * @param N
	 * @param nDemes
	 * @param m
	 * @param events
	 */
	public ModelHistroy(int nDemes, double N, double m, List<ModelEvent> e, boolean forwardOnly) {
		Model model = new Model(this, nDemes, N, m);
		model.setForwardOnly(forwardOnly);
		this.N = N;
		events = new LinkedList<ModelEvent>(e);
		Collections.sort(events);
		// System.out.println("Sorted:"+events);
		// find a selection event
		// for (ModelEvent me : events) {
		// if ((me instanceof SelectionEndTimeEvent) || (me instanceof SelectionTimeConditionEvent)) {
		// selection =true;
		// break;
		// }
		// }

		List<ModelEvent> shortList = new ArrayList<ModelEvent>();

		models = new LinkedList<Model>();

		// so now we need to be a little smarter as not all the events can now be removed.
		// we need to use iterators that don't have a "current"
		Iterator<ModelEvent> iterator = events.listIterator();
		double time = 0;
		// new version
		// add the basic model.
		// models.add(model);
		// model.commitObject();
		// model=new Model(model);
		// System.out.println("modelFirst:"+Arrays.tomodel.getMigrationRatesByDeme());
		while (iterator.hasNext()) {
			ModelEvent event = iterator.next();
			if (event.getEventTime() == time) {
				// collect all events for this model break
				shortList.add(event);
			} else {
				// System.err.println("ShortList:"+shortList);
				// we have finished this model break...
				model.setEndTime(event.getEventTime());
				// so end and start time are set
				for (ModelEvent me : shortList) {
					me.modifiyModel(model);
				}
				model.commitObject();
				// model.initSelectionData();
				models.add(model);
				// now for the next model
				model = new Model(model);
				model.setStartTime(event.getEventTime());
				time = event.getEventTime();
				shortList.clear();
				shortList.add(event);
			}
			if (event.isModelOnly()) {
				// iterator.remove(); //BIG CHANGE. No Events Removed.
				// System.out.println("RM:"+event+"\t"+event.getEventTime());
			}
		}
		// now finish the last model
		model.setEndTime(Long.MAX_VALUE);
		for (ModelEvent me : shortList) {
			me.modifiyModel(model);
		}
		model.commitObject();
		models.add(model);
		// System.err.println("Models:" + models);
		// for (Model mm : models) {
		// System.err.println("WTH:" + mm.hashCode());
		// }
	}

	public int getMaxDemeCount() {
		return models.getLast().getDemeCount();
	}

	public int getFirstModelDemeCount() {
		// System.err.println("FirstModel:"+models.size());
		return models.getFirst().getDemeCount();
	}

	public double getRecombinationRate() {
		return recombinationRate;
	}

	public void setRecombinationRate(double recombinationRate) {
		this.recombinationRate = recombinationRate;
	}

	public void setRecombinationCutSites(long recombinationCutSites) {
		this.recombinationCutSites = recombinationCutSites;
	}

	public long getRecombinationCutSites() {
		return recombinationCutSites;
	}

	public double getForwardAlleleMutationRate() {
		return forwardAlleleMutationRate;
	}

	public void setForwardAlleleMutationRate(double alleleMutationRate) {
		this.forwardAlleleMutationRate = alleleMutationRate;
	}

	public double getBackAlleleMutationRate() {
		return backAlleleMutationRate;
	}

	public void setBackAlleleMutationRate(double backAlleleMutationRate) {
		this.backAlleleMutationRate = backAlleleMutationRate;
	}

	public double getNeutralMutationRate() {
		return neutralMutationRate;
	}

	public void setNeutralMutationRate(double neutralMutationRate) {
		this.neutralMutationRate = neutralMutationRate;
	}

	public double getAlleleLocation() {
		return alleleLocation;
	}

	public void setAlleleLocation(double alleleLocation) {
		this.alleleLocation = alleleLocation;
	}

	public BackwardIterator backwardIterator() {
		return new BackwardIterator();
	}

	public double getMaxRecombinationRate() {
		return maxRecombinationRate;
	}

	public void setMaxRecombinationRate(double maxRecombinationRate) {
		this.maxRecombinationRate = maxRecombinationRate;
	}

	public boolean isSelection() {
		return selection;
	}

	public double getSAA() {
		return sAA;
	}

	public double getSaA() {
		return saA;
	}

	public double getSaa() {
		return saa;
	}

	public void setSAA(double saa) {
		sAA = saa;
	}

	public void setSaA(double saA) {
		this.saA = saA;
	}

	public void setSaa(double saa) {
		this.saa = saa;
	}

	public double getN() {
		return N;
	}

	public List<SuperFrequencyTrace> getSelectionTraces() {
		List<SuperFrequencyTrace> traces = new ArrayList<SuperFrequencyTrace>();
		if (!selection)
			return traces;
		Iterator<Model> modIter = models.iterator();
		while (modIter.hasNext()) {
			Model model = modIter.next();
			SelectionData sd = model.getSelectionData();
			if (sd != null) {
				SuperFrequencyTrace ft = sd.getFrequencys();
				if (ft != null) {
					traces.add(ft);
					// System.out.println("Model:"+model+"\n\t"+sd+"\n\t"+ft);
				}
			}
		}
		Collections.reverse(traces);
		return traces;
	}

	public double simulateSelection() {
		// ugly as hell. Required to supprt -SFC/
		// if an event results in a resampling that hits the restart condition we can't restart locally. we must restart from the real start.
		while (true) {
			// System.err.println("MH simulation called in the while:"+selection);
			boolean restartFlag = false;
			if (!selection)
				return 0;
			if (N == Integer.MAX_VALUE || N == Long.MAX_VALUE)// not correct...its a note for me
				throw new RuntimeException("Must use the -N option with selection");

			// System.err.println("Do i have any events that we give a crap about?:"+events);
			// first lets clear volitoles
			Iterator<ModelEvent> iterator = events.iterator();
			while (iterator.hasNext()) {
				ModelEvent me = iterator.next();
				if (me.isVolatileSelectionEvent())
					iterator.remove();
			}

			// init simulator
			List<ModelEvent> moreEvents = getSelectionSimulator().init(this);// more FIXME we just don't have the class design right here
			events.addAll(moreEvents);
			Collections.sort(events);
			//System.out.println(events);
			// System.err.println("more evets?:"+events);

			// we find the start and keep calling till we are either at the last model, or
			// we hit a start selection event. then all all the new selection events...
			Iterator<ModelEvent> eventIterator = new Iterator<ModelEvent>() {
				Iterator<ModelEvent> wraped = events.descendingIterator();

				@Override
				public void remove() {
					throw new RuntimeException("Bugger off ");

				}

				@Override
				public ModelEvent next() {
					ModelEvent me = wraped.next();
					// System.out.println("WrappedNext:"+me);
					// StackTraceElement[] st=Thread.getAllStackTraces().get(Thread.currentThread());
					// System.out.println(Arrays.toString(st));
					return me;
				}

				@Override
				public boolean hasNext() {
					// TODO Auto-generated method stub
					return wraped.hasNext();
				}
			};
			eventIterator = events.descendingIterator();
			Iterator<Model> modelIterator = models.descendingIterator();

			ModelEvent startEvent = eventIterator.next();
			while (!(startEvent instanceof SelectionEndTimeEvent || startEvent instanceof SelectionTimeConditionEvent) && eventIterator.hasNext()) {
				startEvent = eventIterator.next();
			}
			// System.err.println("StartEvent:"+startEvent);
			// FIXME No Warning here. Incorrect cmd lines my cycle forever...........
			// if(!(startEvent instanceof SelectionEndTimeEvent)){
			// throw new RuntimeException("No Selection \"end\"!");
			// }
			// get to the right model... ie the first model with start time < event time.
			Model model = modelIterator.next();
			while (model.getStartTime() > startEvent.getEventTime() && modelIterator.hasNext()) {
				model = modelIterator.next();
			}
			if (startEvent instanceof SelectionEndTimeEvent && modelIterator.hasNext() && model.getStartTime() >= startEvent.getEventTime()) {// note the =
				model = modelIterator.next();
			}
			// System.err.println("FirstModel:"+model+"\t"+startEvent+"\t"+model.getStartTime());
			// note that we have the model at and *above* the SelectionTimeConditionEvent

			// now we go down the models as required with "boundry" events and the posiblity of intermediate
			// events --however intermediate events do not require boundry checks.

			double lastSweepTime = -1;

			FrequencyState fstate = new FrequencyState(this.getMaxDemeCount(), model.getDemeCount());
			// so the current "start" event
			// System.err.println("ModelH!");
			model.initSelectionData();
			// System.err.println("SSim:"+model);
			if (startEvent instanceof SelectionEndTimeEvent) {
				FrequencyState init = ((SelectionEndTimeEvent) startEvent).getInitalFrequencys();
				model.getSelectionData().setFrequencyToEnd(init);
			}
			List<ModelEvent> newEvents = model.getSelectionData().runSelectionSimulation();
			// System.err.println("Events? "+newEvents);
			if (newEvents == null) {
				//System.err.println("First continue!");
				continue;// restart simulations from the start!
			}
			lastSweepTime = model.getSelectionData().getSweepTime();

			while (modelIterator.hasNext()) {
				// in fact first we see if we should have stoped.
				if (containtsForwardSimStopEvent(newEvents)) {
					break;
				}
				// first we find the next model
				Model nextModel = modelIterator.next();
				nextModel.initSelectionData();
				// now we move the events to the relevent boundry... note we don't do anything with
				// events that are intermediate in a model. This is because these events are for the
				// coalsescent part... you can't "copy" state within a model pass... so events that
				// need to do this must already be processed.
				// System.out.println("SKIP EVENTS:"+startEvent+"\tMSTART TIME"+nextModel.getStartTime());
				while (startEvent.getEventTime() != nextModel.getEndTime() && eventIterator.hasNext()) {
					startEvent = eventIterator.next();
					if (startEvent instanceof SelectionStartEvent)
						break;// don't need to break to a label, since the remaining whiles will fall through to the instanceof test again
				}
				// now apply events. but should make sure they are not "stop" events
				model.getSelectionData().getFrequencysFromStart(fstate);
				// System.out.println("START EVENTS:"+startEvent+"\t"+nextModel.getStartTime());
				while (startEvent.getEventTime() == nextModel.getEndTime() && !(startEvent instanceof SelectionStartEvent)) {
					startEvent.processEventSelection(model.getSelectionData(), nextModel.getSelectionData(), fstate);
					if (eventIterator.hasNext()) {
						startEvent = eventIterator.next();
					} else {
						break;
					}
				}
				// we need to consider the case where there are a bunch models between here and this "startEvent"
				if (startEvent instanceof SelectionStartEvent && startEvent.getEventTime() >= nextModel.getEndTime()) {
					break;
				}
				// we are gold... we simulate the next model.
				// System.out.println("SSim:"+nextModel);
				SelectionData data = nextModel.getSelectionData();
				data.setFrequencyToEnd(fstate);
				List<ModelEvent> reallyNewEvents = data.runSelectionSimulation();
				if (reallyNewEvents == null) {
					//System.err.println("2nd continue!");
					restartFlag=true;
					break;// restart from the start
				}
				newEvents.addAll(reallyNewEvents);
				lastSweepTime = data.getSweepTime();
				model = nextModel;
			}
			if(restartFlag){
				continue;
			}
			// System.out.println("newand Old:"+newEvents+"\n\t"+events);
			events.addAll(newEvents);
			Collections.sort(events);
			// System.out.println(lastSweepTime);
			return lastSweepTime;
		}
	}

	private boolean containtsForwardSimStopEvent(List<ModelEvent> newEvents) {
		for (ModelEvent me : newEvents) {
			if (me instanceof SelectionStartEvent)
				return true;
		}
		return false;
	}

	/**
	 * this gives access to the events and models...
	 * 
	 * This permits internal management of events that occur in the middle of a valid model. Such events are dynamic in that they occur at different times due
	 * to zeros in the selection simulation.
	 * 
	 * Backward is in the sense of time. ie pastward.
	 * 
	 * @author bob
	 * 
	 */
	/*
	 * don't forget the case where there are no events between the models
	 */
	public class BackwardIterator {
		private ListIterator<ModelEvent> eventIterator;
		private ListIterator<Model> modelIterator;

		private ModelEvent currentEvent;
		private Model currentModel;

		private BackwardIterator() {
			// System.err.println("BackIteratorEvents:+" + events);
			eventIterator = events.listIterator();
			if (eventIterator.hasNext())
				currentEvent = eventIterator.next();
			modelIterator = models.listIterator();
			currentModel = modelIterator.next();
			// System.out.println(models);
			// System.out.println(events);
		}

		public Model getCurrentModel() {
			return currentModel;
		}

		public long getNextEventTime() {
			if (currentEvent != null)
				return Math.min(currentEvent.getEventTime(), currentModel.getEndTime());
			return currentModel.getEndTime();
		}

		/**
		 * moves to the next model via processing the needed events.
		 * 
		 * The state must be at the current next Event Time...
		 * 
		 * @param state
		 */
		public void nextModel(LineageState state) {
			state.setCurrentTime(getNextEventTime());
			double time = getNextEventTime();

			if (time == Long.MAX_VALUE) {
				// assert false:state.getLinagesActive();
				throw new RuntimeException("Model does not permit full coalescent of linages.\n Check for zero migration rates or for exp population growth pastward");
			}
			// we have severl posible states.
			// -1 we have important events at time zero.... ie newSamples
			// 1 the current event is before the model end.. we increment events but not a model
			// 2 the current events == the the model end. we increment events and the model
			// 3 the current events are after the model. We just incremet the model

			// this covers the 2 cases where the events need to be increment
			// System.out.println("PreCurrentEventLoop:"+currentEvent);
			while (currentEvent != null && currentEvent.getEventTime() == time) {
				// System.out.println("EventsMoveLoop:"+currentEvent);
				currentEvent.processEventCoalecent(state);
				if (eventIterator.hasNext()) {
					currentEvent = eventIterator.next();
				} else {
					currentEvent = null;
				}
			}
			//
			if ((time >= currentModel.getEndTime() || currentEvent == null) && modelIterator.hasNext() && time > 0) {
				currentModel = modelIterator.next();
			}
			state.setCurrentMaxTime(getNextEventTime());
			state.setSelectionData(currentModel.getSelectionData());
			// System.out.println("SelectionData? "+currentModel.getSelectionData()+"\t"+state.getSelectionData());
		}

		public void finishSelectionEvents(LineageState state) {
			if (currentEvent != null) {
				currentEvent.processEventCoalecent(state);
			}
			while (eventIterator.hasNext()) {
				currentEvent = eventIterator.next();
				currentEvent.processEventCoalecent(state);
			}
			currentEvent = null;
			currentModel = null;// make sure we don't call it again.
			modelIterator = null;
			eventIterator = null;
		}
	}

	/**
	 * we relax the original assumptions and try and keep the code cleaner.
	 * 
	 * In particular we relax that model boundaries and events times must coincide. ie events can happen in the middle of a model. And these can be selection
	 * events.
	 * 
	 * @author greg
	 * 
	 */
	private class newForwardIterator {
		private Model currentModel;
		private ModelEvent currentEvent;
		private Iterator<ModelEvent> eventIterator = events.descendingIterator();
		private Iterator<Model> modelIterator = models.descendingIterator();
		private FrequencyState state;

		public newForwardIterator() {
			state = new FrequencyState(models.getLast().getDemeCount(), models.getLast().getDemeCount());

			// find the first "selection start time event".
			// however we may not have a "start event" iff we are conditioning on end time. ie that gets
			// created with the forward simulator.

			while (eventIterator.hasNext()) {
				currentEvent = eventIterator.next();
				if (currentEvent instanceof SelectionEndTimeEvent) {
					moveToModel(currentEvent.getEventTime());
					return;// we are done!
				}
				if (currentEvent instanceof SelectionTimeConditionEvent) {// the "fixation at time 0" event
					moveToModel(currentEvent.getEventTime());
					return;
				}
			}
			assert false;

		}

		private void moveToModel(long time) {
			while (currentModel.getStartTime() > time && modelIterator.hasNext()) {
				currentModel = modelIterator.next();
			}
		}
	}

	/*
	 * FIXME this class provides a similar function to the backwards iterator. the assumption will be cut and pasted here from the selection simulation block.
	 * That is the only thing that should use this.
	 * 
	 * the current model is always the model *above* the currentEvent. In otherwords the first event to get to the "next" model is the current Event.
	 * 
	 * we seek to the SelectionEndTime Event and throw exceptions if we Find a FixationTimeEvent or no selectionEvent...Note that the current model is the model
	 * ABOVE the SelectionEndTimeEvent
	 * 
	 * @author bob
	 */
	private class ForwardIterator {
		private Model currentModel;
		private ModelEvent currentEvent;
		private Iterator<ModelEvent> eventIterator = events.descendingIterator();
		private Iterator<Model> modelIterator = models.descendingIterator();
		private FrequencyState state;

		private ForwardIterator() {
			// for (Model mm : models) {
			// System.out.println("Models:" + Arrays.toString(mm.getTotalMigrationRates()) + "\t" + mm.getStartTime() + "\t" + mm.getEndTime());
			// }
			// for (ModelEvent me : events) {
			// System.out.println("Events:" + me);
			// }
			eventIterator = events.descendingIterator();
			modelIterator = models.descendingIterator();
			state = new FrequencyState(models.getLast().getDemeCount(), models.getLast().getDemeCount());// should
																											// //
																											// zero
			// so now we just need to find the correct place.
			while (eventIterator.hasNext()) {
				currentEvent = eventIterator.next();
				if (currentEvent instanceof SelectionTimeConditionEvent) {
					// should never happen
					throw new RuntimeException("Fixation Time Error. Perhaps your model is not time invarant.");
				}
				if (currentEvent instanceof SelectionEndTimeEvent) {
					break;
				}
			}
			if (!(currentEvent instanceof SelectionEndTimeEvent)) {
				throw new RuntimeException("Selection must have a start or end time");
			}

			// we are now at the right place...
			// now to get the model to the correct place.
			currentModel = modelIterator.next();
			while (currentModel.getStartTime() != currentEvent.getEventTime() && modelIterator.hasNext()) {
				currentModel = modelIterator.next();
			}
			// System.err.println("Forward Iterator!");
			currentModel.initSelectionData();
		}

		/*
		 * this is always true...even for models with no events between them...
		 */
		// public double getNextEventTime() {
		// return currentModel.getStartTime();
		// }

		/*
		 * Guts of the class. move to the next model --by applying the events between
		 */
		// public void moveToNextModel() {
		// currentModel.getSelectionData().getFrequencysFromStart(state);
		// // so first we must note that the next model may need no events between.
		// // we know there is a next model
		// Model next =modelIterator.next();
		// //next.initSelectionData();// just cheaking
		// if (currentEvent == null || currentEvent.getEventTime() < currentModel.getStartTime()) {
		// next.getSelectionData().setFrequencyToEnd(state);
		// currentModel =next;
		// return;
		// }
		// while (currentEvent != null && currentEvent.getEventTime() == currentModel.getStartTime()) {
		// currentEvent.processEventSelection(currentModel.getSelectionData(), next.getSelectionData(), state);
		// if (eventIterator.hasNext()) {
		// currentEvent =eventIterator.next();
		// } else {
		// currentEvent =null;
		// }
		// }
		// // now apply the state.
		// next.getSelectionData().setFrequencyToEnd(state);
		// currentModel =next;
		// }

		public boolean hasNextModel() {
			return modelIterator.hasNext();
		}

		public Model getCurrentModel() {
			return currentModel;
		}

		public ModelEvent getCurrentEvent() {
			return currentEvent;
		}
	}

	public SampleConfiguration getSampleConfiguration() {
		return sampleConfig;
	}

	public void setSampleConfiguration(SampleConfiguration sampleConfiguration) {
		this.sampleConfig = sampleConfiguration;
	}

	public double[] getLociConfiguration() {
		return lociConfiguration;
	}

	public void setLociConfiguration(double[] lociConfiguration) {
		this.lociConfiguration = lociConfiguration;
	}

	public double getSegSiteCount() {
		return segSiteCount;
	}

	public void setSegSiteCount(double segSiteCount) {
		this.segSiteCount = segSiteCount;
	}

	public FrequencyCondition getTimedCondition() {
		return timedCondition;
	}

	public void setTimedCondition(FrequencyCondition timedCondition) {
		this.timedCondition = timedCondition;
		// System.out.println("TimedCondtions:"+this.timedCondition);
	}

	public SelectionSimulator getSelectionSimulator() {
		return selectionSimulator;
	}

	public void setSelectionSimulator(SelectionSimulator selectionSimulator) {
		this.selectionSimulator = selectionSimulator;
	}

	public ForwardStatsCollector getForwardTraceOutput() {
		return forwardTraceOutput;
	}

	public void setForwardTraceOutput(ForwardStatsCollector forwardTraceOutput) {
		this.forwardTraceOutput = forwardTraceOutput;
	}

	public void setSelection(boolean selection) {
		this.selection = selection;
	}

	public RestartCondition getRestartCondtion() {
		return restartCondtion;
	}

	public void setRestartCondtion(RestartCondition restartCondtion) {
		this.restartCondtion = restartCondtion;
	}

	public void setFoldMutations(boolean foldMutations) {
		this.foldMutations = foldMutations;
	}

	public boolean isFoldMutations() {
		return foldMutations;
	}

	public boolean isUnphase() {

		return unphase;
	}

	public void setUnphase(boolean unphase) {
		this.unphase = unphase;
	}

	public boolean isWeightedMutations() {
		return weightedMutations;
	}

	public void setWeightedMutations(boolean weightedMutations) {
		this.weightedMutations = weightedMutations;
	}

	public int getSeedDeme() {

		return seedDeme;
	}

	public void setSeedDeme(int seedDeme) {
		this.seedDeme = seedDeme;
	}
}
