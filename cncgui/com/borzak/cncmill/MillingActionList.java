package com.borzak.cncmill;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.commons.logging.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.linearref.*;
import com.vividsolutions.jts.operation.buffer.*;
import com.vividsolutions.jts.simplify.*;
import com.vividsolutions.jts.util.*;

/**
 * MillingActionList
 * 
 *  Encapsulates all activities related to processing a list of actions.
 * 
 * @author Vincent Greene
 *
 */
public class MillingActionList implements MaskConstants {
	private static Log log = LogFactory.getLog(MillingActionList.class);
	
	PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	List actionsList = new ArrayList();
	int currentAction = 0;
	SurfacePanel surface = null;
	int selectionMask = 0;
	List undoStack = new LinkedList();

	Rectangle bounds = null;
	
	private int selectionCount = -1;
	
	
	public void saveUndoBuffer() {
		List newList = new ArrayList();
		newList.addAll(actionsList);
		undoStack.add(0,newList);
		if (undoStack.size() > 5) {
			undoStack.remove(5);
		}
	}
	
	public boolean isUndoable() {
		return !undoStack.isEmpty();
	}
	
	public boolean undo() {
		if (!isUndoable()) return false;
		actionsList = (List) undoStack.get(0);
		undoStack.remove(0);
		drawAll();
		surface.repaint();
		return true;
	}
	
	
	public Rectangle getBounds() {
		if (bounds == null) {
			//recalculate it if actions have changed
			Rectangle newBounds = new Rectangle();
			for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
				MillingAction action = (MillingAction) iter.next();
				Rectangle abounds = action.getBounds();
				if (abounds != null) {
					newBounds.add(abounds);
				}
			}
			bounds = newBounds;
		}
		return bounds;
	}
	
	
	public int getSelectedCount() {
		if (selectionCount == -1) {
			// recalculate it if it is set to -1
			selectionCount = 0;
			for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
				MillingAction action = (MillingAction) iter.next();
				if (action.isSelected()) {
					selectionCount++;
				}
			}
		}
		return selectionCount;
	}
	
	public int getTotalCount() {
		return actionsList.size();
	}
	
	
	public void clearSelections() {
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			action.setSelected(false);
		}
		drawAll();
		fireSelectionChanged();
	}
	
	
	/**
	 * Highlights the current trace as the selected trace 
	 * @param point The location in the coordinate system of the actions
	 */
	public void selectElement(int x, int y) {
		
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			if (action.containsPoint(x, y)  && action.matches(selectionMask)) {
					action.setSelected(true);
					surface.drawAction(action);
				}
			}
		fireSelectionChanged();
	}

	/**
	 * Toggles the select of the current trace. 
	 * @param point The location in the coordinate system of the actions
	 * @param selectAll true selects all matching actions, false selects only one
	 */
	public void toggleElement(int x, int y, boolean selectAll) {
		
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			if (action.containsPoint(x, y)  && action.matches(selectionMask)) {
				if (action.isSelected()) {
					action.setSelected(false);
					surface.drawAction(action);
				} else {
					action.setSelected(true);
					surface.drawAction(action);
					if (!selectAll) {
						break;  // select only the first unselected match
					}
				}
			}
		}
		fireSelectionChanged();
	}

	public void drawAll() {
		if (surface == null) return;
		if (SwingUtilities.isEventDispatchThread()) {
			internalDrawAll();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					internalDrawAll();
				}
			});
		}
		
		internalDrawAll();
	}
	
	private void internalDrawAll() {
		surface.clear();
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			action.drawAction(surface);
		}
	}
	
	public void deleteSelected() {
		List l = getSelectedList(0);
		saveUndoBuffer();
		removeAll(l);
		fireSelectionChanged();
		fireCountChanged();
		drawAll();
	}
	
	protected void isolateTraces(int distance) {
		int depth = 2;
		
		List list = getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		
		// Now buffer all the traces
		int radius = distance/2;
		
		log.debug("Calculating buffers at distance "+distance);

		List newActions = new ArrayList();
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingGeometryAction a = (MillingGeometryAction) iter.next();
			Geometry g = a.getGeometry();
			
			g = g.buffer(radius,0,BufferOp.CAP_BUTT); 
			Coordinate c[] = g.getCoordinates();
			for (int i = 1; i < c.length; i++) {
				Coordinate start = c[i-1];
				Coordinate end = c[i];
				newActions.add(new MilledLine((int)start.x, (int)start.y, (int)end.x, (int)end.y, depth, distance));
			}
		}
		
		// discard current traces, etc. and replace with new polygons
		saveUndoBuffer();
		addAll(newActions);
		drawAll();
	}

	public void fattenTraces(double ddistance) {

		List list = getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		List newActions = new LinkedList();
		
		// Now fatten all the traces
		log.debug("Calculating buffers at distance "+ddistance);
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingGeometryAction a = (MillingGeometryAction) iter.next();
			Geometry g = a.getGeometry();
			g = g.buffer(ddistance,0,BufferOp.CAP_SQUARE);
			newActions.add(new TracePolygon(g));
		}
		
		saveUndoBuffer();
		// Add the new traces
		addAll(newActions);
		// discard the existing traces
		removeAll(list);
		drawAll();
	}
	
	public void calculateTraces(ProgressStatusBar status) {
		//First create a set of Geometry objects for each trace and pad

		status.setMessage("Calculating traces");
		status.startProgress(0);
		
		List list = getSelectedList(MASK_COMBINABLE | MASK_GEOMETRY);
		status.startProgress(list.size()*2);
		
		// Now the list contains all of the Geometrys associated with the actions
		// loop through each to find intersections

		MillingGeometryAction[] garray = (MillingGeometryAction[]) list.toArray(new MillingGeometryAction[list.size()]);

		
		int traces[] = new int[garray.length];
		int traceCounter = 0;
		Geometry pg[] = new Geometry[garray.length];  // Geometrys
		
		for (int outer = 0; outer < garray.length; outer++) {
			Geometry gouter = garray[outer].getGeometry();
			boolean matched = false;
			for (int inner = 0; inner < garray.length; inner++) {
				if (outer != inner) {
					Geometry ginner = garray[inner].getGeometry();
					if (gouter.intersects(ginner)) {
						matched = true;
						log.debug("Intersection "+outer+":"+gouter+" with "+inner+":"+ginner);
						if (traces[outer] == 0  && traces[inner] == 0) {
							traces[outer] = traceCounter;
							traces[inner] = traceCounter;
							pg[traceCounter] = gouter.union(ginner);
							traceCounter++;
						} else if (traces[outer] == 0) {
							int trace = traces[inner];
							traces[outer] = trace;
							pg[trace] = pg[trace].union(gouter);
						} else {
							int trace = traces[outer];
							traces[inner] = trace;
							pg[trace] = pg[trace].union(ginner);
						}
					}
				
				}
			}
			if (!matched) {
				traces[outer] = traceCounter;
				pg[traceCounter] = gouter;
				traceCounter++;
			}
			status.incrementProgress(outer,list.size()*2);
		}
		
		log.debug("Found "+traceCounter+" Unique traces on first pass");
		status.setMessage("First pass found "+traceCounter+" traces. Combining...");
		// Second Pass: looking for any intersections between remaining traces

		boolean attached = false; // set true if any intersections are found
		int passes = 0;
		do {
			passes++;
			log.debug("Staring pass #"+passes);
			attached = false; // assume no changes
			for (int outer = 0; outer < traceCounter; outer++) {
				Geometry gouter = pg[outer];
				if (gouter != null) {
					for (int inner = 0; inner < traceCounter; inner++) {
						
						try {
							if (inner != outer) {
								Geometry ginner = pg[inner];
								if (ginner != null && gouter.intersects(ginner)) {
									// Join these traces
									gouter = ginner.union(gouter);
									pg[outer] = gouter;
									pg[inner] = null;
									attached = true;
								}
							}
						} catch (TopologyException e) {
							log.info("Ignored exception",e);
						}
					}
				}
				
			}
		} while (attached);

		saveUndoBuffer();
		// discard current traces, etc. and replace with new polygons
		removeAll(list);
		status.setMessage("Creating "+traceCounter+" traces...");
		int count = 0;
		for (int i = 0; i < traceCounter; i++) {
			Geometry g = pg[i];
			if (g != null) {
				add(new TracePolygon(g));
				count++;
			}
			status.incrementProgress(traceCounter+i,traceCounter*2);		
		}
		log.debug("Found "+count+" Unique traces at end");
		status.stopProgress();
		status.setMessage("Trace calculation completed. "+count+" traces created");
		drawAll();
		
	}
	
	public void simplifyTraces(double distance) {

		List list = getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		List newActions = new LinkedList();
		
		// Now simplify all the traces
		log.debug("Simplifying traces");
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingGeometryAction a = (MillingGeometryAction) iter.next();
			Geometry g = a.getGeometry();
			newActions.add(new TracePolygon(TopologyPreservingSimplifier.simplify(g,distance)));
		}
		
		saveUndoBuffer();
		// Add the new traces
		addAll(newActions);
		// discard the existing traces
		removeAll(list);
		drawAll();
	}

	public void rasterizeInterior(ProgressStatusBar status, Tool tool, int depth) {

		List list = getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		list.addAll(getSelectedList(MASK_DRILL));
		status.setMessage("Rasterizing "+list.size()+" actions");
		status.startProgress(list.size()+1);

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
					rasterizePolygon(tool, depth, newActions, p);
				} else {
					for (int n=0; n < holes; n++) {
						LinearRing lr = gf.createLinearRing(p.getInteriorRingN(n).getCoordinates());
						Polygon ip = gf.createPolygon(lr, new LinearRing[] {});
						rasterizePolygon(tool, depth, newActions, ip);
						if (removeAction != null) {
							removeActions.add(removeAction);
						}
					}
				}
				
			}
			status.incrementProgress(progressCount++,list.size());
		}
		
		saveUndoBuffer();
		// remove anything that needs to be removed
		removeAll(removeActions);
		// Add the new traces
		addAll(newActions);
		drawAll();
		status.stopProgress();
		status.setMessage("Rasterizing "+list.size()+" actions");
	}

	private void rasterizePolygon(Tool tool, int depth, List newActions, Geometry g) {
		GeometryFactory gf = MillingAction.geoFactory;
		Polygon p = (Polygon) g;
		int diameter = tool.getStepDiameter();
		int radius = diameter/2;
		Rectangle r = GeometryUtil.calculateBounds(p);
		if (r == null) {
			log.info("calculateBounds returned null!");
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
				newActions.add(new MilledLine(startx, y, endx, y, depth, tool.getStepDiameter()));
			}
			
			// Finally, make a pass on the interior border
			g=g.buffer(-radius,0,BufferOp.CAP_BUTT); 
			Coordinate c[] = g.getCoordinates();
			for (int i = 1; i < c.length; i++) {
				Coordinate start = c[i-1];
				Coordinate end = c[i];
				newActions.add(new MilledLine((int)start.x, (int)start.y, (int)end.x, (int)end.y, depth, diameter));
			}
		}
	}
	
	public SurfacePanel getSurface() {
		return surface;
	}

	public void setSurface(SurfacePanel surface) {
		this.surface = surface;
	}

	public boolean isEmpty() {
		return (actionsList == null || actionsList.isEmpty());
	}

	public void clear() {
		actionsList.clear();
		resetProcessing();
		fireCountChanged();
	}

	public void add(MillingAction action) {
		actionsList.add(action);
		drawAction(action);
		fireCountChanged();
	}

	public void drawAction(MillingAction action) {
		if (surface == null) return;
		surface.drawAction(action);
		
	}

	public boolean processNextAction(MillingMachine mill) {

		boolean stepped = false;
		do {
			if (currentAction >= actionsList.size()) {
				return false; // At end
			}
			MillingAction action = (MillingAction) actionsList.get(currentAction);
			if (action == null) {
				throw new RuntimeException("action is null");
			}
			currentAction++;
			
			if (action instanceof Executable) {
				Executable exec = (Executable) action;
				if (mill.getCurrentTool() == null || !mill.getCurrentTool().equals(action.getTool())) {
					Tool newTool = action.getTool();
					if (newTool != null) {
						
						// Save the state of the drill and vacuum
						boolean drillwas = mill.isDrillRelay();
						boolean vacwas = mill.isVacuumRelay();
						
						// Stop the Drill and Vacuum
						mill.setDrillRelay(false);
						mill.setVacuumRelay(false);
						
						Object waitObj = new Object(); // just used for synchronization
						ToolChangeDialog dialog = new ToolChangeDialog(null,""+newTool,waitObj);
						dialog.setVisible(true);
						
						while (dialog.getResult() == ToolChangeDialog.RESULT_UNKNOWN) {
							synchronized (waitObj) {
								try {
									waitObj.wait();
								} catch (InterruptedException e) {
									log.debug("in ToolChange.execute", e);
								}
							}
						}
						int result = dialog.getResult();
						if (result != ToolChangeDialog.RESULT_CONTINUE) {
							throw new MillingException(MillingException.UNKNOWN,": user cancelled tool change");
						}
						mill.setCurrentTool(newTool);
						
						// Return the drill and vacuum to previous states
						mill.setDrillRelay(drillwas);
						mill.setVacuumRelay(vacwas);
					}
				}
				exec.execute(mill);
				final MillingAction finalAction = action;
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							drawAction(finalAction);
							surface.repaint();
						}
					});
				} catch (Exception e) {
					log.error(e);
				}
				stepped = true;
			}
		} while (!stepped);
		return true;
	}
	
	public boolean isAtEndOfList() {
		if (currentAction >= actionsList.size()) {
			return true;
		}
		return false;
	}

	public boolean isAtStartOfList() {
		if (currentAction == 0) {
			return true;
		}
		return false;
	}
	
	
	public void resetProcessing() {
		currentAction = 0;
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			action.setComplete(false);
		}
		drawAll();
		
	}

	public void selectRectangle(int x, int y, int width, int height) {
		// Selects all elements that are contained within the rectangle
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			if (action.matches(selectionMask)) {
				action.setSelected(action.isInside(x, y, width, height));
			}
		}
		selectionCount = -1;
		fireSelectRectangle(new Rectangle(x,y,width,height));
		drawAll();

	}

	public boolean addAll(Collection arg0) {
		boolean rtnval = actionsList.addAll(arg0);
		fireCountChanged();
		return rtnval;
	}

	public boolean removeAll(Collection arg0) {
		boolean rtnval = actionsList.removeAll(arg0);
		fireCountChanged();
		return rtnval; 
	}
	
	protected List getSelectedList(int mask) {
		ArrayList list = new ArrayList();
		boolean isAnythingSelected = false;
		
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			if (action.isSelected()) {
				isAnythingSelected = true;
				if (action.matches(mask | selectionMask)) {
					list.add(action);
				}
			}
		}
		
		if (!isAnythingSelected) {
			// loop again for the whole list if nothing is selected.
			for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
				MillingAction action = (MillingAction) iter.next();
				if (action.matches(mask | selectionMask)) {
					list.add(action);
				}
			}
		}
		
		return list;
	}
	
	public void selectAll() {
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			if (action.matches(selectionMask)) {
				action.setSelected(true);
			}
		}
		fireSelectionChanged();
		drawAll();
	}

	public int getSelectionMask() {
		return selectionMask;
	}

	/**
	 * Sets a selection mask that is used for all selections.  Bits on in the 
	 * mask must be on in the selected results.
	 * @param selectionMask
	 */
	public void setSelectionMask(int selectionMask) {
		this.selectionMask = selectionMask;
	}
	
	private void fireSelectionChanged() {
		firePropertyChange("selectionCount",0,-1);
		selectionCount = -1;
	}

	private void fireCountChanged() {
		bounds = null; // force recalculation of bounds
		firePropertyChange("totalCount",0,-1);
	}
	
	private void fireSelectRectangle(Rectangle rect) {
		firePropertyChange("selectRectangle",null, rect);
	}
	
	
	public void addPropertyChangeListener(PropertyChangeListener arg0) {
		propertyChangeSupport.addPropertyChangeListener(arg0);
	}

	public void addPropertyChangeListener(String arg0, PropertyChangeListener arg1) {
		propertyChangeSupport.addPropertyChangeListener(arg0, arg1);
	}

	public void firePropertyChange(PropertyChangeEvent arg0) {
		propertyChangeSupport.firePropertyChange(arg0);
	}

	public void firePropertyChange(String arg0, boolean arg1, boolean arg2) {
		propertyChangeSupport.firePropertyChange(arg0, arg1, arg2);
	}

	public void firePropertyChange(String arg0, int arg1, int arg2) {
		propertyChangeSupport.firePropertyChange(arg0, arg1, arg2);
	}

	public void firePropertyChange(String arg0, Object arg1, Object arg2) {
		propertyChangeSupport.firePropertyChange(arg0, arg1, arg2);
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return propertyChangeSupport.getPropertyChangeListeners();
	}

	public PropertyChangeListener[] getPropertyChangeListeners(String arg0) {
		return propertyChangeSupport.getPropertyChangeListeners(arg0);
	}

	public boolean hasListeners(String arg0) {
		return propertyChangeSupport.hasListeners(arg0);
	}

	public void removePropertyChangeListener(PropertyChangeListener arg0) {
		propertyChangeSupport.removePropertyChangeListener(arg0);
	}

	public void removePropertyChangeListener(String arg0, PropertyChangeListener arg1) {
		propertyChangeSupport.removePropertyChangeListener(arg0, arg1);
	}

	public double getTotalDistance() {
		LinearGeometryBuilder builder = new LinearGeometryBuilder(MillingAction.geoFactory);
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			MillLocation[] locations = action.getCoordinates();
			for (int i = 0; i < locations.length; i++) {
				MillLocation loc = locations[i];
				// construct a geometry
				builder.add(new Coordinate(loc.x, loc.y));
			}
		}
		double length = 0D;
		try {
			length = builder.getGeometry().getLength();
		} catch (RuntimeException e) {
			log.error(e);
		}
		builder = null;
		length = length/240.0D;
		return length;
	}

	public String getTotalTime(MillingMachine mill) {
		// Calculation first calculates steps per second = 1000/min(xdelay,ydelay)
		double stepsPerSecond = 1000.0D/(double)Math.max(mill.getXDelay(),mill.getYDelay());
		// divide by 240 to get inches per second
		double inchesPerSecond = stepsPerSecond/240.0d;
		// divide total distance by inches per second to get number of seconds
		int totalSeconds = (int) (getTotalDistance()/inchesPerSecond);
		int hours = totalSeconds/3600;
		int minutes = (totalSeconds%3600)/60;
		int seconds = (totalSeconds%60);
		Format f = new DecimalFormat("00");
		return f.format(new Integer(hours))+":"+f.format(new Integer(minutes))+":"+f.format(new Integer(seconds));
	}

	/**
	 * Optimizes the order of milling and drilling operations to minimize tool changes
	 * and reduce the amount of non-prodcutive movement.
	 * This process can swap the ends of a milled trace if necessary.
\	 */
	public void optimizeMillPaths(ProgressStatusBar status) {
		status.setMessage("Path optimization in progress");
		status.startProgress(0);
		List sortActions = new ArrayList();
		List removeActions = new ArrayList();
		List newActions = new ArrayList();
		for (Iterator iter = actionsList.iterator(); iter.hasNext();) {
			MillingAction action = (MillingAction) iter.next();
			if (action.matches(MASK_DRILL) || action.matches(MASK_MILL)) {
				if (!action.isComplete()) {
					sortActions.add(action);
					removeActions.add(action);
				}
			}
		}
		status.startProgress(sortActions.size());
		// Now pick a start point (we'll use origin (0,0), and a tool and
		//  start sequencing.
		MillLocation cloc = new MillLocation(0,0,0);
		Tool cTool = null;
		
		
		while (!sortActions.isEmpty()) {
			double dtot = 9999.0D;
			int nindex = -1;
			int cindex = -1;
			MillingAction champ = null; 
			// Loop to find the nearest location with the same tool
			for (int i = 0; i < sortActions.size(); i++) {
				MillingAction action = (MillingAction) sortActions.get(i);
				// first check the tool
				if (cTool != null && !cTool.equals(action.getTool())) continue;
				for (int j = 0; j < action.getCoordinates().length; j++) {
					MillLocation loc = action.getCoordinates()[j];
					// calculate the distance
					double dist = loc.distanceFrom(cloc);
					if (dist < dtot) {
						// its closer, so its better!
						cindex = j;
						nindex = i;
						dtot = dist;
						champ = action;
					} else if (dist == dtot) {
						// they are the same, pick the better one
						if (cindex > j) { 
							// this is better because it doesn't mean a swap
							cindex = j;
							nindex = i;
							dtot = dist;
							champ = action;
						}
					}
				
				}
			}
			if (champ != null) {
				sortActions.remove(champ);
				status.incrementProgress(newActions.size(),removeActions.size());
				if (cindex > 0) {
					// gotta swap the start and end of the MilledLine
					champ =((MilledLine)champ).swapEnds();
					newActions.add(champ);
				} else {
					newActions.add(champ);
				}
				cloc = champ.getEndPoint();
				cTool = champ.getTool();
				
			} else {
				// Could not find a matching tool - try again with null for any tool
				cTool = null;
			}
		} 
			
		// When we get here, newActions contains the correctly ordered list
		saveUndoBuffer();
		actionsList.removeAll(removeActions);
		actionsList.addAll(newActions);
		status.stopProgress();
		status.setMessage("Path optimization complete!");
		fireCountChanged();
	}

	public void mirrorActions() {
		List list = getSelectedList(0);
		List newActions = new LinkedList();
		
		// Now mirror x axis on all actions
		log.debug("Mirroring around X axis");
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingAction a = (MillingAction) iter.next();
			newActions.add(a.getMirrorX());
		}
		
		saveUndoBuffer();
		// Add the new traces
		addAll(newActions);
		// discard the existing traces
		removeAll(list);
		drawAll();

		
	}
	
	
}