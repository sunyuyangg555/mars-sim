/**
 * Mars Simulation Project
 * SoundConstants.java
 * @version 2.84 2008-04-07
 * @author Dima Stepanchuk
 */
package org.mars_sim.msp.ui.standard.sound;

/**
 * File names to sounds used in the user interface.
 */
public interface SoundConstants {
	
	// The root path for sounds.
	public final static String SOUNDS_ROOT_PATH = "sounds/";
	
	// Unit window sounds for rovers.
    public final static String SND_ROVER_MOVING = "rover_moving.ogg";
    public final static String SND_ROVER_MALFUNCTION = "rover_malfunction.ogg";
    public final static String SND_ROVER_MAINTENANCE = "rover_maintenance.ogg";
    public final static String SND_ROVER_PARKED = "";
    
    // Unit window sound for settlements.
    public final static String SND_SETTLEMENT = "";

    // Unit window sounds for people.
    // TODO: Add additional sounds for people based on activity.
    public final static String SND_PERSON_FEMALE1 = "female_person1.ogg";
    public final static String SND_PERSON_FEMALE2 = "female_person2.ogg";
    public final static String SND_PERSON_MALE1 = "male_person1.ogg";
    public final static String SND_PERSON_MALE2 = "male_person2.ogg";
    
    // Unit window sounds for equipment.
    public final static String SND_EQUIPMENT = "";
    
    // Supported sound formats
    public final static String SND_FORMAT_WAV  = ".wav";
    public final static String SND_FORMAT_MP3  = ".mp3";
    public final static String SND_FORMAT_OGG  = ".ogg";
    public final static String SND_FORMAT_MID =  ".mid";
    public final static String SND_FORMAT_MIDI = ".midi";
    
    //maximum amount of clips in cache
    public final static int MAX_CACHE_SIZE = 5;
}