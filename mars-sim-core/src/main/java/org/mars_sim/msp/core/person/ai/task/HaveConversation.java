/**
 * Mars Simulation Project
 * HaveConversation.java
 * @version 3.1.0 2017-09-13
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.social.Relationship;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.person.ai.task.meta.HaveConversationMeta;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The HaveConversation class is the task of having a casual conversation with another person
 */
public class HaveConversation
extends Task
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(HaveConversation.class.getName());

	private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1,
			logger.getName().length());
	
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.haveConversation"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase HAVING_CONVERSATION = new TaskPhase(Msg.getString(
            "Task.phase.havingConversation")); //$NON-NLS-1$

    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.2D;

    private List<Person> invitees = new ArrayList<Person>();
    
    private Person invitee;
    //private int randomTime;

    private Location invitee_location = null;
    
    private enum Location
    {
        All_Settlements,
        Another_Building,
    	Dining_Building,
    	None,
        Same_Building,
        Same_Vehicle
    }
   
    
    /**
     * Constructor. This is an effort-driven task.
     * @param person the person performing the task.
     */
    public HaveConversation(Person person) {
        // Use Task constructor.
        super(NAME, person, true, false, STRESS_MODIFIER - RandomUtil.getRandomDouble(.2), true, 5D + RandomUtil.getRandomDouble(10));
        
    	// List 8 situations for having a conversation
        if (person.isInSettlement()) {

            Set<Person> pool = new HashSet<Person>();
            Settlement s = person.getSettlement();
            
            Collection<Person> p_same_bldg_talking = s.getChattingPeople(person, false, true, false);        	          
            pool.addAll(p_same_bldg_talking);
        	invitee_location = Location.Same_Building;
        	
        	
            if (pool.size() == 0) {
            	// Go to a chatty chow hall
                Building diningBuilding = EatMeal.getAvailableDiningBuilding(person, true);
                if (diningBuilding != null) {
                	// Walk to that building.
                	walkToActivitySpotInBuilding(diningBuilding, FunctionType.DINING, true);
                    Collection<Person> p_dining = s.getChattingPeople(person, false, true, false);
                	pool.addAll(p_dining);
                	invitee_location = Location.Dining_Building;
                }
                // TODO: should try going to another chow hall that have people chatting if not found and not just the one that he is going to
            }
           
//            if (pool.size() == 0) {
//                Collection<Person> p_same_bldg_idle = s.getChattingPeople(person, true, true, false);                       
//            	pool.addAll(p_same_bldg_idle);
//            	invitee_location = Location.Same_Building;
//            }           
          
            if (pool.size() == 0) {
                Collection<Person> p_diff_bldg_talking = s.getChattingPeople(person, false, false, false);                 
            	pool.addAll(p_diff_bldg_talking);
            	invitee_location = Location.Another_Building;
            }
           
//            if (pool.size() == 0) {
//                Collection<Person> p_diff_bldg_idle = s.getChattingPeople(person, true, false, false);               
//            	pool.addAll(p_diff_bldg_idle);
//            	invitee_location = Location.Another_Building;
//            }
            
            if (pool.size() == 0) {
                Collection<Person> p_talking_all = s.getChattingPeople(person, false, false, true);         
            	pool.addAll(p_talking_all);
            	invitee_location = Location.All_Settlements;
            }
            
//            if (pool.size() == 0) {
//                Collection<Person> p_idle_all = s.getChattingPeople(person, true, false, true);         
//            	pool.addAll(p_idle_all);
//            	invitee_location = Location.All_Settlements;
//            }           
           
            if (pool.size() == 0) {
            	invitee_location = Location.None;
             }
            else {
	            int num = pool.size();
	            List<Person> list = new ArrayList<Person>();
	            list.addAll(pool);
	            if (num == 1) {
	        		invitee = list.get(0);
	        		invitees.add(invitee);
	        		talkTo(invitee);
	            }
	            else if (num > 1) {
	            	int rand = RandomUtil.getRandomInt(num-1);           	
	            	// half of the time, talk to just one person
	            	if (RandomUtil.getRandomInt(1) == 0) {
	            		invitee = list.get(rand);
	            		invitees.add(invitee);
	            		talkTo(invitee);
	            	}
	            	else {	
	            	// speak to a group of people
		            	for (int i= 0; i< rand; i++) {
		            		invitee = list.get(i);
		            		invitees.add(invitee);
		            		talkTo(invitee);
		            	}     	
	            	}
	            }  
            }
        }
        else if (person.isInVehicle()) {

            int score = person.getPreference().getPreferenceScore(new HaveConversationMeta());
            super.setDuration(5 + score);
            //2016-09-24 Factored in a person's preference for the new stress modifier 
            super.setStressModifier(score/10D + STRESS_MODIFIER);

	        // set the boolean to true so that it won't be done again today
        	//person.getPreference().setTaskStatus(this, false);
        	
            Set<Person> pool = new HashSet<Person>();
        	Settlement s = person.getAssociatedSettlement();
            Collection<Person> p_talking_all = s.getChattingPeople(person, false, false, true);         

            Vehicle v = (Vehicle) person.getContainerUnit();
            //Collection<Person> crew = ((Rover) v).getCrew();           
            Collection<Person> talking = v.getTalkingPeople();
            //pool.addAll(crew);   
            
            // remove the one who starts the conversation
            pool.remove((Person)person);         
            pool.addAll(talking);
    		invitee_location = Location.Same_Vehicle;
    		
            if (pool.size() == 0) {
            	pool.addAll(p_talking_all);
            	invitee_location = Location.All_Settlements;
            }          
 
            int num = pool.size();
            List<Person> list = new ArrayList<Person>();
            list.addAll(pool);
            if (num == 1) {
                invitee = list.get(0);
        		invitees.add(invitee);
        		talkTo(invitee);
            }
            else if (num > 1) {
            	int rand = RandomUtil.getRandomInt(num-1);           	
            	// half of the time, talk to just one person
            	if (RandomUtil.getRandomInt(1) == 0) {
            		invitee = list.get(rand);
            		invitees.add(invitee);
            		talkTo(invitee);
            	}
            	else {	
            	// speak to a group of people
	            	for (int i= 0; i< rand; i++) {
	            		invitee = list.get(i);
	            		invitees.add(invitee); 
	            		talkTo(invitee);
	            	}     	
            	}
            }  
            
        }
        else {
            endTask();
        }

        // Initialize phase
        addPhase(HAVING_CONVERSATION);
        setPhase(HAVING_CONVERSATION);
    }

    // Add conditional checking to append " via radio" in two cases
    public void talkTo(Person invitee) {
    	String detail = invitee.getName();
    	if (invitee_location == Location.Another_Building | invitee_location == Location.All_Settlements)
    		detail = detail + " via radio";
    		
    	if (invitee.getMind().getTaskManager().getTask() instanceof HaveConversation) {
        	setDescription(Msg.getString("Task.description.havingConversation.detail", 
                detail)); //$NON-NLS-1$
        	//logger.info(person.getName() + " is chatting with " + detail);
			LogConsolidated.log(logger, Level.FINE, 5000, sourceName,
					"[" + person.getLocationTag().getLocale() + "] " + person.getName() + " was chatting with " + detail + ".", null);
        }	
    }
    
    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (HAVING_CONVERSATION.equals(getPhase())) {
            return havingConversation(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs reading phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double havingConversation(double time) {

        if (isDone()) {
            return time;
        }

        // If duration, send invitation.
        if (getDuration() <= (getTimeCompleted() + time)) {

            // TODO: switch the invitee(s) to HaveConversation.           

            // Check if existing relationship between primary researcher and invitee.
            RelationshipManager relationshipManager = Simulation.instance().getRelationshipManager();
            int size = invitees.size();
                     
            for (int i= 0; i< size; i++) {
            	Person invitee = invitees.get(i);
	            if (!relationshipManager.hasRelationship(person, invitee)) {
	                // Add new communication meeting relationship.
	                relationshipManager.addRelationship(person, invitee, Relationship.COMMUNICATION_MEETING);
	            }
	
	            // Add 1 point to invitee's opinion of the one who starts the conversation
	            Relationship relationship = relationshipManager.getRelationship(invitee, person);
	            double currentOpinion = relationship.getPersonOpinion(invitee);
	            relationship.setPersonOpinion(invitee, currentOpinion + RandomUtil.getRandomDouble(1));
	
            }
        }

        return 0D;
    }

    
    
    @Override
    protected void addExperience(double time) {
        // This task adds no experience.
    }

    @Override
    public void endTask() {
        super.endTask();
    }

    @Override
    public int getEffectiveSkillLevel() {
        return 0;
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(0);
        return results;
    }

    @Override
    public void destroy() {
        super.destroy();

    }
}