package com.borzak.cncmill;

import java.awt.*;
import java.awt.Point;
import java.util.*;

import com.vividsolutions.jts.geom.*;

/**
 * Trace Pad
 * This class is used for display and calculation of isolation paths only.
 * It graphically and logically represents a pad on a printed circuit board.
 */
public class TracePad extends MillingGeometryAction {


	private int width = 0;
	private int height = 0;


	/**
	 * Create a TracePad as a rectangle of the specified size at the specifed location.
	 * @param xpos The absolute X coordinate of the center of pad.
	 * @param ypos The absolute Y coordinate of the center of pad.
	 * @param width The absolute width of the pad.
	 * @param height The absolute height of the pad.
	 */
	public TracePad(int xpos, int ypos, int width, int height) {
		super();
		this.xpos = xpos;
		this.ypos = ypos;
		this.width = width;
		this.height = height;
		primaryColor = Color.BLUE;
		}
	
	
	public MillingAction getTransformedInstance(MillingTransform transform) {
		MillLocation newLoc = transform.transform(xpos, ypos);
		TracePad newPad = new TracePad(newLoc.getX(), newLoc.getY(), width, height);
		newPad.setComplete(isComplete());
		newPad.setDisplayOnly(displayOnly);
		newPad.setSelected(isSelected());
		return newPad;
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.borzak.cncmill.GeometryAction#getGeometry()
	 */
	protected Geometry createGeometry() {
		
		Coordinate[] coordinates = new Coordinate[] {
				new Coordinate(getXpos()-width/2, getYpos()-height/2),	
				new Coordinate(getXpos()-width/2, getYpos()+height/2),	
				new Coordinate(getXpos()+width/2, getYpos()+height/2),	
				new Coordinate(getXpos()+width/2, getYpos()-height/2),	
				new Coordinate(getXpos()-width/2, getYpos()-height/2),	
		};
		
		LinearRing lr = geoFactory.createLinearRing(coordinates);
		return geoFactory.createPolygon(lr, new LinearRing[] {});
	}
	
	public boolean matches(int mask) {
		int mymask = MASK_COMBINABLE |MASK_DISPLAYONLY | MASK_GEOMETRY | MASK_TRACEPAD;
		return ( (mask & mymask) == mask );
	}

	public String toString() {
		return "TracePad(id="+getId()+" @("+xpos+","+ypos+") Size="+width+"x"+height+")";
	}
	
	
}
