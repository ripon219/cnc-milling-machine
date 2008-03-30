package com.borzak.cncmill;

import java.awt.*;

import com.vividsolutions.jts.geom.*;

/**
 * Milled Movement Class
 * This class implements the raw methods required for all MillingAction implementations
 *
 */
public abstract class MillingAction implements MaskConstants {
	
	private static int lastId = 0;
	private int id;
	protected int xpos;
	protected int ypos;
	protected int toolDiameter = 0;
	protected int depth = 0;
	protected boolean displayOnly = false;
	
	protected boolean complete = false;
	protected boolean selected = false;

	public static GeometryFactory geoFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FIXED));
	
	public MillingAction() {
		super();
		id = lastId++;
	}
	
	/**
	 * Create a MilledMovement instance that has not yet been completed.
	 * @param xpos The absolute X coordinate of the center of the start of the line
	 * @param ypos The absolute Y coordinate of the center of the start of the line
	 */
	public MillingAction(int xpos, int ypos) {
		this();
		this.xpos = xpos;
		this.ypos = ypos;
	}
	
	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#isComplete()
	 */
	public boolean isComplete() {
		return complete;
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#setComplete(boolean)
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#getDepth()
	 */
	public int getDepth() {
		return depth;
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#getXpos()
	 */
	public int getXpos() {
		return xpos;
	}

	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#getYpos()
	 */
	public int getYpos() {
		return ypos;
	}


	/* (non-Javadoc)
	 * @see com.borzak.cncmill.MillingAction#drawAction(com.borzak.cncmill.SurfacePanel)
	 */
	public Rectangle drawAction(SurfacePanel surface) {
		return null;
	}

	public int getToolDiameter() {
		return toolDiameter; // not really relevant for this action
	}


	public void setDisplayOnly(boolean displayOnly) {
		this.displayOnly = displayOnly;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * Returns true if the object represented by this instance
	 * contains the specified point.  This is used to handle selection
	 * of elements using the mouse.  
	 */
	public abstract boolean containsPoint(int x, int y);

	public abstract boolean isInside(int x, int y, int width, int height);

	public int getId() {
		return id;
	}

	public int hashCode() {
		return id;
	}

	public String toString() {
		return ""+getClass().getName()+"(id="+id+",@"+getXpos()+","+getYpos()+"diam="+getToolDiameter()+",depth="+getDepth();
	}

	public abstract boolean matches(int mask);

	public abstract Rectangle getBounds();

	public abstract MillLocation[] getCoordinates();

	public Tool getTool() {
		return null;
	}

	public MillLocation getEndPoint() {
		return new MillLocation(getXpos(), getYpos(), getDepth());
	}

	/**
	 * Creates another instance of this that is mirrored around the X-axis
	 * (aka x * -1).
\	 */
	public abstract MillingAction getMirrorX();
	
}
