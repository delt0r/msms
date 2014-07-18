package at.mabs.abc;

import java.util.*;

import at.mabs.util.random.Random64;

/**
 * New improved prior density. 
 * 
 * 
 * @author bob
 * 
 */
public class PriorDensity {
	private double min, max, span;
	
	private PriorDensity pmin,pmax;
	private String pminName,pmaxName;
	
	private double proposialWindow = 0.1;
	private Random random = new Random64();// really random...and 100% thread
											// safe.
	private int argIndex = -1;
	private double value;
	private boolean integer=false;
	
	private String name;
	private String copyName;
	

	protected PriorDensity() {
		
	}

	/**
	 * String format: [NAME=%][LABEL%] NUMBER%NUMBER[%NUMBER] [%i][%LABEL]
	 * 
	 * Labels should refer to something correct in context. Here that is other parameter names. 
	 * Names must start with alpha chars [a-z][A-Z]. the %i denotes an integer parameter. The labels are for bigger than and smaller than respectively. 
	 * They should be defined before this parameter. Cycles will probably fail. 
	 * 
	 * this is for rejection sampling.
	 * 
	 * 
	 * s.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")//a number
	 * @param args
	 */
	private PriorDensity(String args, int argIndex) {
		this.argIndex = argIndex;
		name="PD index:"+argIndex;
		StringTokenizer tokens = new StringTokenizer(args, "%");
		int count=tokens.countTokens();
		String token=tokens.nextToken();
		if(count==1){
			copyName=token;
			return;
		}
		
		if(token.endsWith("=")){
			name=token.substring(0, token.length()-1);
			token=tokens.nextToken();
		}
		if(Character.isLetter(token.charAt(0))){
			pminName=token;
			token=tokens.nextToken();
		}
		//must be a number...so we have 2 or 3 numbers. 
		assert token.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$");
		min=Double.parseDouble(token);
		token=tokens.nextToken();
		max=Double.parseDouble(token);
		span = max - min;
		if(!tokens.hasMoreTokens())
			return;
		token=tokens.nextToken();
		//next token if number its range proposial. otherwise its an "i" or a max label.
		if(token.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")){
			proposialWindow=Double.parseDouble(token);
			if(tokens.hasMoreElements()){
				token=tokens.nextToken();
			}else{
				return;
			}
		}
		
		if(token.equals("i")){
			integer=true;
			if(tokens.hasMoreElements()){
				token=tokens.nextToken();
			}else{
				return;
			}
		}
		//must be a label ...right.. so we don't need to check .. right...
		pmaxName=token;
		if(tokens.hasMoreTokens()){
			System.err.println("WARNING! Some parameters not used for PriorDensity. Or parameter prior.");
		}
		
		clampToConstraints();
	}

	public void updateMinMax(double min, double max) {
		this.min = min;
		this.max = max;
		span = max - min;
		
	}

	/**
	 * 
	 */
	public void generateRandom() {
		double u = random.nextDouble();
		value = u * span + min;
		if(integer){
			value=Math.rint(value);
		}
	}

	/*
	 *  propose a value
	 */
	public double nextProp(double v) {
		double nmin = Math.max(v - span * proposialWindow, min);
		double nmax = Math.min(v + span * proposialWindow, max);
		value = random.nextDouble() * (nmax - nmin) + nmin;
		// System.out.println("Parameter:"+lastValue);
		if(integer){
			value=Math.rint(value);
		}
		return value;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	public int getArgIndex() {
		return argIndex;
	}

	public double getValue() {
		return value;
	}

	
	/**
	 * clamps the value. But does not take constraints into account.
	 * @param lastValue
	 */
	public void setValue(double lastValue) {
		lastValue = Math.min(lastValue, max);
		lastValue = Math.max(lastValue, min);
		this.value = lastValue;
		if(integer){
			value=Math.rint(value);
		}
	}

	public void clampToConstraints(){
		double v=getValue();
		if(pmin!=null){
			v=Math.max(v,pmin.getValue());
		}
		if(pmax!=null){
			v=Math.min(v, pmax.getValue());
		}
		setValue(v);
	}
	
	/*
	 * true if constrains are valid. 
	 */
	public boolean isValid(){
		//System.out.println("Valid:"+pmin+"\t"+pmax+"\t"+value);
		if(pmin!=null && value<pmin.getValue())
			return false;
		if(pmax!=null && value>pmax.getValue())
			return false;
		return true;
	}
	
	
	public double getValueUI() {
		if(max-min<1e-15)
			return min;
		return (value - min) / (max - min);
	}

	public void setValueUI(double uiValue) {
		double lv = min + (max - min) * uiValue;
		setValue(lv);
	}
	
	public boolean isInteger() {
		return integer;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		
		return "PD "+name+"("+min+" <- "+value+" -> "+max+")";
	}
	
	public static List<PriorDensity> parseAnnotatedStrings(String[] anotatedArgs){
		HashMap<String,PriorDensity> params=new HashMap<String, PriorDensity>();
		ArrayList<PriorDensity> list=new ArrayList<PriorDensity>();
		for(int i=0;i<anotatedArgs.length;i++){
			String arg=anotatedArgs[i];
			if(!arg.contains("%"))
				continue;
			PriorDensity pd=new PriorDensity(arg,i);
			params.put(pd.getName(), pd);
			list.add(pd);
		}
		//System.out.println("First pass:"+list+"\t"+list.size());
		//now link em.
		for(int i=0;i<list.size();i++){
			PriorDensity pd=list.get(i);
			if(pd.copyName!=null){
				PriorDensity copyD=params.get(pd.copyName);
				if(copyD==null){
					throw new RuntimeException("Incorrect copy label name:"+pd.copyName+"\n"+params);
				}
				CopyPriorDensity cpd=new CopyPriorDensity(copyD, pd.getArgIndex());
				list.set(i, cpd);
				continue;
			}
			//link upper and lower bounds.
			if(pd.pminName!=null){
				PriorDensity pmin=params.get(pd.pminName);
				if(pmin==null){
					throw new RuntimeException("Incorrect min label name:"+pd.pminName+"\n"+params);
				}
				pd.pmin=pmin;
				pd.clampToConstraints();
			}
			
			if(pd.pmaxName!=null){
				PriorDensity pmax=params.get(pd.pmaxName);
				if(pmax==null){
					throw new RuntimeException("Incorrect max label name:"+pd.pmaxName+"\n"+params);
				}
				pd.pmax=pmax;
				pd.clampToConstraints();
			}
			
		}	
		for(PriorDensity pd:list)
			pd.clampToConstraints();
		return list;
	}
}
