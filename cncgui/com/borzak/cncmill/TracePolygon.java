package com.borzak.cncmill;

import java.awt.*;

import com.vividsolutions.jts.geom.*;

/**
 * TracePolygon 
 * This class is used for display and calculation of isolation paths only.
 * It graphically and logically represents a calculated "simple" polygon that
 * is created an manipulated by the Java Topology Suite..
 */
public class TracePolygon extends MillingGeometryAction {

	protected Geometry geometry = null;
	
	/**
	 * Create a TracePolygon from a JTS Polygon. 
	 */
	public TracePolygon(Geometry geometry) {
		super();
		this.geometry = geometry;
		primaryColor = Color.MAGENTA;
	}
	
	
	public MillingAction getMirrorX() {

		Coordinate[] coordinates = getGeometry().getCoordinates();
		if (coordinates.length == 0) return this;
		Coordinate[] newCoordinates = new Coordinate[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			Coordinate coordinate = coordinates[i];
			newCoordinates[i] = new Coordinate(-coordinate.x, coordinate.y, coordinate.z);
		}

		Geometry mirroredG = geoFactory.createPolygon(geoFactory.createLinearRing(newCoordinates), new LinearRing[] {});
		
		TracePolygon newTrace = new TracePolygon(mirroredG);
		newTrace.setComplete(isComplete());
		newTrace.setDisplayOnly(displayOnly);
		newTrace.setSelected(isSelected());
		newTrace.setShowVertices(isShowVertices());
		return newTrace;
	}
	
	
	protected Geometry createGeometry() {
		return geometry;
	}

	public boolean matches(int mask) {
		int mymask = MASK_TRACEPOLYGON | MASK_TRACE | MASK_COMBINABLE | MASK_DISPLAYONLY | MASK_GEOMETRY;
		return ( (mask & mymask) == mask );
	}

	public String toString() {
		return "TracePolygon(id="+getId()+" "+getGeometry().toText()+")";

	}
		
	
}
