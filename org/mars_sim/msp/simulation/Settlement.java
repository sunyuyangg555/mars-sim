/**
 * Mars Simulation Project
 * Settlement.java
 * @version 2.72 2001-08-14
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation;

import java.util.Vector;

/** The Settlement class represents a settlement unit on virtual Mars.
 *  It contains information related to the state of the settlement.
 */
public class Settlement extends Structure {

    // Data members
    Vector people; // List of inhabitants
    Vector vehicles; // List of parked vehicles
    FacilityManager facilityManager; // The facility manager for the settlement.

    /** Constructs a Settlement object
     *  @param name the settlement's name
     *  @param location the settlement's location
     *  @param mars the virtual Mars
     *  @param manager the settlement's unit manager
     */
    Settlement(String name, Coordinates location, VirtualMars mars, UnitManager manager) {

        // Use Unit constructor
        super(name, location, mars, manager);

        // Initialize data members
        people = new Vector();
        vehicles = new Vector();
        facilityManager = new FacilityManager(this);
    }

    /** Returns the facility manager for the settlement 
     *  @return the settlement's facility manager
     */
    public FacilityManager getFacilityManager() {
        return facilityManager;
    }

    /** Get number of inhabitants in settlement 
     *  @return the number of inhabitants
     */
    public int getPeopleNum() {
        return people.size();
    }

    /** Get number of parked vehicles in settlement 
     *  @return the number of parked vehicles
     */
    public int getVehicleNum() {
        return vehicles.size();
    }

    /** Get an inhabitant at a given vector index 
     *  @param index the inhabitant's index
     *  @return the inhabitant
     */
    public Person getPerson(int index) {
        if (index < people.size()) {
            return (Person) people.elementAt(index);
        } else {
            return null;
        }
    }

    /** Get a parked vehicle at a given vector index. 
     *  @param the vehicle's index
     *  @return the vehicle
     */
    public Vehicle getVehicle(int index) {
        if (index < vehicles.size()) {
            return (Vehicle) vehicles.elementAt(index);
        } else {
            return null;
        }
    }

    /** Determines if given person is an inhabitant of this settlement.
     *  @return true if person is an inhabitant of this settlement
     */
    public boolean isInhabitant(Person person) {
        boolean result = false;
        for (int x=0; x < people.size(); x++) {
            if (people.contains(person)) result = true;
        }
        return result;
    } 

    /** Bring in a new inhabitant 
     *  @param newPerson the new person
     */
    public void addPerson(Person newPerson) {
        if (!isInhabitant(newPerson)) people.addElement(newPerson);
    }

    /** Make a given inhabitant leave the settlement 
     *  @param person the person leaving
     */
    public void personLeave(Person person) {
        if (people.contains(person)) {
            people.removeElement(person);
        }
    }

    /** Bring in a new vehicle to be parked 
     *  @param newVehicle the new vehicle
     */
    public void addVehicle(Vehicle newVehicle) {
        if (!vehicles.contains(newVehicle)) vehicles.addElement(newVehicle);
    }

    /** Make a given vehicle leave the settlement 
     *  @param vehicle the vehicle leaving
     */
    public void vehicleLeave(Vehicle vehicle) {
        if (vehicles.contains(vehicle)) vehicles.removeElement(vehicle);
    }

    /** Perform time-related processes 
     *  @param time the amount of time passing (in millisols)
     */
    void timePassing(double time) {
        facilityManager.timePassing(time);
    }
}
