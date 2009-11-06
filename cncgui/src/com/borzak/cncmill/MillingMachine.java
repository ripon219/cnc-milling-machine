/**
 * Milling Machine Controller
 * Created on Dec 17, 2007
 */
package com.borzak.cncmill;

import java.beans.*;
import java.io.*;
import java.util.regex.*;

import javax.comm.*;

import org.apache.commons.logging.*;

import com.sun.org.apache.xerces.internal.impl.xs.*;

public class MillingMachine {
	private static Log log = LogFactory.getLog(MillingMachine.class);

	private int xMin;
	private int xMax;
	private int yMin;
	private int yMax;
	private int zMin;
	private int zMax;
	
	private int xLocation=0;
	private int yLocation=0;
	private int zLocation=0;
	
	private int xDelay = 100;
	private int yDelay = 100;
	private int zDelay = 30;

	private int xInch = 240;
	private int yInch = 240;
	private int zInch = 230;

	private boolean xHold = false;
	private boolean yHold = false;
	private boolean zHold = false;
	
	private boolean drillRelay = false;
	private boolean vacuumRelay = false;
	
	private boolean xLimitMin = false;
	private boolean xLimitMax = false;

	private boolean yLimitMin = false;
	private boolean yLimitMax = false;

	private boolean zLimitMin = false;
	private boolean zLimitMax = false;

	private boolean simulating = false; 
	private int simulateDelay = 1;
	
	private Tool currentTool = null;

	private SerialPort port;
	private Reader portReader;
	private BufferedReader portInBuffer;
	private PrintWriter portPrinter;
	private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

	private float firmwareLevel = 0.0F;
	
	MillingProperties properties = new MillingProperties();

	public MillingMachine() {
		// Create with machine limits assuming x,y at center and z touching material at zero
//		this(-1271,1271,-939,939, -705, 47);
		this(-2000,2000,-2000,2000, -705, 705);
	}
	
