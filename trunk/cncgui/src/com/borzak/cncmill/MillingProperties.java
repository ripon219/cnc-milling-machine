/*
 * Created on Apr 28, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.beans.*;
import java.io.*;

import org.apache.commons.logging.*;

/**
 * This class represents the global properties that control the gui and milling
 * operations.
 * 
 * @author Vincent Greene
 */
public class MillingProperties implements Serializable {
	private static final long serialVersionUID = -6038219633428497785L;

	private static Log log = LogFactory.getLog(MillingProperties.class);

	public int zsafe = -10;
	public boolean showVertices = false;
	public boolean coolDown = true;
	public int maxCutTimeSecs = 300; /* five minutes */
	public int coolDownSecs = 300; /* five minutes */
	public String portname = null;
	transient private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void firePropertyChange(PropertyChangeEvent evt) {
		changeSupport.firePropertyChange(evt);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

	public String getPortname() {
		// Default the port name based on OS
		if (portname == null) {
			String osname = System.getProperty("os.name","WTF");
			log.debug("Operating System is: "+osname);
			if (osname.toLowerCase().startsWith("linux")) {
				portname = "/dev/ttyS0";
			}
			if (osname.toLowerCase().startsWith("windows")) {
				portname = "COM1";
			}
		}
		return portname;
	}

	public void setPortname(String portname) {
		String oldvalue = this.portname;
		this.portname = portname;
		if (portname != null && !portname.equals(oldvalue)) {
			firePropertyChange(new PropertyChangeEvent(this, "portname", oldvalue, portname));
		}
	}

	public MillingProperties() {
		super();
	}
	
	
	public void setDefaults() {
		// Default the port name based on OS
		String osname = System.getProperty("os.name","WTF");
		log.debug("Operating System is: "+osname);
		if (osname.toLowerCase().startsWith("linux")) {
			portname = "/dev/tts/1";
		}
	}
	
	public void save() {
		
		FileOutputStream fos = null;
		XMLEncoder oos = null;
		
		try {
			fos = new FileOutputStream("mill.xml",false);
			oos = new XMLEncoder(fos);
			oos.writeObject(this);
		} catch (IOException e) {
			log.error("in MillingProperties.save", e);
		} finally {
			if (oos != null) {
				oos.close();
			}
			
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				log.error("Error closing fos", e);
			}
		}
		
	}
	

	public int getZsafe() {
		return zsafe;
	}

	public void setZsafe(int zSafe) {
		this.zsafe = zSafe;
	}

	public void load() {
		FileInputStream fis = null;
		XMLDecoder ois = null;
		try {
			// Read the tools from a file
			fis = new FileInputStream("mill.xml");
			ois = new XMLDecoder(fis);
			Object o = null;
			do {
				o = ois.readObject();
				if (o instanceof MillingProperties) {
					MillingProperties newProps = (MillingProperties) o;
					copyFrom(newProps);
				}
			} while (o != null);
		} catch (IOException e) {
			log.error("While loading mill.xml", e);
		} catch (Exception e) {
			log.error("Failed to load mill.xml:",e);
		} finally {
			if (ois != null) {
				ois.close();
			}
			
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				log.error("Error closing fis", e);
			}
		}
	}
		
	/**
	 * Copies the properties from the specified object to this one.
	 * @param newProps The one to copy
	 */
	public void copyFrom(MillingProperties newProps) {
		this.zsafe = newProps.zsafe;
		this.showVertices = newProps.showVertices;
		this.coolDown = newProps.coolDown;
		this.maxCutTimeSecs = newProps.maxCutTimeSecs; 
		this.coolDownSecs = newProps.coolDownSecs; 
		setPortname(portname);
}

	public boolean isCoolDown() {
		return coolDown;
	}


	public void setCoolDown(boolean coolDown) {
		this.coolDown = coolDown;
	}


	public int getCoolDownSecs() {
		return coolDownSecs;
	}


	public void setCoolDownSecs(int coolDownSecs) {
		this.coolDownSecs = coolDownSecs;
	}


	public int getMaxCutTimeSecs() {
		return maxCutTimeSecs;
	}


	public void setMaxCutTimeSecs(int maxCutTimeSecs) {
		this.maxCutTimeSecs = maxCutTimeSecs;
	}


	public boolean isShowVertices() {
		return showVertices;
	}


	public void setShowVertices(boolean showVertices) {
		this.showVertices = showVertices;
	}
	
	
	
}
