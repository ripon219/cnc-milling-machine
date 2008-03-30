package com.borzak.cncmill;

/**
 * An interface that can receive status events.
 * 
 * @author Administrator
 */
public interface IStatusHandler {
	
	public abstract void startProgress();

	public abstract void stopProgress();

	public abstract void setText(String message);
}