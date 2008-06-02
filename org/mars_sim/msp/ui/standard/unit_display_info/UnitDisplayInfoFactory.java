/**
 * Mars Simulation Project
 * UnitDisplayInfoFactory.java
 * @version 2.84 2008-06-01
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard.unit_display_info;

import org.mars_sim.msp.simulation.Unit;
import org.mars_sim.msp.simulation.equipment.Equipment;
import org.mars_sim.msp.simulation.person.Person;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.simulation.vehicle.Rover;
import org.mars_sim.msp.simulation.vehicle.Vehicle;

/**
 * Factory for unit display info beans.
 */
public final class UnitDisplayInfoFactory {
    
    // Static bean instances.
    private static UnitDisplayInfo settlementBean = new SettlementDisplayInfoBean();
    private static UnitDisplayInfo personBean = new PersonDisplayInfoBean();
    private static UnitDisplayInfo roverBean = new RoverDisplayInfoBean();
    private static UnitDisplayInfo explorerRoverBean = new ExplorerRoverDisplayInfoBean();
	private static UnitDisplayInfo transportRoverBean = new TransportRoverDisplayInfoBean();
	private static UnitDisplayInfo cargoRoverBean = new CargoRoverDisplayInfoBean();
	private static UnitDisplayInfo luvBean = new LUVDisplayInfoBean();
    private static UnitDisplayInfo equipmentBean = new EquipmentDisplayInfoBean();
    
    /**
     * Private constructor
     */
    private UnitDisplayInfoFactory() {}
    
    /**
     * Gets a display information about a given unit.
     * @param unit the unit to display.
     * @return unit display info instance.
     */
    public static UnitDisplayInfo getUnitDisplayInfo(Unit unit) {
        if (unit instanceof Settlement) return settlementBean;
        else if (unit instanceof Person) return personBean;
        else if (unit instanceof Vehicle) {
        	if (unit instanceof Rover) {
        		if (unit.getDescription().equals("Explorer Rover")) return explorerRoverBean;
        		else if (unit.getDescription().equals("Transport Rover")) return transportRoverBean;
        		else if (unit.getDescription().equals("Cargo Rover")) return cargoRoverBean;
        		else return roverBean;
        	}
        	else if (unit instanceof LightUtilityVehicle) return luvBean;
        	else return null;
        }
        else if (unit instanceof Equipment) return equipmentBean;
        else return null;
    }
}
