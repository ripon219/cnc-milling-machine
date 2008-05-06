package com.borzak.cncmill;

import java.awt.*;
import java.awt.Point;

import com.vividsolutions.jts.geom.*;

/**
 * MillingGeometryAction
 * This abstract class represents logical geometry based actions that are typically 
 * not executable actions, but are rather view-based components that are used to do
 * high level operations on PCB Traces or Pads rather than specific milling
 * instructions.
 */
public abstract class MillingGeometryAction extends MillingAction {

	private Geometry geometry;
	protected Color primaryColor = Color.BLACK;
	Rectangle bounds = null;

	public MillingGeometryAction() {
		super();
	}
	
	
	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#drawAction(com.borzak.cncmill.SurfacePanel)
	 */
	public Rectangle drawAction(SurfacePanel surface) {
		if (surface == null) return null; // skip it if no GUI is available

		// Calculate a rectangle to draw
		Graphics g = surface.getSurfaceGraphics();
		if (g == null) return null;
		
		
		if (isSelected()) {
			g.setColor(Color.PINK);
		} else {
			g.setColor(primaryColor);
		}
		
		Coordinate[] coordinates = getGeometry().getCoordinates();
		if (coordinates.length == 0) return null;
		int x[] = new int[coordinates.length];
		int y[] = new int[coordinates.length];
		for (int i = 0; i < coordinates.length; i++) {
			Coordinate coordinate = coordinates[i];
			x[i] = (int) coordinate.x;
			y[i] = (int) coordinate.y;
			Point p = surface.drawPointFromCoordinates(new Point(x[i],y[i]));
			x[i] = p.x;
			y[i] = p.y;
		}
		
		g.fillPolygon(x,y,coordinates.length);
		
		if (surface.isShowVertices()) {
			// Mark vertices
			g.setColor(Color.CYAN);
			
			for (int i = 0; i < y.length; i++) {
				int xloc = x[i];
				int yloc = y[i];
				g.drawRect(xloc,yloc,1,1);
			}
		}
		
		
		return getBounds();
	}
	
	/* (non-Javadoc)
	 * @see com.borzak.cncmill.GeometryAction#getGeometry()
	 */
	public final Geometry getGeometry() {
		if (geometry == null) {
			geometry = createGeometry();
		}
		return geometry;
	}
	
	
	/** 
	 * Template method that should be implemented to create the appropriate
	 * Geometry instance as needed. 
	 * @return
	 */
	protected abstract Geometry createGeometry();
	

	
	public boolean containsPoint(int x, int y) {
		Coordinate coord = new Coordinate(x, y);
		com.vividsolutions.jts.geom.Point geoPoint = geoFactory.createPoint(coord);
		return getGeometry().contains(geoPoint);
	}


	public boolean isInside(int x, int y, int width, int height) {
		return getGeometry().coveredBy(
				geoFactory.createPolygon(geoFactory.createLinearRing(new Coordinate[] {
				new Coordinate((int)x,(int)y),
				new Coordinate((int)x,(int)y+height),
				new Coordinate((int)x+width,(int)y+height),
				new Coordinate((int)x+width,(int)y),
				new Coordinate((int)x,(int)y)
				}),new LinearRing[] {}));
	}


	public Rectangle getBounds() {
		if (bounds == null) {
			bounds= GeometryUtil.calculateBounds(getGeometry());
		}
		return bounds;
	}




	public MillLocation[] getCoordinates() {
		return new MillLocation[] {};
	}

}
