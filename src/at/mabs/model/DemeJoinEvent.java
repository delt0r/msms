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

import java.util.Arrays;

import at.mabs.coalescent.LineageState;
import at.mabs.model.selection.SelectionData;
import at.mabs.util.random.RandomGenerator;

/**
 * Implements the -ej switch in ms. ie we join i to j.
 * 
 * I want to delete i but thats not how ms works... its still a real
 * population... This does not affect coalescent simulations since it is
 * unobservable... But this is not the case with selection. If they set finite
 * migration or something after the join... Its pretty weird really (the way ms
 * does i mean).
 * 
 * for now we assume its deleting.. hence its population size it set to zero,
 * and it is ignored by the forward simulator...
 * 
 * @author bob
 * 
 */

public class DemeJoinEvent extends ModelEvent {
	private final int i, j;

	public DemeJoinEvent(long t, int i, int j) {
		super(t);
		this.i = i;
		this.j = j;
	}

	@Override
	public void modifiyModel(Model model) {
		model.clearDemeBySizeByMigration(i);
	}

	@Override
	public void processEventCoalecent(LineageState state) {
		// this is pretty easy. move everthing from i to j..
		//System.err.println("JOIN that Loser");
		int n = state.getLineageSize(i, 0);
		for (int c = 0; c < n; c++)
			state.migrationEvent(i, j, 0, 0.0);

		if (!state.isSelection())
			return;
		
		n = state.getLineageSize(i, 1);
		for (int c = 0; c < n; c++)
			state.migrationEvent(i, j, 1, 0.0);

	}

	/**
	 * we simply resample for the 2 new population sizes. Nothing fancy.
	 */
	@Override
	protected void processEventSelection(SelectionData oldData, SelectionData currentData, FrequencyState state) {
		double f = state.getFrequency(j, 1);// assume biallelic
		double changeTime = eventTime;
		//System.out.println("PROCESSING EJ\t"+currentData.getParent().getPopulationSizeModels().length+"\t"+i+"\t"+j);
		//System.out.println("PROCESSING EJ :"+Arrays.toString(currentData.getParent().getPopulationSizeModels()));
		//System.out.println("PROCESSING EJ Old:"+Arrays.toString(oldData.getParent().getPopulationSizeModels()));
		///System.exit(-1);
		int Ni = (int) currentData.getParent().getPopulationSizeModels()[i].populationSize(changeTime);
		int Nj = (int) currentData.getParent().getPopulationSizeModels()[j].populationSize(changeTime);

		double fi = (double) RandomGenerator.getBinomial().generateBinomial(Ni, f) / Ni;
		double fj = (double) RandomGenerator.getBinomial().generateBinomial(Nj, f) / Nj;
		//System.out.println("EJ selection stuff:"+fi+"\t"+fj);
		state.setFrequency(i, 1, fi);
		state.setFrequency(i, 0, 1 - fi);

		state.setFrequency(j, 1, fj); 
		state.setFrequency(j, 0, 1 - fj);

		// state.setCurrentDemeCount(state.getCurrentDemeCount()+1);

	}

}
