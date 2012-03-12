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
	public double next() {
		//throw new RuntimeException("Copy does not create randomness!");
		return pd.getLastValue();//should always copy something that was before this in the list FIXME
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
	public double getLastValue() {
		return pd.getLastValue();
	}
	
	
}
