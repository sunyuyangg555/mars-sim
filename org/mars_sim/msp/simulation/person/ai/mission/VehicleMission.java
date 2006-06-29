/**
 * Mars Simulation Project
 * VehicleMission.java
 * @version 2.79 2006-06-13
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person.ai.mission;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mars_sim.msp.simulation.Coordinates;
import org.mars_sim.msp.simulation.Inventory;
import org.mars_sim.msp.simulation.RandomUtil;
import org.mars_sim.msp.simulation.Simulation;
import org.mars_sim.msp.simulation.person.Person;
import org.mars_sim.msp.simulation.person.PersonIterator;
import org.mars_sim.msp.simulation.person.ai.task.LoadVehicle;
import org.mars_sim.msp.simulation.person.ai.task.OperateVehicle;
import org.mars_sim.msp.simulation.resource.AmountResource;
import org.mars_sim.msp.simulation.resource.ItemResource;
import org.mars_sim.msp.simulation.resource.Resource;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.SettlementIterator;
import org.mars_sim.msp.simulation.vehicle.Vehicle;
import org.mars_sim.msp.simulation.vehicle.VehicleCollection;
import org.mars_sim.msp.simulation.vehicle.VehicleOperator;
import org.mars_sim.msp.simulation.vehicle.VehicleIterator;
import org.mars_sim.msp.simulation.time.MarsClock;

/**
 * A mission that involves driving a vehicle along a series of navpoints.
 */
public abstract class VehicleMission extends TravelMission {
	
	// Mission phases
	public static final String EMBARKING = "Embarking";
	public static final String TRAVELLING = "Travelling";
	public static final String DISEMBARKING = "Disembarking";
	
	// Data members
	private Vehicle vehicle;
	private VehicleOperator lastOperator; // The last operator of this vehicle in the mission.
	protected boolean loadedFlag = false; // True if vehicle has been loaded.
	
    // Mission tasks tracked
    private OperateVehicle operateVehicleTask; // The current operate vehicle task.
    
    // Caches
	protected Map equipmentNeededCache;

    /**
     * Constructor
     * @param name the name of the mission.
     * @param startingPerson the person starting the mission
     * @param minPeople the minimum number of mission members allowed
     * @throws MissionException if error constructing mission.
     */
	protected VehicleMission(String name, Person startingPerson, int minPeople) throws MissionException {
		// Use TravelMission constructor.
		super(name, startingPerson, minPeople);
		
		// Add mission phases.
		addPhase(EMBARKING);
		addPhase(TRAVELLING);
		addPhase(DISEMBARKING);
		
		// Reserve a vehicle.
		try {
			if (!reserveVehicle(startingPerson)) endMission();
		}
		catch (Exception e) {
			throw new MissionException("Constructor", e);
		}
	}
	
	/**
	 * Gets the mission's vehicle if there is one.
	 * @return vehicle or null if none.
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}
	
	/**
	 * Sets the vehicle for this mission.
	 * @param newVehicle the vehicle to use.
	 * @throws MissionException if vehicle cannot be used.
	 */
	protected void setVehicle(Vehicle newVehicle) throws MissionException {
		if (newVehicle != null) {
			try {
				if (isUsableVehicle(newVehicle)) {
					vehicle = newVehicle;
					newVehicle.setReservedForMission(true);
				}
				throw new MissionException(getPhase(), "newVehicle is not usable for this mission.");
			}
			catch (Exception e) {
				throw new MissionException(getPhase(), "Problem determining if vehicle is usable.");
			}
		}
		else throw new IllegalArgumentException("newVehicle is null.");
	}
	
	/**
	 * Checks if the mission has a vehicle.
	 * @return true if vehicle.
	 */
	public boolean hasVehicle() {
		return (vehicle != null);
	}
	
	/**
	 * Leaves the mission's vehicle and unreserves it.
	 */
	protected void leaveVehicle() {
		if (hasVehicle()) {
			vehicle.setReservedForMission(false);
			vehicle = null;
		}
	}
	
