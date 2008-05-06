/*
 * Created on Dec 20, 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

import org.apache.commons.logging.*;

import com.borzak.cncmill.MillingMachineFrame.*;

public class SurfacePanel extends JPanel {
	private static Log log = LogFactory.getLog(SurfacePanel.class);
	int yHeight = 1828;
	int xWidth = 2543;
	BufferedImage internalImage = null;
	private int xCenter = xWidth/2;
	private int yCenter = yHeight/2;
	private Point millPoint = null;
	private Rectangle selectRectangle = null;
	private AffineTransform transform = new AffineTransform();
	private JScrollPane surfaceScroll;
	MillingActionList actions = null;
	MillingProperties properties = null;
	
	private class ViewportDragListener extends MouseAdapter implements MouseMotionListener {
		Point dragOrigin = null;
		Point selectOrigin = null;

		public void mousePressed(MouseEvent evt) {
			if (dragOrigin == null && evt.getButton() == MouseEvent.BUTTON3) {
				dragOrigin = evt.getPoint(); 
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
			if (dragOrigin == null && evt.getButton() == MouseEvent.BUTTON1) {
				selectOrigin = evt.getPoint(); 
			}

		}

		public void mouseReleased(MouseEvent evt) {
			if (dragOrigin != null && evt.getButton() == MouseEvent.BUTTON3) {
				// Shift the view based on offset from the original position
				Point dropPoint = evt.getPoint();
				int xDiff =  dragOrigin.x - dropPoint.x;
				int yDiff = dragOrigin.y - dropPoint.y ;
				JViewport view = surfaceScroll.getViewport();
				Point viewPos = view.getViewPosition();
				viewPos.translate(xDiff,yDiff);
				view.setViewPosition(viewPos);
				dragOrigin = null;
				setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			}
			dragOrigin = null;
			selectOrigin = null;
			clearSelectRectangle();
		}

		public void mouseDragged(MouseEvent evt) {
			if (dragOrigin != null) {
				// Shift the view based on offset from the original position
				Point dropPoint = evt.getPoint();
				int xDiff =  dragOrigin.x - dropPoint.x;
				int yDiff = dragOrigin.y - dropPoint.y ;
				JViewport view = surfaceScroll.getViewport();
				Point viewPos = view.getViewPosition();
				viewPos.translate(xDiff,yDiff);
				// Check bounds
				setViewPosition(viewPos);
			}
			if (selectOrigin != null) {
				// Store a selection rectangle
				Point p = evt.getPoint();
				int minx = selectOrigin.x < p.x ? selectOrigin.x : p.x;
				int displayminy = selectOrigin.y < p.y ? selectOrigin.y : p.y; 
				int miny = selectOrigin.y > p.y ? selectOrigin.y : p.y; 
				int width = Math.abs(selectOrigin.x - p.x); 
				int height = Math.abs(selectOrigin.y - p.y);
				p = coordinatesFromScreenPoint(new Point(minx, miny));

				try {
					Point2D dim = transform.inverseTransform(new Point(width, height),null);

					setSelectRectangle(minx, displayminy, width, height);
					
					actions.selectRectangle(p.x,p.y, (int) (dim.getX()), (int) (dim.getY()));
				} catch (NoninvertibleTransformException e) {
					log.error("in ViewportDragListener.mouseDragged", e);
				}
				
			}
		}

		public void mouseMoved(MouseEvent evt) {
		}

		public void mouseClicked(MouseEvent evt) {
			if (evt.getButton() == MouseEvent.BUTTON1) {
				// Single click Button 1 toggles select on the current Element
				// First Deselect all traces (unless ctrl is pressed)
				// Then select the trace that is pointed to.
				
				if (!evt.isControlDown() && !evt.isAltDown()) {
					actions.clearSelections();
				}
				Point p = coordinatesFromScreenPoint(evt.getPoint());
				actions.toggleElement(p.x, p.y, !evt.isAltDown());
			}
		}
	}
	
	
	public SurfacePanel() {
		super();
		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		transform.setToIdentity();
		surfaceScroll = new JScrollPane(this,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		surfaceScroll.setPreferredSize(new Dimension(500, 500));
		ViewportDragListener listener = new ViewportDragListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);

	}
	
	
	public JScrollPane getScrollPane() {
		return surfaceScroll;
	}

	
	public void setScale(double scale) {
		// First get the center of the viewport in the current scale
		
		Rectangle viewRect = surfaceScroll.getViewport().getViewRect();
		
		Point center = new Point(viewRect.x+viewRect.width/2, viewRect.y+viewRect.height/2);
		Point point = coordinatesFromScreenPoint(center); 
		
		transform.setToScale(scale,scale);
		invalidate();
		validateTree();
		surfaceScroll.invalidate();
		surfaceScroll.validate();
		
		centerView(point);
	}
	
	protected BufferedImage getInternalImage() {
		if (internalImage == null) {
//			internalImage = createImage(xWidth,yHeight);
			internalImage = new BufferedImage(xWidth, yHeight, BufferedImage.TYPE_INT_RGB);
			// Put a line down the screen
			clear();
		} 
		return internalImage;
	}

	private void paintMarker(Color color, int x, int y, Graphics g) {
		Color saveColor;
		saveColor = g.getColor();
		g.setColor(color);
		g.drawLine(x-3,y-3,x+3,y+3);
		g.drawLine(x+3,y-3,x-3,y+3);
		g.setColor(saveColor);
	}

	/**
	 * Draws the center and mill position icons
	 */
	private void paintPositions(Graphics g) {
		// Draw red crosshairs at center
		paintMarker(Color.RED, xCenter, yCenter, g); 
		
		// Draw blue crosshairs at the current mill location
		if (millPoint != null) {
			paintMarker(Color.BLUE, millPoint.x, millPoint.y, g);
		}
	}
	
	
	
	/**
	 * Converts a coordinate in the milling machine coordinates to a point in the panel
	 * @param coordinates - the location in the milling machine coordinate system
	 * @return Point - The point location on the screen
	 */
	public Point drawPointFromCoordinates(Point coordinates) {
		
		Point screen = new Point(coordinates);
		screen.setLocation(screen.getX(),-screen.getY()); 
		screen.translate(xCenter,yCenter);
		return screen;
	}

	/**
	 * Converts a point on the panel to the milling machine coordinates.
	 * @param screen - The location in the panel
	 * @return Point - The milling machine coordinate.
	 */
	public Point coordinatesFromScreenPoint(Point screen) {
		
		Point coordinates = new Point(screen);
		try {
			transform.createInverse().transform(coordinates,coordinates);
		} catch (NoninvertibleTransformException e) {
			log.error("in SurfacePanel.coordinatesFromScreenPoint", e);
		}
		coordinates.translate(-xCenter,-yCenter);
		
		coordinates.setLocation(coordinates.getX(),-coordinates.getY()); 
		
		return coordinates;
	}
	
	
	

	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		AffineTransform saveAF = g2d.getTransform();
		g2d.transform(transform);
		g2d.drawImage( getInternalImage(), 0, 0,this );
		paintPositions(g);
		g2d.setTransform(saveAF);
		if (selectRectangle != null) {
			g2d.setXORMode(Color.WHITE);
			g2d.drawRect(selectRectangle.x, selectRectangle.y, selectRectangle.width, selectRectangle.height);
			g2d.setPaintMode();
		}
		
	}

	public Dimension getMinimumSize() {
		
		// Adjust the dimensions for the scroll pane
		Point2D p = new Point(xWidth, yHeight);
		p = transform.transform(p, p);
		return new Dimension((int)p.getX(),(int)p.getY());
	}

	public Dimension getPreferredSize() {
		return getMinimumSize();
	}

	public void setMillPosition(int x, int y) {
		millPoint = drawPointFromCoordinates(new Point(x,y));
	}
	

	public boolean isVisible(Point p) {
		Rectangle visible = getVisibleRect();
		Point2D visiblePoint = transform.transform(p,null);
		return visible.contains(visiblePoint);
		}
	
	public void drawAction(MillingAction action) {
		// Draws the specified action
		Graphics g = getSurfaceGraphics();
		if (g == null) return;
		Color saveColor;
		saveColor = g.getColor();

		Rectangle rect = action.drawAction(this);
		g.setColor(saveColor);
		if (rect != null) {
			repaint(rect);
		}
	}
	
	
	public Graphics getSurfaceGraphics() {
		
		if (internalImage == null) return null;
		Graphics2D g = (Graphics2D) internalImage.getGraphics();
		return g;
	}

	public void clear() {
		Graphics g = getSurfaceGraphics();
		if (g == null) return;
		g.setColor(getBackground());
		g.fillRect(0,0,xWidth,yHeight);
		this.repaint();
	}

	public void setSelectRectangle(int minx, int miny, int width, int height) {
		selectRectangle = new Rectangle(minx, miny, width, height);
		repaint();
	}

	public void clearSelectRectangle() {
		selectRectangle = null;
		repaint();
		
	}
		
	/**
	 * Centers the view on a particular logical coordinate
	 * @param point The mill surface coordinate to center on
	 * 
	 */
	public void centerView(Point point) {
		Dimension size = surfaceScroll.getViewport().getSize();
		Point center = drawPointFromCoordinates(point);
		transform.transform(center,center);
		center.translate(-size.width/2,-size.height/2);
		if (center.x < 0) {
			center.x = 0;
		}
		if (center.y < 0) {
			center.y = 0;
		}
		surfaceScroll.getViewport().setViewPosition(center);
	}


	public MillingActionList getActions() {
		return actions;
	}


	public void setActions(MillingActionList actions) {
		this.actions = actions;
	}

	public void setViewPosition(Point viewPos) {
		// TODO this does not always work correctly
		transform.transform(viewPos, viewPos);
		if (viewPos.x < 0) {
			viewPos.x = 0;
		}
		if (viewPos.y < 0) {
			viewPos.y = 0;
		}
		if (viewPos.x > xWidth) {
			viewPos.x = xWidth;
		}
		if (viewPos.y > yHeight) {
			viewPos.y = yHeight;
		}
		surfaceScroll.getViewport().setViewPosition(viewPos);
	}


	public boolean isShowVertices() {
		return properties.isShowVertices();
	}


	public MillingProperties getProperties() {
		return properties;
	}


	public void setProperties(MillingProperties properties) {
		this.properties = properties;
	}

	
}