	public MillingMachine(int xMin, int xMax, int yMin, int yMax, int zMin, int zMax) {
		super();
		this.xMin = xMin;
		this.xMax = xMax;
		this.yMin = yMin;
		this.yMax = yMax;
		this.zMin = zMin;
		this.zMax = zMax;
		properties.load();
	}

	
/**
 * Move a specific offset from current position.  The Z Axis is processed first,
 * The X and Y axises are moved together.
 * @param xOffset - the distance to move in the X direction
 * @param yOffset - the distance to move in the Y direction
 * @param zOffset - the distance to move in the Z direction
 * @throws Exception 
 */	
public void moveOffset(int xOffset, int yOffset, int zOffset) {
	
	try {
		MillLocation startLocation = getLocation();
		
		// first check for software limits
		int newloc = xLocation + xOffset;
		if (newloc < xMin || newloc > xMax) {
			throw new MillingException(MillingException.SOFT_LIMIT, "X: "+newloc);
		}

		newloc = yLocation + yOffset;
		if (newloc < yMin || newloc > yMax) {
			throw new MillingException(MillingException.SOFT_LIMIT, "Y: "+newloc);
		}

		newloc = zLocation + zOffset;
		if (newloc < zMin || newloc > zMax) {
			throw new MillingException(MillingException.SOFT_LIMIT, "Z: "+newloc);
		}
		
		
		String sign = "+";
		// all three specified - move Z first.
		if (zOffset < 0) {
			sign = "-";
			zOffset = Math.abs(zOffset);
		} else {
			sign = "+";
		}
		int offset = 0;
		while (zOffset > 0) { // handles bigger than 255 numbers
			if (zOffset > 255) {
				offset = 255;
			} else {
				offset = zOffset;
			}
			
			MillingException me = null;
			
			String response = null;
			try {
				response = processCommand("MZ"+sign+toThreeChar(offset));
			} catch (MillingException e) {
				me = e;
				response = e.getResponse();
			}
			
			if (getFirmwareVersionAsFloat() >= 1.1F) {
				int[] moves = extractMovement(response, new int[] {0,0,offset});
				offset = moves[2];
			}
			
			if (sign.equals("-")) {
				zLocation -= offset;
			} else {
				zLocation += offset;
			}
				
			zOffset -= offset;
			
			if (me != null) {
				throw me; // rethrow the milling exception after handling the steps processed
			}
		}
		
		// Move x and y 
		String xsign = "+"; 
		if (xOffset < 0) {
			xsign = "-"; 
			xOffset = Math.abs(xOffset);
		}
		String ysign = "+";
		if (yOffset < 0) {
			ysign = "-";
			yOffset = Math.abs(yOffset);
		}
		
		while (xOffset > 0 || yOffset > 0) {
			
			double slope = (double)yOffset/(double)xOffset;
			int xSteps = xOffset;
			int ySteps = yOffset;

			if (xOffset > 255 && xOffset >= yOffset) {
				// calculate the number of ySteps based on 255 xSteps
				xSteps = 255;
				ySteps = (int)Math.round((double)xSteps * slope);
			}
			if (yOffset > 255 && yOffset > xOffset) {
				// calculate the number of xSteps based on 255 ySteps
				ySteps = 255;
				xSteps = (int)Math.round((double)ySteps / slope);
			}
			String s = "M";
			if (xSteps > 0) {
				s = s + "X" + xsign + toThreeChar(xSteps);
			}
			
			if (ySteps > 0) {
				s = s + "Y" + ysign + toThreeChar(ySteps);
			}
			if (s.length() > 1) {
				MillingException me = null;
				
				String response = null;
				try {
					response = processCommand(s);
				} catch (MillingException e) {
					me = e;
					response = e.getResponse();
				}
				
				if (getFirmwareVersionAsFloat() >= 1.1F) {
					int[] moves = extractMovement(response, new int[] {xSteps,ySteps,0});
					xSteps = moves[0];
					ySteps = moves[1];
				}
				
				if (xsign.equals("-")) { 
					xLocation -= xSteps;
				} else {
					xLocation += xSteps;
				}
				xOffset -= xSteps;
				if (ysign.equals("-")) {
					yLocation -= ySteps;
				} else {
					yLocation += ySteps;
				}
				yOffset -= ySteps;

				if (me != null) {
					throw me; // rethrow the milling exception after handling the steps processed
				}
			}
			
		}

		if (simulating) {
			// One second delay for simulations
			try {
				Thread.sleep(simulateDelay);
			} catch (InterruptedException e) {
			} 
		}
		
		// if we got this far, limits must all be off
		clearLimits();

		firePropertyChange("location", startLocation, getLocation());
		log.info("Position: "+xLocation+","+yLocation+","+zLocation);
	} catch (MillingException e) {
		if (e.getReason() == MillingException.BROWNOUT_ERROR) {
		// Special handling of brownouts during a move
		// Assume this system has better location data than the mill does, so force it all back to 
		// stored values
		log.error("Reseting mill locations due to Brownout!");
		calibrateX(xLocation);
		calibrateY(yLocation);
		calibrateZ(zLocation);
		setXDelay(xDelay);
		setYDelay(yDelay);
		setZDelay(zDelay);
		}
		
		throw e;
	}
}

/**
 * Extracts the raw number of processed x,y,and z steps from a
 * movement response.  If this is an older firmware, the value 
 * returned will always be zero, so that is worth trapping for.
 * Movement steps are only provided by firmware v1.1 and up.
 * @param response  The movement response to extract steps from
 * @return int[] with three counts of steps in the order x,y,z
 */
private int[] extractMovement(String response, int[] inMoves) {
	int[] moves = new int[3];
	Pattern p = Pattern.compile("[xyz]+");
	Matcher m = p.matcher(response);
	if (m.find()) {
		String s = m.group();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == 'x') moves[0]++;
			if (c == 'y') moves[1]++;
			if (c == 'z') moves[2]++;
		} 
	}
	if ((inMoves[0] != moves[0]) || (inMoves[1] != moves[1]) || (inMoves[2] != moves[2])) {
		log.error("Step Mismatch - expected ("+inMoves[0]+","+inMoves[1]+","+inMoves[2]+
				") got ("+moves[0]+","+moves[1]+","+moves[2]+")");
	}
	
	
	return moves;
}
	
