package at.mabs.simulators;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;

import at.mabs.model.FrequencyState;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;
import at.mabs.model.selection.FrequencyTrace;

import at.mabs.model.selection.SelectionEndTimeEvent;
import at.mabs.model.selection.SelectionSimulator;
import at.mabs.model.selection.SelectionStartEvent;
import at.mabs.model.selection.SelectionStrengthModel;
import at.mabs.model.selection.SuperFrequencyTrace;

/**
 * this class handles all the reading from a reader and marshaling frequencys to
 * traces.
 * 
 * FIXME we have a +1 thing going on.
 * 
 * 
 * 
 * 
 * @author greg
 * 
 */
public class ReaderSelection implements SelectionSimulator {
	private final BufferedReader reader;
	private List<FrequencyEntry> data = new ArrayList<FrequencyEntry>();

	private int currentIndex;
	private FrequencyEntry previousLine;

	public ReaderSelection(BufferedReader inputData) {
		reader = inputData;
		String line;
		try {
			line = inputData.readLine();
			// System.out.println("initReadLine:"+line);
			while (line != null && line.trim().length() == 0)
				line = inputData.readLine();
		} catch (IOException e) {
			throw new RuntimeException("Can't read any data for frequency trace!", e);
		}
		previousLine = new FrequencyEntry(line);
	}

	@Override
	public List<ModelEvent> init(ModelHistroy modelHistory) {
		try {
			readData();
		} catch (IOException e) {
			throw new RuntimeException("could not read trace data:", e);
		}

		// add the start and end times of the simulation.
		// kinda need to know the deme count at the end time
		// and the start time. //Here we assume a bi allele state so we have an
		// implied deme count...

		List<ModelEvent> events = new ArrayList<ModelEvent>();

		FrequencyEntry lastPastward = data.get(0);
		FrequencyEntry firstPastward = data.get(data.size() - 1);

		double genScale = modelHistory.getN() * 4;
		// System.out.println("StartStop:"+firstPastward.time*genScale+"\t"+lastPastward.time*genScale);
		double[] freqs=new double[firstPastward.frequencys.length/2];
		for(int i=1;i<firstPastward.frequencys.length;i+=2){
			freqs[i/2]=firstPastward.frequencys[i];
		}
		SelectionStartEvent sse = new SelectionStartEvent((long) ((firstPastward.time) * genScale), freqs);

		double[] fs = lastPastward.frequencys;
		// System.out.println("LAST:"+lastPastward.time+"\t"+firstPastward.time);
		FrequencyState state = new FrequencyState(fs.length, fs.length);
		boolean fixation=true;
		for (int i = 0; i < fs.length; i++) {
			state.setFrequency(i, 1, fs[i]);
			state.setFrequency(i, 0, 1 - fs[i]);
			if(fs[i]>0 && fs[i]<1){
				fixation=false;
			}
		}
		SelectionEndTimeEvent see = new SelectionEndTimeEvent((long) (lastPastward.time * genScale + 1), state,true);
		
		events.add(sse);
		events.add(see);
		//System.out.println(events);
		return events;
	}