	/**
	 * Checks if vehicle is usable for this mission.
	 * (This method should be added to by children)
	 * @param newVehicle the vehicle to check
	 * @return true if vehicle is usable.
	 * @throws IllegalArgumentException if newVehicle is null.
	 * @throws Exception if problem checking vehicle is loadable.
	 */
	protected boolean isUsableVehicle(Vehicle newVehicle) throws Exception {
		if (newVehicle != null) {
			boolean usable = true;
			if (newVehicle.isReserved()) usable = false;
			if (!newVehicle.getStatus().equals(Vehicle.PARKED)) usable = false;
			return usable;
		}
		else throw new IllegalArgumentException("isUsableVehicle: newVehicle is null.");
	}
	
	/**
	 * Compares the quality of two vehicles for use in this mission.
	 * (This method should be added to by children)
	 * @param firstVehicle the first vehicle to compare
	 * @param secondVehicle the second vehicle to compare
	 * @return -1 if the second vehicle is better than the first vehicle, 
	 * 0 if vehicle are equal in quality,
	 * and 1 if the first vehicle is better than the second vehicle.
	 * @throws Exception if error determining vehicle range.
	 */
	protected int compareVehicles(Vehicle firstVehicle, Vehicle secondVehicle) throws Exception {
		if (isUsableVehicle(firstVehicle)) {
			if (isUsableVehicle(secondVehicle)) {
				// Vehicle with superior range should be ranked higher.
				if (firstVehicle.getRange() > secondVehicle.getRange()) return 1;
				else if (firstVehicle.getRange() < secondVehicle.getRange()) return -1;
				else return 0;
			}
			else return 1;
		}
		else {
			if (isUsableVehicle(secondVehicle)) return -1;
			else return 0;
		}
	}
	
	/**
	 * Reserves a vehicle for the mission if possible.
	 * @param person the person reserving the vehicle.
	 * @return true if vehicle is reserved, false if unable to.
	 * @throws Exception if error reserving vehicle.
	 */
	protected boolean reserveVehicle(Person person) throws Exception {
		
		VehicleCollection bestVehicles = new VehicleCollection();
		
		// Create list of best unreserved vehicles for the mission.
		VehicleIterator i = getAvailableVehicles(person.getSettlement()).iterator();
		while (i.hasNext()) {
			Vehicle availableVehicle = i.next();
			if (bestVehicles.size() > 0) {
				int comparison = compareVehicles(availableVehicle, (Vehicle) bestVehicles.get(0));
				if (comparison == 0) bestVehicles.add(availableVehicle);
				else if (comparison == 1) {
					 bestVehicles.clear();
					 bestVehicles.add(availableVehicle);
				}
			}
			else bestVehicles.add(availableVehicle);
		}
		
		// Randomly select from the best vehicles.
		if (bestVehicles.size() > 0) {
			int bestVehicleIndex = RandomUtil.getRandomInt(bestVehicles.size() - 1);
			try {
				setVehicle((Vehicle) bestVehicles.get(bestVehicleIndex));
			}
			catch (Exception e) {}
		}
		
		return hasVehicle();
	}	
	
	/**
	 * Gets a collection of available vehicles at a settlement that are usable for this mission.
	 * @param settlement the settlement to find vehicles.
	 * @return list of available vehicles.
	 * @throws Exception if problem determining if vehicles are usable.
	 */
	private VehicleCollection getAvailableVehicles(Settlement settlement) throws Exception {
		VehicleCollection result = new VehicleCollection();
		
		VehicleIterator i = settlement.getParkedVehicles().iterator();
		while (i.hasNext()) {
			Vehicle vehicle = i.next();
			if (isUsableVehicle(vehicle)) result.add(vehicle);
		}
		
		return result;
	}	
	
	/** 
	 * Finalizes the mission 
	 */
	protected void endMission() {
		leaveVehicle();
		super.endMission();
	}	
	
    /** 
     * Determine if a vehicle is sufficiently loaded with fuel and supplies.
     * @return true if rover is loaded.
     * @throws Exception if error checking vehicle.
     */
    public boolean isVehicleLoaded() throws Exception {
    	
    	return LoadVehicle.isFullyLoaded(getResourcesNeededForRemainingMission(true), 
    			getEquipmentNeededForRemainingMission(true), getVehicle());
    }
    
