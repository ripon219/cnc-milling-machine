package com.borzak.cncmill;

/**
 * JobProperties - This represents all of the user selected
 * properties that control the loading of a file.
 * @author Vincent Greene
 */
public class JobProperties {

	public static final int RML_FORMAT = 0;
	public static final int NC_FORMAT = 1;
	public static final int RS274X_FORMAT = 2;
	
	private static final String[] format_xlate = new String[] {
		"RML Format",
		"NC Drill Format",
		"RS274X Gerber Format"
	};
	
	private int x=0;
	private int y=0;
	private boolean replace=true;
	private boolean displayOnly=false;
	private boolean skipShort=true;
	private int fileFormat = RS274X_FORMAT;
	private String filename=".";
	private int depth=15;
	private int diameter=4;
	private boolean depthOverride=false;
	private boolean toolOverride=false;
	
	
	public JobProperties() {
		super();
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public boolean isDepthOverride() {
		return depthOverride;
	}

	public void setDepthOverride(boolean depthOverride) {
		this.depthOverride = depthOverride;
	}

	public int getDiameter() {
		return diameter;
	}

	public void setDiameter(int diameter) {
		this.diameter = diameter;
	}

	public boolean isDisplayOnly() {
		return displayOnly;
	}

	public void setDisplayOnly(boolean displayOnly) {
		this.displayOnly = displayOnly;
	}

	public int getFileFormat() {
		return fileFormat;
	}

	public void setFileFormat(int fileFormat) {
		this.fileFormat = fileFormat;
	}

	public void setFileFormatAsString(String format) {
		for (int i = 0; i < format_xlate.length; i++) {
			String s = format_xlate[i];
			if (s.equals(format)) {
				setFileFormat(i);
				return;
			}
		}
	}
	
	public String getFileFormatAsString() {
		return format_xlate[fileFormat];
	}
	
	public String[] getFileFormats() {
		return format_xlate;
	}
	
	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public boolean isReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}

	public boolean isSkipShort() {
		return skipShort;
	}

	public void setSkipShort(boolean skipShort) {
		this.skipShort = skipShort;
	}

	public boolean isToolOverride() {
		return toolOverride;
	}

	public void setToolOverride(boolean toolOverride) {
		this.toolOverride = toolOverride;
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

}
