/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.commons.logging.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.*;

public class RasterizeInteriorCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(RasterizeInteriorCommand.class);

	private int depth = 2;

	private JTextField depthField;

	private JPanel extraPanel;

	private Tool tool;

	
	public RasterizeInteriorCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar) {
		super("Rasterize Interior", actionsList, statusBar);

		extraPanel = new JPanel();
		extraPanel.setLayout(new GridLayout(2,0));
		extraPanel.add(new JLabel("Depth (Steps)"));
		
		depthField = new JTextField("2");
		extraPanel.add(depthField);
	}
	
	protected void showDialog() {
		tool = actionsList.showToolSelection("Select Rasterization Parameters",extraPanel);

		depth = Integer.parseInt(depthField.getText());
	}

	protected void execute() {
		List list = actionsList.getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		list.addAll(actionsList.getSelectedList(MASK_DRILL));
		statusBar.setMessage("Rasterizing "+list.size()+" actions");
		statusBar.startProgress(list.size()+1);
	
		List newActions = new LinkedList();
		List removeActions = new LinkedList();
		
		// Now simplify all the traces
		log.debug("Rasterize Interior");
		
		int progressCount=0;
		GeometryFactory gf = MillingAction.geoFactory;
	
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			
			Object o = iter.next();
			Geometry g = null;
			Object removeAction = null;
			if (o instanceof MillingGeometryAction) {
				MillingGeometryAction a = (MillingGeometryAction) o;
				g = a.getGeometry();
			} else if (o instanceof DrilledHole) {
				DrilledHole hole = (DrilledHole) o;
				if (hole.getToolDiameter() > tool.getStepDiameter() ) {
					g = gf.createPoint(new Coordinate(hole.getXpos(), hole.getYpos()));
					g = g.buffer(hole.getToolDiameter()/2,3,BufferOp.CAP_ROUND);
					removeAction = hole;
				} else {
					log.info("Skipping hole smaller than tool size: "+hole);
					g = null;
				}
			}
			
			// Rasterize the interior of this geometry
			
			// If it has holes, rasterize the holes
			if (g != null && g instanceof Polygon) {
				Polygon p = (Polygon) g;
				int holes = p.getNumInteriorRing();
				if (holes == 0) {
					rasterizePolygon(newActions, p);
				} else {
					for (int n=0; n < holes; n++) {
						LinearRing lr = gf.createLinearRing(p.getInteriorRingN(n).getCoordinates());
						Polygon ip = gf.createPolygon(lr, new LinearRing[] {});
						rasterizePolygon(newActions, ip);
						if (removeAction != null) {
							removeActions.add(removeAction);
						}
					}
				}
				
			}
			statusBar.incrementProgress(progressCount++,list.size());
		}
		
		// remove anything that needs to be removed
		actionsList.removeAll(removeActions);
		// Add the new traces
		actionsList.addAll(newActions);
		statusBar.stopProgress();
		statusBar.setMessage("Completed Rasterizing "+list.size()+" actions");
	}

	void rasterizePolygon(List newActions, Geometry g) {
		GeometryFactory gf = MillingAction.geoFactory;
		Polygon p = (Polygon) g;
		int diameter = tool.getStepDiameter();
		int radius = diameter/2;
		Rectangle r = GeometryUtil.calculateBounds(p);
		if (r == null) {
			MillingActionList.log.info("calculateBounds returned null!");
		} else {
			// Step though the hole in tool diameter steps
			for (int y=r.y+(diameter/2); y < ((r.y+r.height)-diameter/2); y+=diameter) {
				int startx = 0; 
				int endx = 0;
				boolean started = false;
				for (int x=r.x+(diameter/2); x < ((r.x+r.width)-diameter/2); x+=1) {
					Point pt = gf.createPoint(new Coordinate((int)x,(int)y));
					
					if (p.intersects(pt)) {
						if (!started) {
							startx = x;
							endx = x;
							started = true;
						} else {
							// save the last point
							endx = x;
						}
					}
				}
				// whatever is in endpt is the end
				newActions.add(new MilledLine(startx, y, endx, y, depth, tool));
			}
			
			// Finally, make a pass on the interior border
			g=g.buffer(-radius,0,BufferOp.CAP_BUTT); 
			Coordinate c[] = g.getCoordinates();
			for (int i = 1; i < c.length; i++) {
				Coordinate start = c[i-1];
				Coordinate end = c[i];
				newActions.add(new MilledLine((int)start.x, (int)start.y, (int)end.x, (int)end.y, depth, tool));
			}
		}
	}

}