    /**
     * Checks if a vehicle can load the supplies needed by the mission.
     * @return true if vehicle is loadable.
     * @throws Exception if error checking vehicle.
     */
    public boolean isVehicleLoadable() throws Exception {
    	
    	Map resources = getResourcesNeededForRemainingMission(true);
    	Map equipment = getEquipmentNeededForRemainingMission(true);
    	Vehicle vehicle = getVehicle();
    	Settlement settlement = vehicle.getSettlement();
    	double tripTime = getEstimatedRemainingMissionTime(true);
    	
    	boolean vehicleCapacity = LoadVehicle.enoughCapacityForSupplies(resources, equipment, vehicle, settlement);
    	boolean settlementSupplies = LoadVehicle.hasEnoughSupplies(settlement, resources, equipment, getPeopleNumber(), tripTime);
    	
    	return vehicleCapacity && settlementSupplies;
    }
    
    /**
     * Gets the amount of fuel (kg) needed for a trip of a given distance (km).
     * @param tripDistance the distance (km) of the trip.
     * @param fuelEfficiency the vehicle's fuel efficiency (km/kg).
     * @param useBuffer use time buffers in estimation if true.
     * @return
     */
    public static double getFuelNeededForTrip(double tripDistance, double fuelEfficiency, boolean useBuffer) {
    	double result = tripDistance / fuelEfficiency;
    	if (useBuffer) result *= Vehicle.RANGE_ERROR_MARGIN;
    	return result;
    }
    
    /**
     * Determines a new phase for the mission when the current phase has ended.
     * @throws MissionException if problem setting a new phase.
     */
    protected void determineNewPhase() throws MissionException {
    	if (EMBARKING.equals(getPhase())) setPhase(VehicleMission.TRAVELLING);
		else if (TRAVELLING.equals(getPhase())) {
			if (getCurrentNavpoint().isSettlementAtNavpoint()) setPhase(VehicleMission.DISEMBARKING);
		}
		else if (DISEMBARKING.equals(getPhase())) endMission();
    }
    
    /**
     * The person performs the current phase of the mission.
     * @param person the person performing the phase.
     * @throws MissionException if problem performing the phase.
     */
    protected void performPhase(Person person) throws MissionException {
    	if (EMBARKING.equals(getPhase())) performEmbarkFromSettlementPhase(person);
		else if (TRAVELLING.equals(getPhase())) performTravelPhase(person);
		else if (DISEMBARKING.equals(getPhase())) performDisembarkToSettlementPhase(person, 
				getCurrentNavpoint().getSettlement());
    }
    
    /**
     * Performs the travel phase of the mission.
     * @param person the person currently performing the mission.
     * @throws MissionException if error performing phase.
     */
    protected void performTravelPhase(Person person) throws MissionException {
    	
    	// Initialize travel phase if it's not.
    	if (!TravelMission.TRAVEL_TO_NAVPOINT.equals(getTravelStatus())) startTravelToNextNode(person);
    	
    	NavPoint destination = getNextNavpoint();
    	
    	// If vehicle has not reached destination and isn't broken down, travel to destination.
    	boolean reachedDestination = getVehicle().getCoordinates().equals(destination.getLocation());
    	boolean malfunction = getVehicle().getMalfunctionManager().hasMalfunction();
    	if (!reachedDestination && !malfunction) {
    		// Don't operate vehicle if person was the last operator.
    		if (person != lastOperator) {
    			// If vehicle doesn't currently have an operator, set this person as the operator.
    			if (getVehicle().getOperator() == null) {
    				try {
    					if (operateVehicleTask != null) {
    						operateVehicleTask = getOperateVehicleTask(person, operateVehicleTask.getTopPhase());
    					}
    					else operateVehicleTask = getOperateVehicleTask(person, null); 
    					assignTask(person, operateVehicleTask);
    					lastOperator = person;
    				}
    				catch (Exception e) {
    					throw new MissionException(TRAVELLING, e);
    				}
    			}
    			else {
    				// If emergency, make sure current operate vehicle task is pointed home.
    				if (!operateVehicleTask.getDestination().equals(destination.getLocation())) 
    					operateVehicleTask.setDestination(destination.getLocation());
    			}
    		}
    		else lastOperator = null;
    	}
    	
    	// If the destination has been reached, end the phase.
    	if (reachedDestination) {
    		try {
    			reachedNextNode();
    			setPhaseEnded(true);
    		}
    		catch (Exception e) {
    			throw new MissionException(getPhase(), e);
    		}
    	}
    	
    	try {
    		// Check if enough resources for remaining trip.
    		if (!hasEnoughResourcesForRemainingMission(false)) {
    			// If not, determine an emergency destination.
    			determineEmergencyDestination();
    		}
    	}
    	catch (Exception e) {
    		throw new MissionException(e.getMessage(), getPhase());
    	}
    }
    
