package com.borzak.cncmill;

import java.io.*;

/**
 * Class that represents a specific tool  For now, it just has the type and 
 * diameter.  Later it will have descriptions, diameter/depth profiles, etc.
 * @author Vincent Greene
 */
public class Tool implements Serializable {

	int stepDiameter = 0;
	public static final int ENDMILL = 0;
	public static final int DRILL = 1;
	int type = ENDMILL;
	int maxDepth = 0;
	String description = null;
	
	public Tool() {
		// This is used for the XMLEncoder
	}
	
	public Tool(int type, double diameter) {
		this.type = type;
		this.stepDiameter = (int) (diameter*240.0D);
		description = getTypeAsString()+" "+diameter;
	}

	public Tool(int type, double diameter, String description) {
		this.type = type;
		this.stepDiameter = (int) (diameter*240.0D);
		this.description = description;
	}
	
	public Tool(int type, int stepDiameter) {
		this.type = type;
		this.stepDiameter = stepDiameter;
		description = getTypeAsString()+" Diameter "+stepDiameter;
	}
	
	
	public String getTypeAsString() {
		String s = null;
		switch (type) {
		case ENDMILL:
			s = "Endmill";
			break;

		case DRILL:
			s = "Drill Bit";
			break;
			
		default:
			s = "Unknown";
		}
		
		return s;

	}
	
	public boolean equals(Object other) {
		if (other == null) return false;
		if (other == this) return true;
		if (other instanceof Tool) {
			Tool otherTool = (Tool) other;
			if (otherTool.stepDiameter == this.stepDiameter
					&& otherTool.type == this.type) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Try to produce a unique hashcode for each unique instance.
	 */
	public int hashCode() {
		return type + (stepDiameter >> 4);
	}
	
	public String toString() {
		if (description != null) {
			return description;
		}

		String s = getTypeAsString();
		return s+" Diameter "+stepDiameter;
	}

	public int getStepDiameter() {
		return stepDiameter;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setStepDiameter(int stepDiameter) {
		this.stepDiameter = stepDiameter;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}
}
