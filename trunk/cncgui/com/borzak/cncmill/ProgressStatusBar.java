package com.borzak.cncmill;

import java.awt.*;
import java.text.*;

import javax.swing.*;

/**
 * This implementation of the IStatusBar uses a standard JProgressBar
 * and a JTextField for the status message area.
 * 
 * All the methods are thread-safe. Each method ensures that the change is
 * run on the event thread (if it isn't already in the event thread).
 * 
 * @author mpritchs
 */
public class ProgressStatusBar extends JPanel {
	private static final long serialVersionUID = 1L;
	JTextField messageTF = null;
	JProgressBar progressBar = null;
	JTextField positionTF = null;
	private JTextField totalCountTF;
	private JTextField totalDistanceTF;
	private JTextField totalTimeTF;
	private JTextField selectCountTF;
	

	public ProgressStatusBar() {
		super();
		
		progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(100, 21));
		
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		
		// The progress indicator
		c.gridx=0; c.gridy=0;
		c.gridwidth=1;
		c.insets = new Insets(0, 0, 0, 2);
		c.anchor=GridBagConstraints.WEST;
		c.fill=GridBagConstraints.HORIZONTAL;
		layout.setConstraints(progressBar, c);
		add(progressBar);
		
		// The position 
		c.gridx=1; c.gridy=0;
		c.gridwidth=1;
		c.insets = new Insets(0, 0, 0, 2);
		positionTF = new JTextField(5);
		positionTF.setPreferredSize(new Dimension(40, 21));
		positionTF.setEditable(false);
		layout.setConstraints(positionTF, c);
		add(positionTF);
		positionTF.setToolTipText("Mouse Position");

		// The action count  
		c.gridx=2; c.gridy=0;
		c.gridwidth=1;
		c.insets = new Insets(0, 0, 0, 2);
		totalCountTF = new JTextField(5);
		totalCountTF.setPreferredSize(new Dimension(40, 21));
		totalCountTF.setEditable(false);
		layout.setConstraints(totalCountTF, c);
		add(totalCountTF);
		totalCountTF.setToolTipText("Total number of actions loaded");

		// The total movement distance
		c.gridx=3; c.gridy=0;
		c.gridwidth=1;
		c.insets = new Insets(0, 0, 0, 2);
		totalDistanceTF = new JTextField(5);
		totalDistanceTF.setPreferredSize(new Dimension(40, 21));
		totalDistanceTF.setEditable(false);
		layout.setConstraints(totalDistanceTF, c);
		add(totalDistanceTF);
		totalDistanceTF.setToolTipText("Total distance of movements loaded");
		
		// The total estimated time
		c.gridx=4; c.gridy=0;
		c.gridwidth=1;
		c.insets = new Insets(0, 0, 0, 2);
		totalTimeTF = new JTextField(5);
		totalTimeTF.setPreferredSize(new Dimension(40, 21));
		totalTimeTF.setEditable(false);
		layout.setConstraints(totalTimeTF, c);
		add(totalTimeTF);
		totalTimeTF.setToolTipText("Total estimated time to process loaded actions");

		// The count of selected items
		c.gridx=5; c.gridy=0;
		c.gridwidth=1;
		c.insets = new Insets(0, 0, 0, 2);
		selectCountTF = new JTextField(5);
		selectCountTF.setPreferredSize(new Dimension(40, 21));
		selectCountTF.setEditable(false);
		layout.setConstraints(selectCountTF, c);
		add(selectCountTF);
		selectCountTF.setToolTipText("Total count of selected actions");
		
		
		// The message text
		c.gridx=6; c.gridy=0;
		c.gridwidth=GridBagConstraints.REMAINDER;
		c.weightx=1.0;
		messageTF = new JTextField(20);
		messageTF.setEditable(false);
		messageTF.setPreferredSize(new Dimension(40, 21));
		layout.setConstraints(messageTF, c);
		add(messageTF);
		
