/**
 * Mars Simulation Project
 * ReadMeta.java
 * @version 3.1.0 2017-02-20
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.task.Read;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;

/**
 * Meta task for the Read task.
 */
public class ReadMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.read"); //$NON-NLS-1$
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new Read(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        LocationSituation ls = person.getLocationSituation();
        if (ls == LocationSituation.IN_SETTLEMENT
        	|| ls == LocationSituation.IN_VEHICLE) {

        	result += 3D;

        	if (ls == LocationSituation.IN_VEHICLE)
        		result *= RandomUtil.getRandomDouble(2); // more likely than not if on a vehicle

	        // Effort-driven task modifier.
	        //result *= person.getPerformanceRating();

            String fav = person.getFavorite().getFavoriteActivity();
            // The 3 favorite activities drive the person to want to read
            if (fav.equalsIgnoreCase("Research")) {
                result *= 1.5D;
            }
            else if (fav.equalsIgnoreCase("Tinkering")) {
                result *= 1.1D;
            }
            else if (fav.equalsIgnoreCase("Lab Experimentation")) {
                result *= 1.2D;
            }


            // Probability affected by the person's stress and fatigue.
            PhysicalCondition condition = person.getPhysicalCondition();
            double fatigue = condition.getFatigue();
            double stress = condition.getStress();
            
         	if (fatigue > 750D)
         		result/=1.5;
         	else if (fatigue > 1500D)
         		result/=2D;
         	else if (fatigue > 2000D)
         		result/=3D;
         	else
         		result/=4D;
         	
         	if (stress > 45D)
         		result/=1.5;
         	else if (stress > 65D)
         		result/=2D;
         	else if (stress > 85D)
         		result/=3D;
         	else
         		result/=4D;
         	
            // 2015-06-07 Added Preference modifier
            if (result > 0D) {
                result = result + result * person.getPreference().getPreferenceScore(this)/2D;
            }
            
            
	        if (result < 0) result = 0;

        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getProbability(Robot robot) {
		// TODO Auto-generated method stub
		return 0;
	}
}