	@Override
	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace frequencys) {
		// note that frequencys only need to be defined between the first and
		// last specified times.
		FrequencyEntry lastPastward = data.get(0);
		FrequencyEntry firstPastward = data.get(data.size() - 1);
		double N = modelHistory.getN();
		long startForward = Math.max(Math.round(lastPastward.time * 4 * N) + 1, model.getStartTime());
		long endForward = Math.min(Math.round(firstPastward.time * 4 * N), model.getEndTime());

		// frequencys.shiftAndSet((int)startForward, (int)endForward);
		
		frequencys.setIndexMostPastward();
		// System.out.println("StartTime:"+frequencys.getStartTime()+"\t"+frequencys.getEndTime());
		double[] data = frequencys.getFrequencys(null);
		//System.out.println(frequencys);
		boolean resize=model.getEndTime()==Long.MAX_VALUE;
		double lastTime=startForward/(4*N);
		for(long i=startForward;i>endForward;i--){
			double realTime = (double) i / (4.0 * N);
			//System.out.println("Real:"+realTime+"\t"+"\t"+frequencys.getIndexTime()+"\t"+i);

			putFrequency(realTime, data);
			frequencys.setFrequencys(data);
			frequencys.moveForward();
			lastTime=realTime;
		}
		double realTime = frequencys.getIndexTime() / (4.0 * N);
		// System.out.println("Real+:"+realTime+"\t"+frequencys.getTime()+"\t"+N);
		putFrequency(realTime, data);
		frequencys.setFrequencys(data);
		frequencys.setCurrentIndexAsStart();
		
		return new ArrayList<ModelEvent>();
		
		// return null;
	}

	
	private void putFrequency(double time,double[] array){
		//first we check the current Index is in a valid range. 
		if(currentIndex>=data.size() || currentIndex<0){
			currentIndex=0;
		}
		//now lets find the closest index. 
		FrequencyEntry e=data.get(currentIndex);
		while(e.time>time && currentIndex<(data.size()-1)){
			currentIndex++;
			e=data.get(currentIndex);
		}
		FrequencyEntry next=data.get(currentIndex);
		//could be where we want. need to go forwards too.
		while(next.time<time && currentIndex>1){
			currentIndex--;
			next=data.get(currentIndex);
		}
		if(currentIndex-1>=0)
			next=data.get(currentIndex-1);
		double t = (e.time - time) / (e.time - next.time);
		if (Double.isInfinite(t) || Double.isNaN(t) || t < 0) {
			t = 0;
			// System.out.println("NaN||Inf");
		}
		//System.out.println("Index:"+currentIndex+"\t"+t+"\t"+time+"\ttime:"+e.time+"\tnext:"+next.time);
		int demeCount = array.length;

		int index = 1;
		double[] entryData = e.frequencys;
		double[] nextEntryData = next.frequencys;
		for (int d = 0; d < demeCount; d++) {
			double total = 0;
			// for(int a=1;a<alleleCount;a++){
			double f = entryData[index] * (1 - t) + nextEntryData[index] * t;
			index++;
			total += f;
			array[d] = f;
			// }
			// array[d][0]=1-total;
			// System.out.println(Arrays.toString(array[d]));
		}
	}
	
	

	// private void put(FrequencyEntry e, double[][] array){
	// //FrequencyEntry contains a entry for each non wild type allele for each
	// deme. hence we only can tell once we know how many demes!
	// int demeCount=array.length;
	// int alleleCount=array[0].length;
	// int index=0;
	// double[] entryData=e.frequencys;
	// for(int d=0;d<demeCount;d++){
	// double total=0;
	// for(int a=1;a<alleleCount;a++){
	// double f=entryData[index++];
	// total+=f;
	// array[d][a]=f;
	// }
	// array[d][0]=1-total;
	// }
	// }

	private void readData() throws IOException {
		data.clear();
		// we read till one time is larger than the previous line.
		String line = reader.readLine();
		while (line != null && line.trim().length() == 0)
			line = reader.readLine();
		if (line == null) {
			throw new EOFException("Wanted new data, but @ end of stream");
		}
		FrequencyEntry newEntry = new FrequencyEntry(line);
		while (newEntry.time < previousLine.time) {
			if (!Double.isInfinite(previousLine.time))
				data.add(previousLine);
			// System.out.println("Adding:"+previousLine);
			previousLine = newEntry;
			line = reader.readLine();
			while (line != null && line.trim().length() == 0)
				line = reader.readLine();
			if (line == null)
				break;
			newEntry = new FrequencyEntry(line);
		}
		if (!Double.isInfinite(previousLine.time))
			data.add(previousLine);
		// System.out.println("Adding:"+previousLine);
		previousLine = newEntry;
		currentIndex = 0;
		// System.out.println(data);
	}

	private class FrequencyEntry {
		// time Inf signals the end
		double time;// we don't have N0 till we do the forward thing

		double[] frequencys;

		public FrequencyEntry(String line) {
			if (line.startsWith("//")) {
				time = Double.POSITIVE_INFINITY;
				return;
			}
			// System.out.println("ReadingLine:"+line);
			StringTokenizer tokens = new StringTokenizer(line);
			int tcount = tokens.countTokens();
			time = Double.parseDouble(tokens.nextToken());
			frequencys = new double[tcount - 1];
			for (int i = 0; i < frequencys.length; i++) {
				frequencys[i] = Double.parseDouble(tokens.nextToken());
			}
		}

		@Override
		public String toString() {

			return "FreqEntry:" + time + "[" + frequencys[0] + "]";
		}
	}
}
