/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.util.*;

import com.vividsolutions.jts.geom.*;

public class CalculateTraceCommand extends ActionListCommand {

	private static final long serialVersionUID = 1L;

	public CalculateTraceCommand(MillingActionList actionsList, ProgressStatusBar statusBar) {
		super("Calculate Traces", actionsList, statusBar);
	}

	protected void execute() {
		//First create a set of Geometry objects for each trace and pad
	
		statusBar.setMessage("Calculating traces");
		
		List list = actionsList.getSelectedList(MASK_COMBINABLE | MASK_GEOMETRY);
		statusBar.startProgress(list.size()*2);
		
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
						MillingActionList.log.debug("Intersection "+outer+":"+gouter+" with "+inner+":"+ginner);
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
			statusBar.incrementProgress(outer,list.size()*2);
		}
		
		MillingActionList.log.debug("Found "+traceCounter+" Unique traces on first pass");
		statusBar.setMessage("First pass found "+traceCounter+" traces. Combining...");
		// Second Pass: looking for any intersections between remaining traces
	
		boolean attached = false; // set true if any intersections are found
		int passes = 0;
		do {
			passes++;
			MillingActionList.log.debug("Staring pass #"+passes);
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
							MillingActionList.log.info("Ignored exception",e);
						}
					}
				}
				
			}
		} while (attached);
	
		actionsList.saveUndoBuffer();
		// discard current traces, etc. and replace with new polygons
		actionsList.removeAll(list);
		statusBar.setMessage("Creating "+traceCounter+" traces...");
		int count = 0;
		for (int i = 0; i < traceCounter; i++) {
			Geometry g = pg[i];
			if (g != null) {
				actionsList.add(new TracePolygon(g));
				count++;
			}
			statusBar.incrementProgress(traceCounter+i,traceCounter*2);		
		}
		MillingActionList.log.debug("Found "+count+" Unique traces at end");
		statusBar.stopProgress();
		statusBar.setMessage("Trace calculation completed. "+count+" traces created");
		actionsList.drawAll();
		
	}

}
