package com.borzak.cncmill;

/**
 * Encapsulates the location of the cutter on the milling machine
 * Note - this allows direct field access.
 * 
 * @author Vincent Greene
 */
public class MillLocation {
	public int x;
	public int y;
	public int z;
	

	/**
	 * Default constructor for bean model 
	 */
	public MillLocation() {
		super();
	}
	
	

	/**
	 * Creates an object that represents a specific location
	 * @param x
	 * @param y
	 * @param z
	 */
	public MillLocation(int x, int y, int z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}



	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public double distanceFrom(MillLocation other) {
		return Math.sqrt(Math.pow(Math.abs(x-other.x),2) + Math.pow(Math.abs(y-other.y),2));
	}
}
