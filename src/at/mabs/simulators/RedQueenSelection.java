package at.mabs.simulators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import at.mabs.cern.jet.random.Binomial;
import at.mabs.model.Model;
import at.mabs.model.ModelEvent;
import at.mabs.model.ModelHistroy;
import at.mabs.model.PopulationSizeModel;

import at.mabs.model.selection.SelectionSimulator;
import at.mabs.model.selection.SelectionStartEvent;
import at.mabs.model.selection.SelectionStrengthModel;
import at.mabs.model.selection.SuperFrequencyTrace;
import at.mabs.util.random.RandomGenerator;

public class RedQueenSelection implements SelectionSimulator {
	private final double sh;
	private final double sp;
	private final double mu; 
	
	
	
	public RedQueenSelection(double sh, double sp, double mu) {
		this.sh =sh;
		this.sp =sp;
		this.mu =mu;
	}

	@Override
	public List<ModelEvent> init(ModelHistroy modelHistory) {
		return new ArrayList<ModelEvent>();
	}
	
	
	@Override
	public List<ModelEvent> forwardSimulator(Model model, ModelHistroy modelHistory, SelectionStrengthModel[] ssm, SuperFrequencyTrace trace) {
//		if(model.getEndTime()==Long.MAX_VALUE || model.getDemeCount()!=1){
//			throw new RuntimeException("Queens forward simulator must be used on time bounded selection models (use the -SI option) and on just one deme.");
//		}
//		Random random=RandomGenerator.getRandom();
//		
//		double p=random.nextDouble();
//		double q=random.nextDouble();
//		
//		PopulationSizeModel[] sizes = model.getPopulationSizeModels();
//		int N = (int) sizes[0].populationSize(0);//;
//		
//		
//		Binomial bin=RandomGenerator.getBinomial();
//		
//		do{
//			double shqn=1-sh*q;
//			double pn=1-p;
//			double qn=1-q;
//			double pp=p*shqn;
//			double b=pp+pn*(1-sh*qn);
//			
//			pp/=b;
//			
//			double sppnn=1-sp*pn;
//			double qp=q*sppnn;
//			b=qp+qn*(1-sp*p);
//			qp/=b;
//			
//			//System.out.println(pp+"\t"+qp);
//			
//			q=qp*(1-mu)+(1-qp)*mu;
//			p=pp*(1-mu)+(1-pp)*mu;
//			
//			p=(double)bin.generateBinomial(N, p)/N;
//			//q=(double)bin.generateBinomial(N, q)/N;
//			double[][] frequencys=trace.previous(false);
//			frequencys[0][1]=p;
//			frequencys[0][0]=1-p;
//			
//		}while(trace.hasPrevious());
//		
//		double[] freq =new double[model.getDemeCount()];
//		double[][] local=trace.current();
//		for (int i =0; i < freq.length; i++) {
//			freq[i] =local[i][1];
//		}
//		List<ModelEvent> events=new ArrayList<ModelEvent>();
//		events.add(new SelectionStartEvent(trace.getStartTime(), freq));
		return null;
	}

}