    /**
     * Gets a new instance of an OperateVehicle task for the person.
     * @param person the person operating the vehicle.
     * @return an OperateVehicle task for the person.
     * @throws Exception if error creating OperateVehicle task.
     */
    protected abstract OperateVehicle getOperateVehicleTask(Person person, 
    		String lastOperateVehicleTaskPhase) throws Exception;
	
    /** 
     * Performs the embark from settlement phase of the mission.
     * @param person the person currently performing the mission.
     * @throws MissionException if error performing phase.
     */ 
    protected abstract void performEmbarkFromSettlementPhase(Person person) throws MissionException;
    
    /**
     * Performs the disembark to settlement phase of the mission.
     * @param person the person currently performing the mission.
     * @param disembarkSettlement the settlement to be disembarked to.
     * @throws MissionException if error performing phase.
     */
    protected abstract void performDisembarkToSettlementPhase(Person person, 
    		Settlement disembarkSettlement) throws MissionException;
    
    /**
     * Gets the estimated time of arrival (ETA) for the current leg of the mission.
     * @return time (MarsClock) or null if not applicable.
     */
    public MarsClock getLegETA() {
    	if (TRAVELLING.equals(getPhase())) 
    		return operateVehicleTask.getETA();
    	else return null;
    }
    
    /**
     * Gets the estimated time for a trip.
     * @param useBuffer use time buffers in estimation if true.
     * @param distance the distance of the trip.
     * @return time (millisols)
     * @throws Exception
     */
    public double getEstimatedTripTime(boolean useBuffer, double distance) throws Exception {
    	
    	// Determine average driving speed for all mission members.
    	double averageSpeed = getAverageVehicleSpeedForOperators();
    	double millisolsInHour = MarsClock.convertSecondsToMillisols(60D * 60D);
    	double averageSpeedMillisol = averageSpeed / millisolsInHour;
    	
    	return distance / averageSpeedMillisol;
    }
    
    /**
     * Gets the estimated time remaining for the mission.
     * @param useBuffer Use time buffer in estimations if true.
     * @return time (millisols)
     * @throws Exception
     */
    public double getEstimatedRemainingMissionTime(boolean useBuffer) throws Exception {
    	return getEstimatedTripTime(useBuffer, getTotalRemainingDistance());
    }
    
    /**
     * Gets the average operating speed of the mission vehicle for all of the mission members. 
     * @return average operating speed (km/h)
     */
    protected double getAverageVehicleSpeedForOperators() {
    	
    	double totalSpeed = 0D;
    	PersonIterator i = getPeople().iterator();
    	while (i.hasNext()) totalSpeed += getAverageVehicleSpeedForOperator(i.next());
    	
    	return totalSpeed / getPeopleNumber();
    }
    
    /**
     * Gets the average speed of a vehicle with a given person operating it.
     * @param person the vehicle operator.
     * @return average speed (km/h)
     */
    private double getAverageVehicleSpeedForOperator(Person person) {
    	return OperateVehicle.getAverageVehicleSpeed(vehicle, person);
    }
    
	/**
	 * Gets the number and amounts of resources needed for the mission.
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of amount and item resources and their Double amount or Integer number.
	 * @throws Exception if error determining needed resources.
	 */
    public Map getResourcesNeededForRemainingMission(boolean useBuffer) throws Exception {
    	return getResourcesNeededForTrip(useBuffer, getTotalRemainingDistance());
    }
    
