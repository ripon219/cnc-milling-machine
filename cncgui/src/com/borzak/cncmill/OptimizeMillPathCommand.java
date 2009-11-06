/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.util.*;

import org.apache.commons.logging.*;

/**
 * Optimizes the order of milling and drilling operations to minimize tool changes
 * and reduce the amount of non-prodcutive movement.
 * This process can swap the ends of a milled trace if necessary.
 */
public class OptimizeMillPathCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(OptimizeMillPathCommand.class);

	int startX = 0;
	int startY = 0;
	
	public OptimizeMillPathCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar, int startX, int startY) {
		super("Optimize Milling Paths",actionsList, statusBar);
		this.startX = startX;
		this.startY = startY;
	}

	protected void execute() {
		statusBar.setMessage("Path optimization in progress");
		statusBar.startProgress(0);
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
		statusBar.startProgress(sortActions.size());
		// Now pick a start point (we'll use the selected postion, and a tool and
		//  start sequencing.
		MillLocation cloc = new MillLocation(startX,startY,0);
		Tool cTool = null;
		
		
		while (!sortActions.isEmpty()) {
			double dtot = 9999.0D;
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
						dtot = dist;
						champ = action;
					} else if (dist == dtot) {
						// they are the same, pick the better one
						if (cindex > j) { 
							// this is better because it doesn't mean a swap
							cindex = j;
							dtot = dist;
							champ = action;
						}
					}
				
				}
			}
			if (champ != null) {
				sortActions.remove(champ);
				statusBar.incrementProgress(newActions.size(),removeActions.size());
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
		actionsList.removeAll(removeActions);
		actionsList.addAll(newActions);
		statusBar.stopProgress();
		statusBar.setMessage("Path optimization complete!");
	}

}