/**
 * Moves to a specific location.  The reserved value -9999 an be used to default any
 * axis to its current location.
 * @param xAddress The x-axis location to postion to (-9999 means no movement)
 * @param yAddress The y-axis location to postion to (-9999 means no movement)
 * @param zAddress The z-axis location to postion to (-9999 means no movement)
 */
public void moveTo(int xAddress, int yAddress, int zAddress) {
	// Calculate the offsets and call the move offset version
	int xOffset = 0;
	int yOffset = 0;
	int zOffset = 0;
	if (xAddress != -9999) {
		xOffset = xAddress - xLocation;
	}
	if (yAddress != -9999) {
		yOffset = yAddress - yLocation;
	}
	if (zAddress != -9999) {
		zOffset = zAddress - zLocation;
	}
	moveOffset(xOffset, yOffset, zOffset);
}


private String toSignedFiveChar(int number) {
	String sign = "";
	if (number < 0) {
		sign = "-";
		number=0-number;
	}
	String s = Integer.toString(number);
	if (s.length() < 5) {
		s = "00000"+s;
		s = s.substring(s.length()-5);
	}
	s = sign+s;
	return s;
}


public void calibrateX(int newX) {
	if (newX == 0) {
		calibrateX();
		return;
	}
	processCommand("CX="+toSignedFiveChar(newX));
	xLocation = newX;
}

public void calibrateY(int newY) {
	if (newY == 0) {
		calibrateY();
		return;
	}
	processCommand("CY="+toSignedFiveChar(newY));
	yLocation = newY;
}

public void calibrateZ(int newZ) {
	if (newZ == 0) {
		calibrateZ();
		return;
	}
	processCommand("CZ="+toSignedFiveChar(newZ));
	zLocation = newZ;
}

public void calibrateX() {
	processCommand("CX");
	xLocation = 0;
}

public void calibrateY() {
	processCommand("CY");
	yLocation = 0;
}

public void calibrateZ() {
	processCommand("CZ");
	zLocation = 0;
}

public String getFirmwareVersion() {
	String version;
	try {
		version = processCommand("F");
		// strip off any leading or trailing whitespace
		version = version.trim();
	} catch (MillingException e) {
		if (e.getReason() == MillingException.INVALID_COMMAND) {
			version = "Default - Old Firmware V1.0"; 
		} else {
			throw e;
		}
	}
	return version;
}

public float getFirmwareVersionAsFloat() {
	if (firmwareLevel == 0.0F) {
		// Look it up and parse it out
		String version = getFirmwareVersion();
		Pattern p = Pattern.compile("V[0-9]+\\.[0-9]+");
		Matcher m = p.matcher(version);
		if (m.find()) {
			String shortVersion = m.group();
			firmwareLevel = Float.parseFloat(shortVersion.substring(1));
		}
	}
	return firmwareLevel;
}

