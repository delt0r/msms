package at.mabs.simulators;

import java.util.ArrayList;
import java.util.List;

import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;
import at.mabs.model.selection.FrequencyTrace;
import at.mabs.model.selection.SelectionSimulator;
import at.mabs.model.selection.SelectionStartEvent;
import at.mabs.model.selection.SelectionStrengthModel;
import at.mabs.model.selection.SuperFrequencyTrace;

/**
 * assumes fixed interval simulation. Otherwise we barf.
 * @author bob
 *
 */
public class SinwaveSelection implements SelectionSimulator {
	private final double frequency;
	private final double shift;
	private final double amp;
	private final double phase;
	
	public SinwaveSelection(double amp,double period,double phase,double shift) {
		frequency=Math.PI*2/period;
		this.shift=shift;
		this.phase=phase;
		this.amp=amp;
	}
	
	@Override
	public List<ModelEvent> init(ModelHistroy modelHistory) {
		return new ArrayList<ModelEvent>();
	}
	
	
	@Override
	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace frequencys) {
		//if(model.getEndTime()==Long.MAX_VALUE){
//			throw new RuntimeException("SinewaveSelection must be used with a closed selection interval. use the -SI option.");
//		}
//		
//		do {
//			double[][] freq=frequencys.previous(false);
//			double t=frequencys.getTime();
//			double f=amp*.5*Math.sin(frequency*t+phase)+.5+shift;
//			f=Math.max(0, f);//clamp
//			f=Math.min(1, f);
//			for(int i=0;i<model.getDemeCount();i++){
//				freq[i][1]=f;
//				freq[i][0]=1-f;
//				//System.out.println(f);
//				
//			}
//			
//		}while (frequencys.hasPrevious());
//		
//		
//		double[] freq =new double[model.getDemeCount()];
//		double[][] local=frequencys.current();
//		for (int i =0; i < freq.length; i++) {
//			freq[i] =local[i][1];
//		}
//		List<ModelEvent> events=new ArrayList<ModelEvent>();
//		events.add(new SelectionStartEvent(frequencys.getStartTime(), freq));
		
		return null;
	}

}
