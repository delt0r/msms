package at.mabs.model;

/**
 * see Model for details. Events modify models in a pastward direction. 
 * @author bob
 *
 */
public class Event implements Comparable<Event>{
	private Model pastward;
	private Model presentward;
	public final long time;
	
	
	public Event(long time) {
		this.time=time;
	}
	
	
	@Override
	public int compareTo(Event o) {
		if(time>o.time)
			return 1;
		if(time<o.time)
			return -1;
		return 0;
	}
	
    
    public Model getPastward() {
		return pastward;
	}
    
    public Model getPresentward() {
		return presentward;
	}
    
    void setPresentward(Model presentward) {
		this.presentward = presentward;
	}
    
    void setPastward(Model pastward) {
		this.pastward = pastward;
	}

    /**
     * creates the link to more "present" model. ie this event is more pastward, and returns the more pastward model with this event applied. 
     * @param parent
     * @return
     */
    public Model link(Model presentwardModel) {
    	assert presentwardModel.getPresentward().time<=this.time;
    	
    	this.presentward=presentwardModel;
    	presentwardModel.setPastward(this);
    	
    	Model pastwardModel=new Model(presentwardModel);
    	this.applyEvent(pastwardModel);
    	
    	this.pastward=pastwardModel;
    	pastwardModel.setPresentward(this);
    	
    	return pastwardModel;
	}
    
    
    /**
     * 
     * @param model to mutate 
     * @return
     */
    protected void applyEvent(Model model){
    	
    }
    
    public long getTime() {
		return time;
	}
    
    @Override
    public String toString() {
    	
    	return "Event @ "+time;
    }
    
}
