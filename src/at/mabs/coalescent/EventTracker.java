package at.mabs.coalescent;
import java.util.*;

import at.mabs.segment.SegmentEventRecoder;
import at.mabs.stats.StringStatsCollector;
/**
 * log coalescent events for fun. For now just recombination events!
 * @author bob
 *
 */
public class EventTracker implements StringStatsCollector {
	private List<Double> data=new ArrayList<Double>();
	
	
	public EventTracker() {
		//System.out.println("Creating Event Tracker:"+this);
	}
	
	
	
	public void init(){
		//data.clear();
	}
	public void recombinationEvent(double time,double p){
		//System.err.println("event:"+this);
		data.add(time);
		data.add(p);
	}
	
	
	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		double gs=recorder.getGenerationScale();
		builder.append("recombinationEventTimes: ");
		for(int i=0;i<data.size();i+=2){
			builder.append(data.get(i)*gs+" ");
		}
		builder.append("\nrecombinationPositions: ");
		for(int i=1;i<data.size();i+=2){
			builder.append(data.get(i)+" ");
		}
		builder.append("\n");
	}
	
	@Override
	public void summary(StringBuilder builder) {
		//no op.
	}

}
