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

import java.util.*;

//import at.mabs.coalescent.CoalescentEvent;
//import at.mabs.coalescent.CoalescentEventCalculator;
import at.mabs.model.selection.SelectionData;
import at.mabs.util.Util;

/**
 * Changing this again. This time we are going for a component based approach. This model represents
 * different views of itself that make other components life easier.
 * 
 *We change this to *only* use the by deme storage method...
 * 
 *The life cycle is that this is first mutated. Then finalised. Calling any setter method after
 * that results in an error. Calling any getter method before the finalised is also an error. This
 * should perhaps implement the state pattern. A state for setable, a state for readable
 * 
 *The checks should probably use a cut point in AspectJ or something...
 * 
 * @author bob
 * 
 */
public final  class Model {
	private final ModelHistroy modelHistory;
	private final Model parent;

	private PopulationSizeModel[] sizeModels;

	// temearay storage... just for when this thing is getting mutated to hell.
	private List<Integer>[] mDirectionList;
	private List<Double>[] mRateList;
	private boolean finalized =false;

	// list by deme so deme i has no zero migration in the array mDirectionByDeme[i]
	// this is the FINALIZED storage...
	private int[][] mDirectionByDeme;
	private double[][] mRatesByDeme;
	private double[] demeTotalRate;

	private int demeCount;

	private long startTime, endTime;
	
	private SelectionData selectionData;
	
	private boolean forwardOnly;

	//private Model(ModelHistroy mh) {
	//	parent =mh;
	//}
	/**
	 * copy constructor... Deep copy. The result is *not* finalised.
	 * SelectionData is not initialised
	 * 
	 * @param model
	 */
	protected Model(Model model) {
		//System.err.println("Model creation A:"+this.hashCode()+"\t"+model);
		if (!model.finalized)
			throw new RuntimeException("Model must be finalized before its data is acessed");
		modelHistory =model.modelHistory;
		demeCount =model.demeCount;
		mDirectionList =new ArrayList[demeCount];
		mRateList =new ArrayList[demeCount];
		for (int i =0; i < demeCount; i++) {
			mDirectionList[i] =Util.asListBox(model.mDirectionByDeme[i]);
			mRateList[i] =Util.asListBox(model.mRatesByDeme[i]);
		}
		sizeModels =model.sizeModels.clone();
		forwardOnly=model.isForwardOnly();
		parent=model;
		
	}

	/**
	 * default population structure...
	 * 
	 * @param mh
	 * @param deme
	 * @param N
	 * @param m
	 */
	protected Model(ModelHistroy mh, int demes, double N, double m) {
		//System.err.println("Model creation B:"+this.hashCode());
		modelHistory =mh;

		demeCount =demes;

		sizeModels =new PopulationSizeModel[demeCount];
		PopulationSizeModel constant =new ConstantPopulation(N);
		Arrays.fill(sizeModels, constant);

		initLists();
		setMigrationMatrix(m);
		//System.out.println("Matrix:"+mRateList[0]+"\t"+demeCount);
		parent=null;
	}
	
	public ModelHistroy getModelHistory(){
		return modelHistory;
	}
	
	public Model getParent() {
		return parent;
	}
	
	boolean isFinalized(){
		return finalized;
	}

	private void initLists() {
		mDirectionList =new ArrayList[demeCount];
		mRateList =new ArrayList[demeCount];
		Util.initArray(mDirectionList, ArrayList.class);
		Util.initArray(mRateList, ArrayList.class);
	}

	
	
	public int[][] getMigrationDirectionsByDeme() {
		return mDirectionByDeme;
	}

	public double[][] getMigrationRatesByDeme() {
		return mRatesByDeme;
	}

	public double[] getTotalMigrationRates() {
		return demeTotalRate;
	}

	public PopulationSizeModel[] getPopulationSizeModels() {
		return sizeModels;
	}

	public int getDemeCount() {
		return demeCount;
	}

	public double getRecombinationRate() {
		return modelHistory.getRecombinationRate();
	}

