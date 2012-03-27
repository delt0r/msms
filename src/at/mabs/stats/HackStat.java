package at.mabs.stats;

import java.util.Arrays;
import java.util.Random;

import at.mabs.config.CommandLineMarshal;
import at.mabs.segment.SegmentEventRecoder;
import at.mabs.util.random.RandomGenerator;

public class HackStat extends StatsCollectorAdapter {
	private double[] parameters;
	private Random random = RandomGenerator.getRandom();

	public HackStat() {

	}

	@Override
	public double[] collectStatsImp(SegmentEventRecoder recorder) {

		String[] params = CommandLineMarshal.HACK_PARAMS;
		//System.out.println("HPARAMString:" + Arrays.toString(params));
		parameters = new double[params.length];
		for (int i = 0; i < params.length; i++) {
			parameters[i] = Double.parseDouble(params[i]);
		}

		double[] result = new double[parameters.length / 2];
		//System.out.println("HPARAM:" + Arrays.toString(parameters));
		for (int i = 0; i < result.length; i++) {
			result[i] = parameters[i * 2] + random.nextGaussian() * parameters[i * 2 + 1];
		}
		//System.out.println("R:" + Arrays.toString(result));

		for(int i=result.length-1;i>=0;i--){
			int j=1;
			if(i!=0)
				j=1;
			result[i]=((result[i]*result[(i+j)%result.length])+10000);
		}
		
		return result;
	}

}
