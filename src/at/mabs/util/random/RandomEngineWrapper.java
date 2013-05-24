package at.mabs.util.random;

import cern.jet.random.engine.RandomEngine;
import java.util.*;
/**
 * Wrap a java.util.Random for the colt libs. Kinda dumb that they didn't just use that really. 
 * <p>
 * Technically we violate the contract because we return something more random with raw() etc.
 * @author bob
 *
 */
public class RandomEngineWrapper extends RandomEngine {
	private final Random random;
	
	public RandomEngineWrapper(Random rand) {
		this.random=rand;
	}
	
	@Override
	public int nextInt() {
		return random.nextInt();
	}
	
	@Override
	public double nextDouble() {
		return random.nextDouble();
	}

	@Override
	public float nextFloat() {
		return random.nextFloat();
	}
	
	@Override
	public long nextLong() {
		return random.nextLong();
	}
	
	@Override
	public double raw() {
		return random.nextDouble();
	}
	
	@Override
	public double apply(double arg0) {
	
		return random.nextDouble();
	}
	
	@Override
	public int apply(int arg0) {
	
		return random.nextInt();
	}
}
