/**
 * Mars Simulation Project
 * ReviewJobReassignment.java
  * @version 3.1.0 2017-09-07
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.RoleType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.JobAssignment;
import org.mars_sim.msp.core.person.ai.job.JobAssignmentType;
import org.mars_sim.msp.core.person.ai.job.JobManager;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.Administration;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The ReviewJobReassignment class is a task for reviewing job reassignment
 * submission in an office space
 */
public class ReviewJobReassignment extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static transient Logger logger = Logger.getLogger(ReviewJobReassignment.class.getName());
	
	private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1,
			logger.getName().length());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.reviewJobReassignment"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase REVIEWING_JOB_ASSIGNMENT = new TaskPhase(
			Msg.getString("Task.phase.reviewingJobReassignment")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -.1D;

	// Data members
	/** The administration building the person is using. */
	private Administration office;

	// private MarsClock clock;

	public RoleType roleType;

	/**
	 * Constructor. This is an effort-driven task.
	 * 
	 * @param person the person performing the task.
	 */
	public ReviewJobReassignment(Person person) {
		// Use Task constructor.
		super(NAME, person, true, false, STRESS_MODIFIER, true, 50D + RandomUtil.getRandomDouble(100D));

		if (person.isInside()) {
			// if (roleType == null)
			// NOTE: sometimes enum is null. sometimes it is NOT. why?
			roleType = person.getRole().getType();

			if (roleType.equals(RoleType.PRESIDENT) || roleType.equals(RoleType.MAYOR)
					|| roleType.equals(RoleType.COMMANDER) || roleType.equals(RoleType.SUB_COMMANDER)) {

				// System.out.println("ReviewJobReassignment : "
				// + person.getName() + " (" + roleType
				// + ") is going to review job reassignment");

				// If person is in a settlement, try to find an office building.
				Building officeBuilding = Administration.getAvailableOffice(person);

				// Note: office building is optional
				if (officeBuilding != null) {
					// Walk to the office building.
					walkToActivitySpotInBuilding(officeBuilding, false);

					office = officeBuilding.getAdministration();
					
					this.walkToActivitySpotInBuilding(officeBuilding, true);
				}

				// TODO: add other workplace if administration building is not available

			} // end of roleType
			else {
				endTask();
			}
		}
		else {
			endTask();
		}

		// Initialize phase
		addPhase(REVIEWING_JOB_ASSIGNMENT);
		setPhase(REVIEWING_JOB_ASSIGNMENT);
	}

	@Override
	protected FunctionType getLivingFunction() {
		return FunctionType.ADMINISTRATION;
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (REVIEWING_JOB_ASSIGNMENT.equals(getPhase())) {
			return reviewingPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the reviewingPhasephase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double reviewingPhase(double time) {
		// Iterates through each person
		Iterator<Person> i = person.getSettlement().getAllAssociatedPeople().iterator();
		while (i.hasNext()) {
			Person tempPerson = i.next();
			List<JobAssignment> list = tempPerson.getJobHistory().getJobAssignmentList();
			int last = list.size() - 1;
			JobAssignmentType status = list.get(last).getStatus();

			if (status != null && status == JobAssignmentType.PENDING) {
				// System.out.println("ReviewJobReassignment : start reviewing job reassignment
				// request from " + tempPerson.getName() + "\n");
				String pendingJobStr = list.get(last).getJobType();
				String lastJobStr = null;
				if (last == 0)
					lastJobStr = pendingJobStr;
				else
					lastJobStr = list.get(last - 1).getJobType();
				String approvedBy = person.getRole().getType() + " " + person.getName();

				// 1. Reviews requester's cumulative job rating
				double rating = list.get(last).getJobRating();
				double cumulative_rating = 0;
				int size = list.size();
				for (int j = 0; j < size; j++) {
					cumulative_rating += list.get(j).getJobRating();
				}
				cumulative_rating = cumulative_rating / size;

				// TODO: Add more depth to this process
				// 2. Reviews this person's preference 
				// 3. Go to him/her to have a chat
				// 4. Modified by the affinity between them
				// 5. Approve/disapprove the job change
				
				if (rating < 2.5 || cumulative_rating < 2.5) {
					tempPerson.getMind().reassignJob(lastJobStr, true, JobManager.USER,
							JobAssignmentType.NOT_APPROVED, approvedBy);

					LogConsolidated.log(logger, Level.INFO, 5000, sourceName,
							"[" + person.getSettlement() + "] " + approvedBy + " did NOT approve " + tempPerson
							+ "'s job reassignment as " + pendingJobStr
							+ "Try again when the performance rating is higher.", null);
				} else {

					// Updates the job
					tempPerson.getMind().reassignJob(pendingJobStr, true, JobManager.USER,
							JobAssignmentType.APPROVED, approvedBy);
					LogConsolidated.log(logger, Level.INFO, 5000, sourceName,
							"[" + person.getSettlement() + "] " + approvedBy + " just approved " + tempPerson
							+ "'s job reassignment as " + pendingJobStr, null);
				}
				
				// Do only one review each time
				break;
			}
		} // end of while
		
		return 0D;
	}

	@Override
	protected void addExperience(double time) {
		// This task adds no experience.
	}

	@Override
	public void endTask() {
		super.endTask();

		// Remove person from administration function so others can use it.
		if (office != null && office.getNumStaff() > 0) {
			office.removeStaff();
		}
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

		office = null;
	}
}