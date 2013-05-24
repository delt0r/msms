package at.mabs.stats;

import java.util.Iterator;
import java.util.List;

import at.mabs.segment.InfinteMutation;
import at.mabs.segment.SegmentEventRecoder;

public class DiversityStat implements StringStatsCollector {
	private int size=25;
	private Density last;
	public DiversityStat() {
	
	}
	
	public DiversityStat(String size) {
		this.size=Integer.parseInt(size);
	}
	
	
	@Override
	public void collectStats(SegmentEventRecoder recorder, StringBuilder builder) {
		//builder.append("Diversity:\n");
		List<InfinteMutation> mutations=recorder.getMutationsSorted();
		if(mutations.size()<size){
			return;
		}
		//double[] aves=new double[mutations.size()];
		Density density=new Density(mutations.size()-size);
		for(int i=0;i<mutations.size()-size;i++){
			InfinteMutation start=mutations.get(i);
			InfinteMutation end=mutations.get(i+size);
			
			double l=end.position-start.position;
			double p=(end.position+start.position)/2;
			density.add(p, l);
			//builder.append(p+"\t"+l+"\n");
//			for(int j=i;j<i+size;j++){
//				aves[j]+=l;
//			}
			
		}
		if(last!=null){
			builder.append("div:\t"+Density.norm(last, density)+"\n");
		}else{
			last=density;
		}
//		for(int i=0;i<mutations.size();i++){
//			double p=mutations.get(i).postion;
//			double v=aves[i]/size;
//			builder.append(p+"\t"+v+"\n");
//		}
		

	}

	@Override
	public void summary(StringBuilder builder) {


	}

}
