package com.borzak.cncmill;

import java.awt.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.operation.buffer.*;


/**
 * Trace Polygon class
 * This class is used for display and calculation of isolation paths only.
 * It graphically and logically represents a trace on a printed circuit board.
 *
 */
public class TraceSegment extends MillingGeometryAction {

	protected int xstart;
	protected int ystart;

	/**
	 * Create a MilledLine instance that has not yet been cut.
	 * @param xpos The absolute X coordinate of the center of the start of the line
	 * @param ypos The absolute Y coordinate of the center of the start of the line
	 * @param xstart The absolute X coordinate of the center of the end of the line
	 * @param xend The absolute Y coordinate of the center of the end of the line
	 * @param toolDiameter The diameter of the hole in x/y Steps - this determines the tool to use.
	 */
	public TraceSegment(int xstart, int ystart, int xpos, int ypos, int toolDiameter) {
		super();
		this.xstart = xstart;
		this.ystart = ystart;
		this.xpos = xpos;
		this.ypos = ypos;
		this.toolDiameter = toolDiameter;
		primaryColor = Color.RED;
	}
	
	public MillingAction getMirrorX() {

		TraceSegment newTrace = new TraceSegment(-xstart, ystart, -xpos, ypos, toolDiameter);
		newTrace.setComplete(isComplete());
		newTrace.setDisplayOnly(displayOnly);
		newTrace.setSelected(isSelected());
		return newTrace;
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
		int mymask = MASK_TRACESEGMENT | MASK_COMBINABLE | MASK_DISPLAYONLY | MASK_GEOMETRY;
		return ( (mask & mymask) == mask );
	}

	public String toString() {
		return "TraceSegment(id="+getId()+" ("+xstart+","+ystart+") to ("+xpos+","+ypos+") @Width="+toolDiameter+")";
	}
		
}
