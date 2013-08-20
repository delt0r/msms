package at.mabs.model.test;

import java.util.*;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.mabs.model.Event;
import at.mabs.model.ModelEventList;
import at.mabs.util.random.Random64;

public class EventModelTest {
	private  Random64 random=new Random64();
	@Before
	public void setup(){
		
	}
	
	@After
	public void tearDown(){
		
	}
	
	@Test
	public void modelEventOrder() {
		//build 1000 random null events/models.
		for(int i=0;i<1000;i++){
			ArrayList<Event> list=new ArrayList();
			for(int j=0;j<100;j++){
				list.add(new Event(random.nextInt(100)));
			}
			ModelEventList mel=new ModelEventList(list);
			
			//test everything.
			Event present=mel.getPresent();
			int count=0;
			while(present.getPastward()!=null){
				count++;
				Event next=present.getPastward().getPastward();//skip the model see the next event.
				assertTrue(present.time<=next.time);
				present=next;
			}
			assertEquals(list.size()+1, count);
		}
	}

	
	
}