public void readStatus() {
	// This sends a list command and stores the result
	
	StringReader r = null;
	BufferedReader br = null;
	try {
		String response = processCommand("L");
		
		r = new StringReader(response);
		br = new BufferedReader(r);
		
		String line = null;
		Pattern axisPattern = Pattern.compile(
		"([XYZ]) Axis: Location: ([- 0-9]{0,6})  Delay: ([0-9]{0,3})  Steps per Inch: ([0-9]{0,3})  Holding Current: ([01])");
		Pattern drillPattern = Pattern.compile("Drill=([01])");
		Pattern vacPattern = Pattern.compile("Vacuum=([01])");
		Pattern limitPattern = Pattern.compile("Limit Switch: ([XYZ])-axis min=([01]) max=([01])");
		
		while ( (line = br.readLine()) != null) {
			Matcher m = axisPattern.matcher(line);
			if (m.matches()) {
				// Its an AXIS line
				switch (m.group(1).charAt(0)) {
				case 'X':
					xLocation = Integer.parseInt(m.group(2).trim()); 
					xDelay = Integer.parseInt(m.group(3));
					xInch = Integer.parseInt(m.group(4));
					xHold = m.group(5).equals("1");
					break;

				case 'Y':
					yLocation = Integer.parseInt(m.group(2).trim());
					yDelay = Integer.parseInt(m.group(3));
					yInch = Integer.parseInt(m.group(4));
					yHold = m.group(5).equals("1");
					break;

				case 'Z':
					zLocation = Integer.parseInt(m.group(2).trim());
					zDelay = Integer.parseInt(m.group(3));
					zInch = Integer.parseInt(m.group(4));
					zHold = m.group(5).equals("1");
					break;
				}
			}
			
			m = drillPattern.matcher(line);
			if (m.matches()) {
				// Its a drill line
				drillRelay = m.group(1).equals("1");
			}
			
			m = vacPattern.matcher(line);
			if (m.matches()) {
				// Its a drill line
				vacuumRelay = m.group(1).equals("1");
			}

			m = limitPattern.matcher(line);
			if (m.matches()) {
				// Its a Limit line
				switch (m.group(1).charAt(0)) {

				case 'X':
					xLimitMin = m.group(2).equals("1");
					xLimitMax = m.group(3).equals("1");
					break;


				case 'Y':
					yLimitMin = m.group(2).equals("1");
					yLimitMax = m.group(3).equals("1");
					break;

				case 'Z':
					zLimitMin = m.group(2).equals("1");
					zLimitMax = m.group(3).equals("1");
					break;
				}
			}
		}
	} catch (Exception e) {
		log.error("in MillingMachine.readStatus", e);
	} finally {
		try {
			if (br != null) {
				br.close();
			}
		} catch (IOException e) {
			log.error("Error closing r", e);
		}
		try {
			if (r != null) {
				r.close();
			}
		} catch (Exception e) {
			log.error("Error closing r", e);
		}
	}
	
}


