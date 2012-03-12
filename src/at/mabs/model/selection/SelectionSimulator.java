package at.mabs.model.selection;

import java.util.List;

import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;
/**
 * MUST BE THREAD SAFE. Easiest way to do this is leaving anything with state on the stack. 
 * @author bob
 *
 */
public interface SelectionSimulator {
	public List<ModelEvent> init(ModelHistroy modelHistory);//called once per "full simualtion" not per model!
	/**
	 * The general contract is that the same method is called for every "valid" section of the
	 * simulation.
	 * 
	 * The frequency trace is initalized as an open interval or closed interval as required. The
	 * method should return the required startSelection/endSelection modelEvents. Otherwise
	 * selection will not be turned on for the coalescnet pass.
	 * 
	 * 
	 * @param model
	 * @param modelHistory
	 * @param ssm
	 * @param trace note that it has been reset and setIteratorToEnd()
	 * @return modelEvents created via the simulation. Note these model events cannot create a full 
	 * model split. ie change the deme count for example.
	 */
	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace trace);
}
