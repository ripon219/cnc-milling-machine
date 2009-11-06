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

public class MillSegmentsCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(MillSegmentsCommand.class);
	
	private static final long serialVersionUID = 1L;

	private int depth = 2;

	private JTextField depthField;

	private JPanel extraPanel;

	private Tool tool;
	
	public MillSegmentsCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar) {
		super("Mill Trace Segments", actionsList, statusBar);
		extraPanel = new JPanel();
		extraPanel.setLayout(new GridLayout(2,0));
		extraPanel.add(new JLabel("Depth (Steps)"));
		
		depthField = new JTextField("2");
		extraPanel.add(depthField);
	}

	
	protected void showDialog() {
		tool = actionsList.showToolSelection("Select Mill Segment Parameters",extraPanel);
		depth = Integer.parseInt(depthField.getText());
	}
	
	protected void execute() {
		
		List list = actionsList.getSelectedList(MASK_TRACESEGMENT);
		
		log.debug("Converting trace segments to milled lines");
	
		List newActions = new ArrayList();
		List removeActions = new ArrayList();
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			TraceSegment a = (TraceSegment) iter.next();
			newActions.add(new MilledLine(a.xstart,a.ystart,a.xpos,a.ypos, depth, tool));
			removeActions.add(a);
		}
		
		// discard current traces, etc. and replace with new polygons
		actionsList.saveUndoBuffer();
		actionsList.addAll(newActions);
		actionsList.removeAll(removeActions);
		actionsList.drawAll();
	}

}
