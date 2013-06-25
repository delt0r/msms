package at.mabs.stats;

import java.util.Arrays;
import java.util.Random;

import at.mabs.config.CommandLineMarshal;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.random.RandomGenerator;

public class GaussNLStat extends StatsCollectorAdapter {
	private double[] parameters;
	private Random random = RandomGenerator.getRandom();

	public GaussNLStat() {

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
		//rnorms[rnorms.length-1]=random.nextGaussian()*.01;

		double[] results = new double[parameters.length/2];

		switch (results.length) {
		case 10:
			results[9]=rnorms[9]*rnorms[8];
		case 9:
			results[8]=rnorms[8];
		case 8:
			results[7]=rnorms[6]+rnorms[7];
		case 7:
			results[6]=rnorms[6];
		case 6:
			results[5]=rnorms[3]+rnorms[5];
		case 5:
			results[4]=rnorms[5]+rnorms[4];
		case 4:
			results[3]=rnorms[3]+rnorms[4];
		case 3:
			results[2]=rnorms[1]-rnorms[2];
		case 2://degen case! 
			results[1]=rnorms[1]+rnorms[2];
		case 1:
			results[0]=rnorms[0];
		}
//		for(int i=10;i<results.length;i++){
//			results[i]=rnorms[i]*rnorms[i-1];
//		}
		return results;
	}
}
