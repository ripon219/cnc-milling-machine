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
import com.vividsolutions.jts.operation.buffer.*;

public class IsolateTraceCommand extends ActionListCommand {
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(IsolateTraceCommand.class);

	private int idistance = 0;
	private int depth = 2;

	private JTextField depthField;

	private JTextField overcutField;

	private JPanel extraPanel;

	private Tool tool;

	
	public IsolateTraceCommand(MillingActionList actionsList, ProgressStatusBar statusBar) {
		super("Isolate Traces", actionsList, statusBar);
		
		extraPanel = new JPanel();
		extraPanel.setLayout(new GridLayout(2,0));
		extraPanel.add(new JLabel("Depth (Steps)"));
		
		depthField = new JTextField("2");
		extraPanel.add(depthField);
		
		extraPanel.add(new JLabel("Overcut (inches)"));
		
		overcutField = new JTextField("0.000");
		extraPanel.add(overcutField);
	}
	
	protected void showDialog() {
		
		
		tool = actionsList.showToolSelection("Select Isolation Parameters",extraPanel);
		depth = Integer.parseInt(depthField.getText());
		double overcut = Double.parseDouble(overcutField.getText());
		
		idistance = (int) (overcut * 240.0);
		idistance+=tool.getStepDiameter();
	}

	protected void execute() {

		List list = actionsList.getSelectedList(MASK_TRACE | MASK_GEOMETRY);
		statusBar.startProgress(list.size());

		
		// Now buffer all the traces
		int radius = idistance/2;
		
		log.debug("Calculating buffers at distance "+idistance);
	
		List newActions = new ArrayList();
		int count = 0;
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			
			MillingGeometryAction a = (MillingGeometryAction) iter.next();
			Geometry g = a.getGeometry();
			
			g = g.buffer(radius,0,BufferOp.CAP_BUTT); 
			Coordinate c[] = g.getCoordinates();
			for (int i = 1; i < c.length; i++) {
				Coordinate start = c[i-1];
				Coordinate end = c[i];
				newActions.add(new MilledLine((int)start.x, (int)start.y, (int)end.x, (int)end.y, depth, tool));
			}
			statusBar.incrementProgress(count++,list.size());
		}
		
		// discard current traces, etc. and replace with new polygons
		actionsList.addAll(newActions);
		statusBar.stopProgress();
		statusBar.setMessage("Trace isolation completed. "+newActions.size()+" milled lines added");

	}
	

}
