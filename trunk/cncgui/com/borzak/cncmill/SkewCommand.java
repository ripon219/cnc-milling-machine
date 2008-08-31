/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.util.*;

import org.apache.commons.logging.*;

public class SkewCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(SkewCommand.class);
	MillingTransform transform = null;
	Object waitObj = new Object();
	SkewDialog dialog = new SkewDialog(null,waitObj);
	
	public SkewCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar) {
		super("Skew Actions", actionsList, statusBar);
	}

	protected void showDialog() {
		// Set the origin x,y location and the xSkew and ySkew directions
		dialog.setVisible(true);
		if (dialog.getResult() != SkewDialog.RESULT_CONTINUE) {
			throw new MillingException(MillingException.UNKNOWN,": user cancelled tool change");
		}
		
		// Create a MillTransformation
		transform = new SkewTransform(dialog.getOrigin(), dialog.getXFactor(),dialog.getYFactor());
	}
	
	protected void execute() {
		List list = actionsList.getSelectedList(0);
		List newActions = new LinkedList();
		
		// Now mirror x axis on all actions
		log.debug("Skewing Points");
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingAction a = (MillingAction) iter.next();
			newActions.add(a.getTransformedInstance(transform));
		}
		
		// Add the new traces
		actionsList.addAll(newActions);
		// discard the existing traces
		actionsList.removeAll(list);
	}

}
