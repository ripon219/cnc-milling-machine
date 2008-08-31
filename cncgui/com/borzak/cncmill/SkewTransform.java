/*
 * Created on Jul 6, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import com.vividsolutions.jts.geom.*;

public class SkewTransform implements MillingTransform {
	MillLocation origin = null;
	double xFactor;
	double yFactor;

	public SkewTransform(MillLocation origin, double xFactor, double yFactor) {
		super();
		this.origin = origin;
		this.xFactor = xFactor;
		this.yFactor = yFactor;
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingTransform#transform(int, int)
	 */
	public MillLocation transform(int xpos, int ypos) {
		int xDiff = xpos - origin.getX();
		int yDiff = ypos - origin.getY();
		
		int xOffset = (int) (xFactor * yDiff);
		int yOffset = (int) (yFactor * xDiff);
		
		return new MillLocation(xpos-xOffset, ypos-yOffset,0);
	}

	public Coordinate transform(Coordinate coordinate) {
		int xDiff = (int)(coordinate.x - origin.getX());
		int yDiff = (int)(coordinate.y - origin.getY());
		
		double xOffset = xFactor * yDiff;
		double yOffset = yFactor * xDiff;
		
		return new Coordinate(coordinate.x-xOffset,coordinate.y-yOffset,coordinate.z);
	}
}
