/**
 * Mars Simulation Project
 * BuildingSalvageMissionMeta.java
 * @version 3.1.0 2017-09-15
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.construction.SalvageValues;

/**
 * A meta mission for the BuildingSalvageMission mission.
 */
public class BuildingSalvageMissionMeta implements MetaMission {

    /** Mission name */
    private static final String NAME = Msg.getString(
            "Mission.description.salvageBuilding"); //$NON-NLS-1$

    /** default logger. */
    private static Logger logger = Logger.getLogger(BuildingSalvageMissionMeta.class.getName());
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Mission constructInstance(Person person) {
        return new BuildingSalvageMission(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;
  
        // No construction until after the first ten sols of the simulation.
        //MarsClock startTime = Simulation.instance().getMasterClock().getInitialMarsTime();
        //MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
        //double totalTimeMillisols = MarsClock.getTimeDiff(currentTime, startTime);
        //double totalTimeSols = totalTimeMillisols / 1000D;
        //if (totalTimeSols < 10D)
        //    return 0;
        //int today = Simulation.instance().getMasterClock().getMarsClock().getSolElapsedFromStart();
        
        if (marsClock.getMissionSol() < BuildingSalvageMission.FIRST_AVAILABLE_SOL)
        	return 0;

        // Check if person is in a settlement.
        if (person.isInSettlement()) {
            Settlement settlement = person.getSettlement();

            // Check if settlement has construction override flag set.
            if (settlement.getConstructionOverride())
            	return 0;


            // Check if available light utility vehicles.
            else if (!BuildingSalvageMission.isLUVAvailable(settlement))
                return 0;

            // Check if enough available people at settlement for mission.
            int availablePeopleNum = 0;
            Iterator<Person> i = settlement.getIndoorPeople().iterator();
            while (i.hasNext()) {
                Person member = i.next();
                boolean noMission = !member.getMind().hasActiveMission();
                boolean isFit = !member.getPhysicalCondition()
                        .hasSeriousMedicalProblems();
                if (noMission && isFit) {
                    availablePeopleNum++;
                }
            }

            if (availablePeopleNum < BuildingSalvageMission.MIN_PEOPLE)
                return 0;

            // Check if min number of EVA suits at settlement.
            if (Mission.getNumberAvailableEVASuitsAtSettlement(settlement) < BuildingSalvageMission.MIN_PEOPLE) {
            	return 0;
            }

            try {
                int constructionSkill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.CONSTRUCTION);
                SalvageValues values = settlement.getConstructionManager()
                        .getSalvageValues();
                double salvageProfit = values
                        .getSettlementSalvageProfit(constructionSkill);
                result = salvageProfit;
                if (result > 10D) {
                    result = 10D;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "Error getting salvage construction site by a person.", e);
                e.printStackTrace();
            	return 0;
            }

            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null) {
                result *= job.getStartMissionProbabilityModifier(BuildingSalvageMission.class);
            }
        }

        return result;
    }

	@Override
	public Mission constructInstance(Robot robot) {
        return null;//new BuildingSalvageMission(robot);
	}

	@Override
	public double getProbability(Robot robot) {
        return 0;
    }
}