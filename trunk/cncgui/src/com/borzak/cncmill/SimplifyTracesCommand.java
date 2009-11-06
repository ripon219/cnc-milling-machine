/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.util.*;

import javax.swing.*;

import org.apache.commons.logging.*;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.*;

public class SimplifyTracesCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(SimplifyTracesCommand.class);

	private static final long serialVersionUID = 1L;

	private double ddistance;

	public SimplifyTracesCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar) {
		super("Simplify Traces", actionsList, statusBar);
	}

	protected void execute() {

		List list = actionsList.getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		List newActions = new LinkedList();
		
		// Now simplify all the traces
		log.debug("Simplifying traces");
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingGeometryAction a = (MillingGeometryAction) iter.next();
			Geometry g = a.getGeometry();
			newActions.add(new TracePolygon(TopologyPreservingSimplifier.simplify(g,ddistance)));
		}
		
		// Add the new traces
		actionsList.addAll(newActions);
		// discard the existing traces
		actionsList.removeAll(list);
	}

	protected void showDialog() {
		String distance = JOptionPane.showInputDialog(null,
				"Optimize Distance", "1.0");
		ddistance = Double.parseDouble(distance);
	}

}