	protected void setPopulationModel(int deme, PopulationSizeModel popModel) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		if (deme >= sizeModels.length)
			throw new RuntimeException("deme label larger than the number of demes:" + deme);
		sizeModels[deme] =popModel;
	}

	/**
	 * to get around knowing the size at the needed time... Not the best... but it works for now...
	 * 
	 */
	protected void setExpGrowth(int deme, double alpha, long time) {
		double N =sizeModels[deme].populationSize(time);
		if (alpha != 0) {
			ExpPopulation popModel =new ExpPopulation(N, alpha, time);
			sizeModels[deme] =popModel;
		} else {
			sizeModels[deme] =new ConstantPopulation(N);
		}
	}

	protected void setExpGrowth(double alpha, long time) {
		for (int i =0; i < demeCount; i++) {
			setExpGrowth(i, alpha, time);
		}

	}

	protected void setPopulationModel(PopulationSizeModel popModel) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		Arrays.fill(sizeModels, popModel);
	}

	/**
	 * a way of deleteing a deme as per MS method of deleting one. Migration into this
	 * deme is set to nill
	 * 
	 * Selection here is a real problem
	 * 
	 * @param deme
	 */
	protected void clearDemeBySizeByMigration(int deme) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		for (int i =0; i < demeCount; i++) {
			if (i == deme){
				mDirectionList[i].clear();
				mRateList[i].clear();
				//return;
			}
			
			ListIterator<Integer> dir =mDirectionList[i].listIterator();
			ListIterator<Double> rates =mRateList[i].listIterator();
			while (dir.hasNext()) {
				int j =dir.next();
				rates.next();
				if (j == deme) {
					dir.remove();
					rates.remove();
				}
			}

		}
		 sizeModels[deme] =PopulationSizeModel.NULL_POPULATION;// not null --to avoid all the cheking
	}

	/**
	 * The sematics are somewhat defined by ms. Note is has zero for everything.
	 */
	protected void addDeme(double N) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		List<Integer>[] newDList =new ArrayList[demeCount + 1];
		List<Double>[] newRList =new ArrayList[demeCount + 1];

		System.arraycopy(mDirectionList, 0, newDList, 0, demeCount);
		System.arraycopy(mRateList, 0, newRList, 0, demeCount);

		newDList[demeCount] =new ArrayList<Integer>();
		newRList[demeCount] =new ArrayList<Double>();

		mDirectionList =newDList;
		mRateList =newRList;

		PopulationSizeModel[] newSizes =new PopulationSizeModel[demeCount + 1];

		System.arraycopy(sizeModels, 0, newSizes, 0, demeCount);

		newSizes[demeCount] =new ConstantPopulation(N);

		sizeModels =newSizes;

		demeCount++;
		
		if(selectionData!=null)
			selectionData.addDeme();

	}

	/**
	 * sets the migration rate for a set of demes. or deletes a entry if rate=0
	 * 
	 * @param i
	 * @param j
	 * @param rate
	 */
	protected void setMigrationRate(int i, int j, double rate) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		// first do we have a i,j entry?
		int index =mDirectionList[i].indexOf(j);
		if (index < 0) {
			if (rate != 0) {
				mDirectionList[i].add(j);
				mRateList[i].add(rate);
			}
			return;
		}
		if (rate == 0) {
			mDirectionList[i].remove(index);
			mRateList[i].remove(index);
			return;
		}

		mRateList[i].set(index, rate);
	}

	/**
	 * just easer for one of ms options
	 * 
	 * @param rates
	 *            a demeCount by demeCount array. We ignore the i==j entrys.
	 */
	protected void setMigrationRates(double[][] rates) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		for (int i =0; i < demeCount; i++) {
			List<Integer> demeList =mDirectionList[i];
			demeList.clear();
			List<Double> rateList =mRateList[i];
			rateList.clear();
			for (int j =0; j < demeCount; j++) {
				if (i == j || rates[i][j] == 0)
					continue;
				demeList.add(j);
				rateList.add(rates[i][j]);
			}
		}

	}

	/**
	 * set the whole migration matrix to a fixed rate... note the rates are totalRate/(npop-1)
	 * 
	 * @param rate
	 */
	protected void setMigrationMatrix(double totalRate) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		if(totalRate<=0)
			return;
		// we just redo the whole state...
		double rate =totalRate / (demeCount - 1);
		for (int i =0; i < demeCount; i++) {
			mDirectionList[i].clear();
			mRateList[i].clear();
			for (int j =0; j < demeCount; j++) {
				if (i == j || rate == 0)
					continue;
				mDirectionList[i].add(j);
				mRateList[i].add(rate);
				//System.out.println("SETTINGMRATE:" + i + "\t" + j + "\t" + rate);
			}
		}
	}

	public long getStartTime() {
		return startTime;
	}

	protected void setStartTime(long startTime) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		this.startTime =startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	protected void setEndTime(long endTime) {
		if (finalized)
			throw new RuntimeException("Mutating a finalized Model");
		this.endTime =endTime;
	}

	/**
	 * finised adding or removing information from this model.
	 */
	public void commitObject() {
		if (finalized)
			return;
		//assert startTime!=endTime;
		// pretty simple...
		mDirectionByDeme =new int[demeCount][0];
		mRatesByDeme =new double[demeCount][0];
		demeTotalRate =new double[demeCount];
		for (int d =0; d < demeCount; d++) {
			mDirectionByDeme[d] =Util.toArrayPrimitiveInteger(mDirectionList[d]);
			mRatesByDeme[d] =Util.toArrayPrimitiveDouble(mRateList[d]);
			demeTotalRate[d] =Util.sum(mRateList[d]);
			// System.out.println("FinilizedTotalRAte:"+demeTotalRate[d]+"\t"+mRateList[d]+"\t"+d+" TIME:"+this.startTime);
		}
		finalized =true;
		mDirectionList =null;
		mRateList =null;
		// System.out.println("Finalized:"+Arrays.toString(demeTotalRate));
		// Thread.dumpStack();
	}
	
	public void initSelectionData(){
		if(selectionData==null)
			selectionData=new SelectionData(this);
	}
	
	public SelectionData getSelectionData() {
		//assert false;
		//initSelectionData();
		return selectionData;
	}
	
	
	
	public String toString(){
		return "Model("+(double)this.getStartTime()+"<>"+(double)this.getEndTime()+")";
	}

	/**
	 * true iff this model is only used for forward simulations. 
	 * @return
	 */
	public boolean isForwardOnly() {
		return forwardOnly;
	}

	public void setForwardOnly(boolean forwardOnly) {
		this.forwardOnly =forwardOnly;
	}

	
	
}
