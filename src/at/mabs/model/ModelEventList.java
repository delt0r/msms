package at.mabs.model;
import java.util.*;
/**
 * The place to put events, that crates models and other structures that are needed. Don't know how caching behavour will work. But its managed here. 
 * @author bob
 *
 */
public class ModelEventList {
	private Event present=new Event(0);//terminators...
	private Event past=new Event(Long.MAX_VALUE);//most pastward.
	private List<Event> events=new ArrayList<Event>();
	
	
	private int totalDemeCount;//-I option + all -es options.
	
	
	
	public ModelEventList(Collection<Event> event) {
		events.clear();
		events.addAll(event);
		init();
	}
	
	private void init(){
		Collections.sort(events);
		
		Model model=new Model();
		model=present.link(model);
		for(Event e:events){
			model=e.link(model);
		}
		model.setPastward(past);
		past.setPresentward(model);
		//System.out.println(this);
	}
	
	public Event getPresent() {
		return present;
	}
	
	@Override
	public String toString() {
		String s="ModelEventList:{";
		Event e=present;
		while(e!=null && e.getPastward()!=null){
			s+=e+"->"+e.getPastward()+"->";
			e=e.getPastward().getPastward();
		}
		return s+"}";
	}

}
