package com.borzak.cncmill;

import java.awt.*;

/**
 * Milled Movement Class
 * This class represents a milling machine movement with the z-axis at ZSafe (not touching the surface)
 *
 */
public class MilledMovement extends MillingAction implements Executable {

	private MillLocation[] coordinates = null;

	/**
	 * Create a MilledMovement instance that has not yet been completed.
	 * @param xpos The absolute X coordinate of the center of the start of the line
	 * @param ypos The absolute Y coordinate of the center of the start of the line
	 */
	public MilledMovement(int xpos, int ypos) {
		super(xpos, ypos);
	}
	

	public MillingAction getMirrorX() {
		MilledMovement newMovement = new MilledMovement(-getXpos(), getYpos());
		newMovement.setComplete(isComplete());
		newMovement.setDisplayOnly(displayOnly);
		newMovement.setSelected(isSelected());
		return newMovement;
	}
	
	
	/**
	 * Moves the x/y axis to the appropriate point after lifting the Z-axis to ZSafe.
	 * Does not affect the current state of the drill motor.  Ends with the  
	 * Z-axis at ZSafe. 
	 * Does not affect the vacuum.  
	 * @param mill The MillingMachine that will do the drilling.
	 * @throws MillingException If there are any problems.  If a MillingException occurs, the 
	 * drill motor and vacuum motor will be turned off.
	 */
	public void execute(MillingMachine mill) throws MillingException {

		if (displayOnly) return;
		
		try {
			
			if (xpos != mill.getXLocation() || ypos != mill.getYLocation()) {
				// If the drill is not at the start position, lift the Z-axis and move to start
				// Raise Z and Move to start location
				mill.moveTo(xpos,ypos,mill.getZSafe());
			}
			complete = true;
		} catch (MillingException e) {
			// Fail safe by turning the motors off for an error
			mill.setDrillRelay(false);
			mill.setVacuumRelay(false);
			throw e; // let the caller deal with the issues.
		}
	}


	public boolean containsPoint(int x, int y) {
		return false;
	}

	public boolean isInside(int x, int y, int width, int height) {
		return false;
	}
	
	public Rectangle getBounds() {
		return null; // not a displayable object
	}

	public boolean matches(int mask) {
		int mymask = MASK_EXECUTABLE | (displayOnly ? MASK_DISPLAYONLY : 0);
		return ( (mask & mymask) == mask );
	}

	public MillLocation[] getCoordinates() {
		if (coordinates == null) {
			coordinates = new MillLocation[] {new MillLocation(getXpos(),getYpos(),depth)};
		}
		return coordinates;
	}
	
}
