package at.mabs.abc;

/**
 * just grabs state from a previously definded density...
 * 
 * @author greg
 * 
 */
public class CopyPriorDensity extends PriorDensity {
	private int argIndex = -1;
	private PriorDensity pd;

	public CopyPriorDensity(PriorDensity pd, int argIndex) {
		this.argIndex = argIndex;
		this.pd = pd;
	}

	@Override
	public void generateRandom() {
		//throw new RuntimeException("Copy does not create randomness!");
	}

	@Override
	public double nextProp(double v) {
		throw new RuntimeException("Copy does not create randomness!");
	}

	@Override
	public int getArgIndex() {
		return argIndex;
	}

	@Override
	public double getValue() {
		return pd.getValue();
	}

	
	

	@Override
	public boolean isValid() {
		return pd.isValid();
	}

	@Override
	public boolean isInteger() {

		return pd.isInteger();
	}

	@Override
	public double getValueUI() {
		return pd.getValueUI();
	}

	@Override
	public void setValueUI(double uiValue) {
		// noop
	}
	
	@Override
	public void setValue(double lastValue) {
		//noop
	}
	
	

}
