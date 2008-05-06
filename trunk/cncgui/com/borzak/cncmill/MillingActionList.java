package com.borzak.cncmill;

import java.awt.*;
import java.beans.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.commons.logging.*;

import com.vividsolutions.jts.geom.*;
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
	static Log log = LogFactory.getLog(MillingActionList.class);
	
	PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	List actionsList = new ArrayList();
	int currentAction = 0;
	SurfacePanel surface = null;
	int selectionMask = 0;
	List undoStack = new LinkedList();

	Rectangle bounds = null;
	
	private int selectionCount = -1;

	private List commandList = new ArrayList();
	private MillingMachineFrame frame = null;
	
	public MillingActionList(MillingMachineFrame frame) {
		this.frame = frame;
	}

	public Tool showToolSelection(String desc, JPanel extraPanel) {
		return frame.showToolSelection(desc, extraPanel);
	}




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

	public void addCommand(AbstractAction command) {
		// Registers the command as one to be enabled/disabled as a group
		commandList.add(command);
	}

	public void enableCommands(boolean enable) {
		if (SwingUtilities.isEventDispatchThread()) {
			internalEnableCommands(enable);
		} else {
			final boolean finalEnable = enable;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					internalEnableCommands(finalEnable);
				}
			});
		}
	}

	private void internalEnableCommands(boolean enable) {
		for (Iterator iter = commandList.iterator(); iter.hasNext();) {
			AbstractAction action = (AbstractAction) iter.next();
			action.setEnabled(enable);
		}
	}

	public Iterator iterator() {
		return actionsList.iterator();
	}
	
	
}