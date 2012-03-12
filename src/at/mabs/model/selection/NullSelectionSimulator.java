package at.mabs.model.selection;

import java.util.ArrayList;
import java.util.List;

import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;

public class NullSelectionSimulator implements SelectionSimulator {

	public NullSelectionSimulator() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public List<ModelEvent> init(ModelHistroy modelHistory) {
		
		return new ArrayList<ModelEvent>();
	}

	@Override
	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace trace) {
		// TODO Auto-generated method stub
		return new ArrayList<ModelEvent>();
	}

}
