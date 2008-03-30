package com.borzak.cncmill;

import java.awt.*;


/**
 * Drilled Hole Class
 * This class represents a hole that may or may not have been drilled.
 * It is used to drive the processing of a CNC drilling program, and the display of the 
 * holes on the MillingMachineFrame GUI.  This class is used for holes made with a 
 * traditional drilling process rather than a larger hole that is made by milling with 
 * a smaller tool.  
 *
 */
public class DrilledHole extends MillingAction implements Executable  {
	
	private Rectangle bounds = null;
	private MillLocation[] coordinates = null;
	private Tool tool=null;

	/**
	 * Create a Drilled instance that has not yet been drilled.
	 * @param xpos The absolute X coordinate of the center of the hole
	 * @param ypos The absolute Y coordinate of the center of the hole
	 * @param depth The depth of the hole in Z-steps
	 * @param toolDiameter The diamter of the hole in x/y Steps - this determines the tool to use.
	 */
	public DrilledHole(int xpos, int ypos, int depth, int toolDiameter) {
		super();
		this.xpos = xpos;
		this.ypos = ypos;
		this.depth = depth;
		this.tool = new Tool(Tool.DRILL,toolDiameter);
		this.toolDiameter = toolDiameter;
	}
	
	public DrilledHole(int xpos, int ypos, int depth, Tool tool) {
		this.xpos = xpos;
		this.ypos = ypos;
		this.depth = depth;
		this.tool = tool;
		this.toolDiameter = tool.getStepDiameter();
	}

	public MillingAction getMirrorX() {
		DrilledHole newHole = new DrilledHole(-xpos,ypos,depth,toolDiameter);
		newHole.setComplete(isComplete());
		newHole.setDisplayOnly(displayOnly);
		newHole.setSelected(isSelected());
		return newHole;
	}
	
	
	public String toString() {
		return "DrilledHole(id="+getId()+",@"+getXpos()+","+getYpos()+" tool="+tool+" diam="+getToolDiameter()+",depth="+getDepth();
	}
	
	
	/**
	 * Drills the hole on the specified machine by positioning to the hole, starting the drill, 
	 * and moving the Z-Axis.  Starts and stops the drill motor as needed and moves the axis to
	 * the approriate absolute positons.  Ends with the drill off and the Z-axis withdrawn to -10 
	 * steps.  Starts the vacuum if it is not on, but does NOT stop it.  Returns with the drill 
	 * motor in the state it was on starting (allowing the caller to decide if the motor should be 
	 * switched on and off, or just left on for the whole process.
	 * @param mill The MillingMachine that will do the drilling.
	 * @throws MillingException If there are any problems.  If a MillingException occurs, the 
	 * drill motor and vacuum motor will be turned off.
	 */
	public void execute(MillingMachine mill) throws MillingException {

		if (displayOnly) return;
		
		boolean drillWasOn = true;
		
		try {
			// Make sure the vacuum is on
			if (!mill.isVacuumRelay()) {
				mill.setVacuumRelay(true);
			}
			// Drill the hole
			// Raise Z and Move to hole location
			mill.moveTo(xpos,ypos,-10);
			mill.setXHold(true);
			mill.setYHold(true);
			
			if (!mill.isDrillRelay()) {
				drillWasOn = false;
				mill.setDrillRelay(true); // turn on drill
			}
			mill.moveTo(xpos,ypos,0); // Position the drill
			try {
				Thread.sleep(50);  // let it settle
			} catch (InterruptedException e) {
				// ignore the exception
			} 
			mill.moveOffset(0,0,depth); // Drill it
			mill.moveOffset(0,0,0-depth); // remove drill
			mill.setXHold(false);
			mill.setYHold(false);

			if (!drillWasOn) {
				mill.setDrillRelay(false); // turn off drill
			}
			complete = true;
		} catch (MillingException e) {
			// Fail safe by turning the motors off for an error
			mill.setDrillRelay(false);
			mill.setVacuumRelay(false);
			throw e; // let the caller deal with the issues.
		}
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#drawAction(com.borzak.cncmill.SurfacePanel)
	 */
	public Rectangle drawAction(SurfacePanel surface) {
		if (surface == null) return null; // skip it if no GUI is available
		Point center = surface.drawPointFromCoordinates(new Point(getXpos(), getYpos()));
		Graphics g = surface.getSurfaceGraphics();
		if (g == null) return null;
		int d = getToolDiameter();
		
		// calulate the bounding rectangle
		int x = center.x - d/2;
		int y = center.y - d/2;
		
		if (isComplete()) {
			// drilled holes are drawn as black filled cirles
			g.setColor(Color.BLACK);
			if (isSelected()) {
				g.setColor(Color.PINK);
			}
			g.fillOval(x,y,d,d);
		} else {
			if (displayOnly) {
				// undrilled holes are shown as Dark Gray open circles
				g.setColor(Color.GRAY);
			} else {
				// undrilled holes are shown as Dark Gray open circles
				g.setColor(Color.DARK_GRAY);
			}
			if (isSelected()) {
				g.setColor(Color.PINK);
			}
			g.drawOval(x,y,d,d);
			g.drawLine(x,y+d/2,x+d,y+d/2);
			g.drawLine(x+d/2,y,x+d/2,y+d);
		}
		return new Rectangle(x-d,y-d,d*2,d*2);
	}


	public boolean containsPoint(int x, int y) {
		return getBounds().contains(x,y);
	}
	
	public Rectangle getBounds() {
		if (bounds == null) {
			int d = getToolDiameter();
			int r = d/2;
			if (r < 1) {
				r = 1;
			}
			bounds = new Rectangle(getXpos()-r,getYpos()-r,d,d);
		}
		return bounds;
	}
	
	public boolean isInside(int x, int y, int width, int height) {
		Rectangle rect = new Rectangle(x,y,width,height);
		return rect.contains(getBounds());

	}

	public boolean matches(int mask) {
		int mymask = MASK_DRILL | MASK_EXECUTABLE | (displayOnly ? MASK_DISPLAYONLY : 0);
		return ( (mask & mymask) == mask );
	}


	public MillLocation[] getCoordinates() {
		if (coordinates == null) {
			coordinates = new MillLocation[] {new MillLocation(getXpos(),getYpos(),depth)};
		}
		return coordinates;
	}
	
	public Tool getTool() {
		return tool;
	}

}
