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
import com.vividsolutions.jts.operation.buffer.*;

public class FattenTracesCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(FattenTracesCommand.class);

	private static final long serialVersionUID = 1L;

	private double ddistance = 0.0F;
	
	public FattenTracesCommand(MillingActionList actionsList, ProgressStatusBar statusBar) {
		super("Fatten Traces", actionsList, statusBar);
	}
	
	protected void showDialog() {
		String distance = JOptionPane.showInputDialog(null,
				"Increase Trace Size by (inches)", "" + 1.0D / 240.0D);
		ddistance = Double.parseDouble(distance);
		ddistance = ddistance * 240.0D;
	}


	protected void execute() {
		List list = actionsList.getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		List newActions = new LinkedList();
		
		// Now fatten all the traces
		log.debug("Calculating buffers at distance "+ddistance);
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingGeometryAction a = (MillingGeometryAction) iter.next();
			Geometry g = a.getGeometry();
			g = g.buffer(ddistance,0,BufferOp.CAP_SQUARE);
			newActions.add(new TracePolygon(g));
		}
		
		actionsList.addAll(newActions);
		
		// discard the existing traces
		actionsList.removeAll(list);
	}

}
