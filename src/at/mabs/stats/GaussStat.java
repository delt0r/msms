package at.mabs.stats;

import java.util.Arrays;
import java.util.Random;

import at.mabs.config.CommandLineMarshal;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.random.RandomGenerator;

public class GaussStat extends StatsCollectorAdapter {
	private double[] parameters;
	private Random random = RandomGenerator.getRandom();

	public GaussStat() {

	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {

		String[] params = CommandLineMarshal.HACK_PARAMS;

		parameters = new double[params.length];
		for (int i = 0; i < params.length; i++) {
			parameters[i] = Double.parseDouble(params[i]);
		}

		double[] rnorms = new double[parameters.length / 2];//extra for the degen case

		for (int i = 0; i < parameters.length/2; i++) {
			rnorms[i] = parameters[i * 2] + random.nextGaussian() * parameters[i * 2 + 1];
		}
		
		return rnorms;
	}
}