		// set reasonable defaults
		setTotalCount(0);
		setTotalDistance(0.0D);
		setTotalTime("00:00:00");
		setSelectCount(0);
		setMessage("");
		
	}
	
	/**
	 * Start the progress bar.
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 */
	public void startProgress(final int maximum) {
		if (SwingUtilities.isEventDispatchThread()) {
			internalStartProgress(maximum);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					internalStartProgress(maximum);
				}
			});
		}
	}
	
	/**
	 * This private method is used internally to actually start the progress
	 * @param maximum
	 */
	private void internalStartProgress(int maximum) {
		if (maximum == 0) {
			progressBar.setIndeterminate(true);
		} else {
			progressBar.setMaximum(maximum);
			progressBar.setIndeterminate(false);
		}
	}
	
	/**
	 * Stop the progress bar.
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 */
	public void stopProgress() {
		if (SwingUtilities.isEventDispatchThread()) {
			progressBar.setIndeterminate(false);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					progressBar.setIndeterminate(false);
				}
			});
		}
	}

	/**
	 * Increment the progress bar.
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 */
	public void incrementProgress(final int value, final int maximum) {
		if (SwingUtilities.isEventDispatchThread()) {
			internalIncrementProgress(value, maximum);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					internalIncrementProgress(value, maximum);
				}
			});
		}
	}
	
	private void internalIncrementProgress(int value, int maximum) {
		progressBar.setMaximum(maximum);
		progressBar.setIndeterminate(false);
		progressBar.setValue(value);
	}

	/**
	 * Clear the messages.
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 */		
	public void clearMessage() {
		if (SwingUtilities.isEventDispatchThread()) {
			messageTF.setText("");
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					messageTF.setText("");
				}
			});
		}
	}

	/**
	 * Set the displayed message. 
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 * @param message
	 */	
	public void setMessage(final String message) {
		if (message != null) {
			if (SwingUtilities.isEventDispatchThread()) {
				messageTF.setText(message);
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						messageTF.setText(message);
					}
				});
			}
		}
	}

	/**
	 * Set the position of the mouse 
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 * @param int
	 */
	public void setPosition(int x, int y) {
		if (positionTF != null) {
			String positionText = ""+x+","+y;
			
			if (SwingUtilities.isEventDispatchThread()) {
				positionTF.setText(positionText);
			} else {
				final String finalPositionText = positionText; 
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						positionTF.setText(finalPositionText);
					}
				});
			}
		}
	}

	/**
	 * Set the selectCount
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 * @param int
	 */
	public void setSelectCount(int selectCount) {
		if (selectCountTF != null) {
			String selectCountText = "" + selectCount;

			if (SwingUtilities.isEventDispatchThread()) {
				selectCountTF.setText(selectCountText);
			} else {
				final String finalText = selectCountText;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						selectCountTF.setText(finalText);
					}
				});
			}
		}
	}

	/**
	 * Set the totalTime
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 * @param int
	 */
	public void setTotalTime(String totalTime) {
		if (totalTimeTF != null) {

			if (SwingUtilities.isEventDispatchThread()) {
				totalTimeTF.setText(totalTime);
			} else {
				final String finalText = totalTime;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						totalTimeTF.setText(finalText);
					}
				});
			}
		}
	}
	
	
	/**
	 * Set the total distance
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 * @param int
	 */
	public void setTotalDistance(double totalDistance) {
		if (totalDistanceTF != null) {
			Format f = new DecimalFormat("#0.000\"");
			String totalDistanceText = f.format(new Double(totalDistance));

			if (SwingUtilities.isEventDispatchThread()) {
				totalDistanceTF.setText(totalDistanceText);
			} else {
				final String finalText = totalDistanceText;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						totalDistanceTF.setText(finalText);
					}
				});
			}
		}
	}
	
	/**
	 * Set the totalCount
	 * This method is thread-safe. It will always be run on the event 
	 * thread (if it is not already).
	 * @param int
	 */
	public void setTotalCount(int totalCount) {
		if (totalCountTF != null) {
			String totalCountText = "" + totalCount;

			if (SwingUtilities.isEventDispatchThread()) {
				totalCountTF.setText(totalCountText);
			} else {
				final String finalText = totalCountText;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						totalCountTF.setText(finalText);
					}
				});
			}
		}
	}	
	/**
	 * Return the message currently being displayed.
	 * @return String
	 */
	public String getMessage() {
		return messageTF.getText();
	}
	
	public void resetProgress() {
		if (SwingUtilities.isEventDispatchThread()) {
			internalResetProgress();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					internalResetProgress();
				}
			});
		}
	}

	private void internalResetProgress() {
		progressBar.setValue(0);
	}
}