private String processCommand(String command) throws RuntimeException {
	if (simulating) {
		log.info("Command: "+command);
		if (command.equals("L")) {
			String s = 
				"X Axis: Location:  "+xLocation+"  Delay: "+xDelay+"  Steps per Inch: "+xInch+"  Holding Current: "+(xHold?"1":"0")+"\n"+
				"Y Axis: Location:  "+yLocation+"  Delay: "+yDelay+"  Steps per Inch: "+yInch+"  Holding Current: "+(yHold?"1":"0")+"\n"+
				"Z Axis: Location:  "+zLocation+"  Delay: "+zDelay+"  Steps per Inch: "+zInch+"  Holding Current: "+(zHold?"1":"0")+"\n"+
				"Drill="+(drillRelay?"1":"0")+"\n"+
				"Vacuum="+(vacuumRelay?"1":"0")+"\n"+
				"Limit Switch: X-axis min=0 max=0\n"+
				"Limit Switch: Y-axis min=0 max=0\n"+
				"Limit Switch: Z-axis min=0 max=0\n$ ";
				return s;
		}
		if (command.equals("F")) {
			return "Vince's SIMULATED Firmware V1.1\n$ ";
		}
		Pattern moveAxis = Pattern.compile("[XYZ][+-][0-9]{3}");
		
		
		if (command.startsWith("M")) {
			// Special case to simulate a Brownout for any move from 5,5 
			if (xLocation == 5 && yLocation == 5) {
			String response = "xyxy\n" +
					"Vince's CNC MILL V1.1\n" +
					"\n" +
					"BROWNOUT RESET" +
					"\n" +
					"\n" +
					"\n$ ";
			throw new MillingException(response,MillingException.BROWNOUT_ERROR);
			}
			
			// parse out the movements requested
			StringBuffer response = new StringBuffer();
			Matcher m = moveAxis.matcher(command);
			while (m.find()) {
				String axis = m.group();
				int n = Integer.parseInt(axis.substring(2,5));
				while (n > 0) {
					response.append(axis.substring(0,1).toLowerCase());
					n--;
				}
			}
			response.append("\n$ ");
			return response.toString();
		}
		return "$ ";
	} else {
		try {
			// Do the real thing
			openPort();
			// Send the command
			log.debug("Sent: "+command);
			portPrinter.print(command+"\n\r");
			portPrinter.flush();
			// read input until "$ " is received
			StringBuffer response = new StringBuffer();
			char lastChar = ' ';
			while (lastChar!='$') {
				lastChar = (char) portReader.read();
				response.append(lastChar);
			}
			log.debug("Received: "+response);
			
			// Process failure messages: 
			//  SERIAL RECEIVE ABORT
			//  LIMIT ERROR
			//  Invalid Command
			//  Command not implemented yet
			
			if (response.indexOf("SERIAL RECEIVE ABORT") != -1) {
				// its a serial abort
				throw new MillingException(response.toString(), MillingException.SERIAL_ABORT);
			}
			
			if (response.indexOf("LIMIT ERROR") != -1) {
				// its a limit switch error
				throw new MillingException(response.toString(), MillingException.LIMIT_ERROR);
			}

			if (response.indexOf("BROWNOUT RESET") != -1) {
				// its a brownout reset error
				throw new MillingException(response.toString(), MillingException.BROWNOUT_ERROR);
			}
			
			if (response.indexOf("Invalid Command") != -1) {
				// its an invalid command (which means a comm problem usually
				throw new MillingException(response.toString(), MillingException.INVALID_COMMAND);
			}
			
			if (response.indexOf("Command not implemented yet") != -1) {
				// its a unimplemented command - comm error or old firmware
				throw new MillingException(response.toString(), MillingException.NOT_IMPLEMENTED);
			}
			return response.toString();
		} catch (MillingException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Processing Command: "+command,e);
		}
	}
}

/**
 * 
 */
private void clearLimits() {
	xLimitMax = false;
	xLimitMin = false;
	yLimitMax = false;
	yLimitMin = false;
	zLimitMax = false;
	zLimitMin = false;
}

/**
 * Opens the com port for the Milling Machine and initializes
 * the input and outpur streams.
 * @throws NoSuchPortException 
 * @throws PortInUseException 
 * @throws UnsupportedCommOperationException 
 * @throws IOException 
 */
private void openPort() throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
	if (port != null) return; // already done it.
	CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier("COM1");
	CommPort cp = cpi.open("MillingMachine",1000);
	if (cp instanceof SerialPort) {
	    port = (SerialPort)cp;
		port.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
	    port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

	    InputStream in = port.getInputStream();
	    portReader = new InputStreamReader(in);
		portInBuffer = new BufferedReader(portReader);
		OutputStream out = port.getOutputStream();
	    portPrinter = new PrintWriter(out);
	}
}

private void closePort() {

	portPrinter.close();
	
	try {
		if (portInBuffer != null) {
			portInBuffer.close();
		}
	} catch (IOException e) {
		log.error("Error closing portInBuffer", e);
	}
	
	try {
		if (portReader != null) {
			portReader.close();
		}
	} catch (IOException e) {
		log.error("Error closing portReader", e);
	}
	
	try {
		if (port != null) {
			port.close();
		}
	} catch (Exception e) {
		log.error("Error closing port", e);
	}
}

public static void main(String[] args) {
	MillingMachine mill = new MillingMachine();
	mill.readStatus();
	
}


public boolean isDrillRelay() {
	return drillRelay;
}


