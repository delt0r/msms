package at.mabs.segment;

import java.util.ArrayList;
import java.util.List;

public class MultiOriginMutation extends InfinteMutation {
	private List<FixedBitSet> origins;
	
	public MultiOriginMutation(double p,FixedBitSet union,List<FixedBitSet> origins) {
		super(p,union);
		//assert origins.size()>0;
		this.origins=new ArrayList<FixedBitSet>(origins);
	}
	
	@Override
	public char tranlasteAtSample(int i) {
		if(!this.leafSet.contains(i) || origins.isEmpty()){
			return '0';
		}
		for(int j=0;j<origins.size() && j+1<Character.MAX_RADIX;j++){
			if(origins.get(j).contains(i)){
				return Character.forDigit(j+1, Character.MAX_RADIX);
			}
		}
		return '*';//got really confused
	}
}