    /**
     * Gets the number and amounts of resources needed for a trip.
     * @param useBuffer use time buffers in estimation if true.
     * @param distance the distance (km) of the trip.
     * @return map of amount and item resources and their Double amount or Integer number.
     * @throws Exception if error determining needed resources.
     */
    public Map getResourcesNeededForTrip(boolean useBuffer, double distance) throws Exception {
    	Map result = new HashMap();
    	if (vehicle != null) 
    		result.put(vehicle.getFuelType(), new Double(getFuelNeededForTrip(distance, 
    				vehicle.getFuelEfficiency(), useBuffer)));
    	return result;
    }
    
    /**
     * Checks if there are enough resources available in the vehicle for the remaining mission.
     * @param useBuffers use time buffers for estimation if true.
     * @return true if enough resources.
     * @throws Exception if error checking resources.
     */
    protected boolean hasEnoughResourcesForRemainingMission(boolean useBuffers) throws Exception {
    	return hasEnoughResources(getResourcesNeededForRemainingMission(useBuffers));
    }
    
    /**
     * Checks if there are enough resources available in the vehicle.
     * @param neededResources map of amount and item resources and their Double amount or Integer number.
     * @return true if enough resources.
     * @throws Exception if error checking resources.
     */
    private boolean hasEnoughResources(Map neededResources) throws Exception {
    	boolean result = true;
    	
        Inventory inv = vehicle.getInventory();

        Iterator iR = neededResources.keySet().iterator();
        while (iR.hasNext() && result) {
        	Resource resource = (Resource) iR.next();
        	if (resource instanceof AmountResource) {
        		double amount = ((Double) neededResources.get(resource)).doubleValue();
        		double amountStored = inv.getAmountResourceStored((AmountResource) resource);
        		if (amountStored < amount) result = false;
        	}
        	else if (resource instanceof ItemResource) {
        		int num = ((Integer) neededResources.get(resource)).intValue();
        		if (inv.getItemResourceNum((ItemResource) resource) < num) result = false;
        	}
        	else throw new Exception("Unknown resource type: " + resource);
        }
        
        return result;
    }
    
    /**
     * Determines the emergency destination settlement for the mission if one is reachable, 
     * otherwise sets the emergency beacon and ends the mission.
     * @throws Exception if error determining an emergency destination.
     */
    protected void determineEmergencyDestination() throws Exception {
    	
    	// Determine closest settlement.
    	Settlement newDestination = findClosestSettlement();
    	
    	// Check if enough resources to get to settlement.
    	double distance = getCurrentMissionLocation().getDistance(newDestination.getCoordinates());
    	if (hasEnoughResources(getResourcesNeededForTrip(false, distance))) {
    		System.out.println(vehicle.getName() + " setting emergency destination to " + newDestination.getName() + ".");
    		// Set the new destination as the travel mission's next and final navpoint.
    		clearRemainingNavpoints();
    		addNavpoint(new NavPoint(newDestination.getCoordinates(), newDestination));
    	}
    	else {
    		// Set the emergency beacon on the rover and end mission.
    		System.out.println(vehicle.getName() + " setting emergency beacon.");
    		endMission();
    	}
    }
    
    /**
     * Finds the closest settlement to the mission.
     * @return settlement
     * @throws Exception if error finding closest settlement.
     */
    private Settlement findClosestSettlement() throws Exception {
    	Settlement result = null;
    	Coordinates location = getCurrentMissionLocation();
    	double closestDistance = Double.MAX_VALUE;
    	
    	SettlementIterator i = Simulation.instance().getUnitManager().getSettlements().iterator();
    	while (i.hasNext()) {
    		Settlement settlement = i.next();
    		if (result == null) result = settlement;
    		else {
    			double distance = settlement.getCoordinates().getDistance(location);
    			if (distance < closestDistance) {
    				result = settlement;
    				closestDistance = distance;
    			}
    		}
    	}
    	
    	return result;
    }
}