public void setDrillRelay(boolean drillRelay) {
	processCommand("D"+(drillRelay ? "1" : "0"));
	// Wait for the drill to get to speed if turning it on
	if (!this.drillRelay && drillRelay) {
		log.debug("Waiting 1 sec for drill to reach speed");
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			// Ignoring irrelevant exception (logging just to be sure)
			log.debug("Ignoring: ",e);
		}
	}
	this.drillRelay = drillRelay;
}


public boolean isVacuumRelay() {
	return vacuumRelay;
}


public void setVacuumRelay(boolean vacuumRelay) {
	this.vacuumRelay = vacuumRelay;
	processCommand("V"+(vacuumRelay ? "1" : "0"));
}


public int getXDelay() {
	return xDelay;
}


public void setXDelay(int delay) {
	xDelay = delay;
	processCommand("SDX="+toThreeChar(delay));
}


private String toThreeChar(int number) {
	String s = Integer.toString(number);
	if (s.length() < 3) {
		s = "000"+s;
		s = s.substring(s.length()-3);
	}
	return s;
}


public boolean isXHold() {
	return xHold;
}


public void setXHold(boolean hold) {
	xHold = hold;
	processCommand("HX"+(xHold ? "1" : "0"));
}


public int getXInch() {
	return xInch;
}


public void setXInch(int inch) {
	xInch = inch;
	processCommand("SIX="+toThreeChar(inch));
}


public int getYDelay() {
	return yDelay;
}


public void setYDelay(int delay) {
	yDelay = delay;
	processCommand("SDY="+toThreeChar(delay));
}


public boolean isYHold() {
	return yHold;
}


public void setYHold(boolean hold) {
	yHold = hold;
	processCommand("HY"+(yHold ? "1" : "0"));
}


public int getYInch() {
	return yInch;
}


public void setYInch(int inch) {
	yInch = inch;
	processCommand("SIY="+toThreeChar(inch));
}


public int getZDelay() {
	return zDelay;
}


public void setZDelay(int delay) {
	zDelay = delay;
	processCommand("SDZ="+toThreeChar(delay));
}


public boolean isZHold() {
	return zHold;
}


public void setZHold(boolean hold) {
	zHold = hold;
	processCommand("HZ"+(zHold ? "1" : "0"));
}


public int getZInch() {
	return zInch;
}


public void setZInch(int inch) {
	zInch = inch;
	processCommand("SIZ="+toThreeChar(inch));
}


public boolean isXLimitMax() {
	return xLimitMax;
}


public boolean isXLimitMin() {
	return xLimitMin;
}


public int getXLocation() {
	return xLocation;
}


public boolean isYLimitMax() {
	return yLimitMax;
}


public boolean isYLimitMin() {
	return yLimitMin;
}


public int getYLocation() {
	return yLocation;
}


public boolean isZLimitMax() {
	return zLimitMax;
}


public boolean isZLimitMin() {
	return zLimitMin;
}


public int getZLocation() {
	return zLocation;
}

protected void finalize() throws Throwable {
	super.finalize();
	closePort();
}

public void addPropertyChangeListener(PropertyChangeListener listener) {
	listeners.addPropertyChangeListener(listener);
}

public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
	listeners.firePropertyChange(propertyName, oldValue, newValue);
}

public void removePropertyChangeListener(PropertyChangeListener listener) {
	listeners.removePropertyChangeListener(listener);
}

public MillLocation getLocation() {
	return new MillLocation(xLocation, yLocation, zLocation);
}

public void SetSimulating(boolean b) {
	simulating = b;
}

public boolean getSimulating() {
	return simulating;
}

public int getSimulateDelay() {
	return simulateDelay;
}

public void setSimulateDelay(int simulateDelay) {
	this.simulateDelay = simulateDelay;
}

public Tool getCurrentTool() {
	return currentTool;
}

public void setCurrentTool(Tool currentTool) {
	this.currentTool = currentTool;
}

public void moveTo(MillLocation loc) {
	moveTo(loc.getX(), loc.getY(), loc.getZ());
	
}

public MillingProperties getProperties() {
	return properties;
}

public int getZSafe() {
	return properties.getZSafe();
}



}