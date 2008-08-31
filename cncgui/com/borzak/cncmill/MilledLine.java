package com.borzak.cncmill;

import java.awt.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.buffer.*;

/**
 * Milled Line Class
 * This class represents a milled line that may or may not have been drilled.
 * It is used to drive the processing of a CNC drilling program, and the display of the 
 * lines on the MillingMachineFrame GUI.
 *
 */
public class MilledLine extends MillingGeometryAction implements Executable {

	private int xstart;
	private int ystart;
	private MillLocation[] coordinates = null;
	private Tool tool;

	/**
	 * Create a MilledLine instance that has not yet been cut.
	 * @param xpos The absolute X coordinate of the center of the start of the line
	 * @param ypos The absolute Y coordinate of the center of the start of the line
	 * @param xstart The absolute X coordinate of the center of the end of the line
	 * @param xend The absolute Y coordinate of the center of the end of the line
	 * @param depth The depth of the tool cut in Z-steps
	 * @param tool The tool to be used for the cut.
	 */
	public MilledLine(int xstart, int ystart, int xpos, int ypos, int depth, Tool tool) {
		super();
		this.xstart = xstart;
		this.ystart = ystart;
		this.xpos = xpos;
		this.ypos = ypos;
		this.depth = depth;
		this.toolDiameter = tool.getStepDiameter();
		this.tool = tool;
	}

	public MillingAction getTransformedInstance(MillingTransform transform) {
		MillLocation start = transform.transform(xstart,ystart);
		MillLocation pos = transform.transform(xpos,ypos);
		MilledLine newLine = new MilledLine(start.getX(), start.getY(), pos.getX(), pos.getY(), depth, tool);
		newLine.setComplete(isComplete());
		newLine.setDisplayOnly(displayOnly);
		newLine.setSelected(isSelected());
		return newLine;
	}
	
	
	public String toString() {
		return "MilledLine(id="+getId()+",From "+xstart+","+ystart+" to "+xpos+","+ypos+" diam="+getToolDiameter()+",depth="+getDepth();
	}
	
	
	/**
	 * Cuts the line on the specified machine by positioning to the start, starting the drill, 
	 * and moving the Z-Axis.  Starts and stops the drill motor as needed and moves the axis to
	 * the approriate absolute positons.  Ends with the drill in the state is was in at the start, 
	 * and the Z-axis at the cutting position. 
	 * Starts the vacuum if it is not on, but does NOT stop it.  
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
			
			if (xstart != mill.getXLocation() || ystart != mill.getYLocation()) {
				// If the drill is not at the start position, lift the Z-axis and move to start
				// Raise Z and Move to start location
				mill.moveTo(xstart,ystart,mill.getZSafe());
			}
			
			// start the drill
			if (!mill.isDrillRelay()) {
				drillWasOn = false;
				mill.setDrillRelay(true); // turn on drill
			}
			
			// Added loop to cut a max of mill.getMaxMillDepth() per pass
			
			int passDepth = depth;
			int maxPass = tool.getMaxDepth();
			if (depth <=  maxPass || maxPass == 0) { 
				mill.moveTo(xstart,ystart,depth); // postion to start
				mill.moveTo(xpos, ypos,depth); // Cut the line
			} else { // multiple passes needed
				int total = 0;
				int passes = 0;
				passDepth = maxPass;
				do {
					total -= maxPass;
					passes++;
				} while (total > maxPass); 
				passes++; 
				// this is the minimum number of passes
				if (passes%2 == 0) {
					// if its even, add one to make it odd
					passes++;
				}
				// now divide it out more evenly
				passDepth = depth / passes + 1;
				if (passDepth > maxPass) {
					passDepth = maxPass;
				}
				
				total = passDepth;
				mill.moveTo(xstart,ystart,total); // postion to start
				mill.moveTo(xpos, ypos,total); // Cut the line
				
				do {
					// even pass
					total += passDepth;
					total = total <= depth ? total : depth; // max at depth
					mill.moveTo(xstart, ystart,total); // Cut the line
					// odd pass
					total += passDepth;
					total = total <= depth ? total : depth; // max at depth
					mill.moveTo(xpos, ypos,total); // Cut the line
				} while (total < depth); 
			}
			
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
		
		if (isComplete()) {
			// drilled holes are drawn as black filled cirles
			primaryColor = Color.BLACK;
		} else {
			if (displayOnly) {
				// display only holes are shown as Light Gray open rectangles
				primaryColor = Color.GRAY;
			} else {
				// undrilled holes are shown as Dark Gray open rectangles
				primaryColor = Color.LIGHT_GRAY;
			}
		}		
		
		return super.drawAction(surface);
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.GeometryAction#getGeometry()
	 */
	protected Geometry createGeometry() {
		
		int d = getToolDiameter();
		
		// calculate the slope
		int r = d/2;
		if (r == 0) {
			r=1;
		}
		
		// New method = draw a line, then calculate a buffer around it
		Geometry line = geoFactory.createLineString(new Coordinate[] {
				new Coordinate(xstart, ystart),
				new Coordinate(xpos, ypos)
		});
		
		BufferOp bufOp = new BufferOp(line);
		bufOp.setEndCapStyle(BufferOp.CAP_ROUND);
		bufOp.setQuadrantSegments(2);
		return bufOp.getResultGeometry(r);
	}
	
	public boolean matches(int mask) {
		int mymask = MASK_EXECUTABLE | MASK_MILL | MASK_GEOMETRY | (displayOnly ? MASK_DISPLAYONLY : 0);
		return ( (mask & mymask) == mask );
	}

	public MillLocation[] getCoordinates() {
		if (coordinates == null) {
			coordinates = new MillLocation[] {new MillLocation(xstart,ystart,depth),new MillLocation(xpos,ypos,depth)};
		}
		return coordinates;
	}

	public Tool getTool() {
		return tool;
	}
	
	/**
	 * Returns a new MilledLine that starts at the end and mills to the start.
	 * This is used in optimization routines.
	 */
	public MillingAction swapEnds() {
		return new MilledLine(xpos, ypos, xstart, ystart, depth, tool);
	}

}
