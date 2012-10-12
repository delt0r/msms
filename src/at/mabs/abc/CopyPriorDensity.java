package at.mabs.abc;
/**
 * just grabs state from a previously definded density...
 * @author greg
 *
 */
public class CopyPriorDensity extends PriorDensity {
	private int argIndex=-1;
	private PriorDensity pd;
	public CopyPriorDensity(PriorDensity pd,int argIndex) {
		this.argIndex=argIndex;
		this.pd=pd;
	}
	@Override
	public void generateRandom() {
		//noop
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
	public double getTransformedValue() {
		return pd.getTransformedValue();
	}
	
	
}
