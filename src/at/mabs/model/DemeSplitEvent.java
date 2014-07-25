/*
This code is licensed under the LGPL v3 or greater with the classpath exception, 
with the following additions and exceptions.  

packages cern.* have retained the original cern copyright notices.

packages at.mabs.cmdline and at.mabs.util.* 
have the option to be licensed under a BSD(simplified) or Apache 2.0 or greater  license 
in addition to LGPL. 

Note that you have permission to replace this license text to any of the permitted licenses. 

Main text for LGPL can be found here:
http://www.opensource.org/licenses/lgpl-license.php

For BSD:
http://www.opensource.org/licenses/bsd-license.php

for Apache:
http://www.opensource.org/licenses/apache2.0.php

classpath exception:
http://www.gnu.org/software/classpath/license.html
*/
package at.mabs.model;

import java.util.Random;


import at.mabs.cern.jet.random.Binomial;
import at.mabs.coalescent.LineageState;
import at.mabs.model.selection.SelectionData;
import at.mabs.util.random.RandomGenerator;

/**
 * an implementation of the -es option in ms
 * 
 * @author bob
 * 
 */
public class DemeSplitEvent extends ModelEvent {
	private final int deme;
	private final double q, N;
	private int demeLabel;
	private Binomial binomial = RandomGenerator.getBinomial();

	public DemeSplitEvent(long t, int deme, double p, double N) {
		super(t);
		this.deme = deme;
		this.q = 1 - p;
		this.N = N;
	}

	/**
	 * should be some kind of error to call this 2x on the same model.
	 */
	@Override
	public void modifiyModel(Model model) {
		// System.out.println("DemeSplit");
		demeLabel = model.getDemeCount();
		model.addDeme(N);

	}

	/**
	 * This requires that lineage state has the right number of demes (or more)
	 * In practice this should be easy to do, since after the models in the
	 * histroy are created we can have a max deme count parameter...
	 * 
	 * but performance?
	 */
	@Override
	public void processEventCoalecent(LineageState state) {
		// we need to move linages from deme to demeLabel with
		// probablity q=1-p.
		assert demeLabel == state.getDemeCount() || demeLabel + 1 == state.getDemeCount();
		// need to do this for the rather odd case where we use this in the
		// first model. Since this model
		// will already have the "added" deme, hence the odd assert.
		if (demeLabel == state.getDemeCount())
			state.setCurrentDemeCount(state.getDemeCount() + 1);
		// System.out.println("SplitCount:"+state.getDemeCount());
		int n = state.getLineageSize(deme, 0);
		if (n > 0) {
			int m = binomial.generateBinomial(n, q);
			// System.out.println("Moving "+m+"\tlinages out of "+n );
			for (int i = 0; i < m; i++) {
				state.migrationEvent(deme, demeLabel, 0, 0.0);
				// System.out.println("State:"+state);
			}
		}
		if (!state.isSelection())
			return;

		n = state.getLineageSize(deme, 1);
		if (n > 0) {
			int m = binomial.generateBinomial(n, q);
			for (int i = 0; i < m; i++)
				state.migrationEvent(deme, demeLabel, 1, 0.0);
		}

	}

	/**
	 * we use a resampled weighted average to produce a new frequency.
	 */
	@Override
	protected void processEventSelection(SelectionData oldData, SelectionData currentData, FrequencyState state) {
		// this needs to merge the 2 different allele frequencys.
		// this is done via a weighted average--ie
		// (N_i*x_i+N_j*x_j)/(N_i+N_j) == new x
		// then resample...
		// first get the pop sizes from the old model
		//System.err.println("PROCESSING ES!");
		double changeTime = oldData.getParent().getStartTime();
		double ni = oldData.getParent().getPopulationSizeModels()[deme].populationSize(changeTime);
		double nj = oldData.getParent().getPopulationSizeModels()[demeLabel].populationSize(changeTime);

		double xi = state.getFrequency(deme, 1);// selected allele assumeing 2
												// allele only for now.
		double xj = state.getFrequency(demeLabel, 1);
		double xp = (xi * ni + xj * nj) / (ni + nj);
		// now to resample
		int N = (int) currentData.getParent().getPopulationSizeModels()[deme].populationSize(changeTime);
		double f = (double) binomial.generateBinomial(N, xp) / N;

		state.setFrequency(deme, 1, f);
		state.setFrequency(deme, 0, 1 - f);
		state.setCurrentDemeCount(demeLabel);

	}

}
