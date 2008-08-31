package com.borzak.cncmill;

import java.awt.*;
import java.awt.Point;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Polygon;

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
	
	/**
	 * Create a TracePolygon from an array of coordinates even=x, odd = y 
	 */
	public TracePolygon(java.awt.Point[] points) {
		super();
		
		Coordinate[] coordinates = new Coordinate[points.length+1];
		
		for (int i = 0; i < points.length; i++) {
			Point p = points[i];
			coordinates[i] = new Coordinate(p.x,p.y);
		}
		
		coordinates[coordinates.length-1] = new Coordinate(points[0].x,points[0].y);
		LinearRing lr = geoFactory.createLinearRing(coordinates);
		this.geometry = geoFactory.createPolygon(lr, new LinearRing[] {});
		primaryColor = Color.MAGENTA;
	}
	
	public MillingAction getTransformedInstance(MillingTransform transform) {

		Coordinate[] coordinates = getGeometry().getCoordinates();
		if (coordinates.length == 0) return this;
		Coordinate[] newCoordinates = new Coordinate[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			newCoordinates[i] = transform.transform(coordinates[i]);
		}

		Geometry mirroredG = geoFactory.createPolygon(geoFactory.createLinearRing(newCoordinates), new LinearRing[] {});
		
		TracePolygon newTrace = new TracePolygon(mirroredG);
		newTrace.setComplete(isComplete());
		newTrace.setDisplayOnly(displayOnly);
		newTrace.setSelected(isSelected());
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
