package com.sap.sailing.gwt.ui.shared;

import java.io.Serializable;

import com.sap.sse.common.Position;

public class WindLatticeGenParamsDTO implements Serializable {

	/**
	 * Generated uid for serialisation
	 */
	private static final long serialVersionUID = 6074038176026601942L;

	private Position center;
	private double xSize = 1;
	private double ySize = 1;
	private int gridsizeX = 20;
	private int gridsizeY = 20;

	public WindLatticeGenParamsDTO() {
	}

	public void setCenter(Position center) {
		this.center = center;
	}

	public Position getCenter() {
		return center;
	}

	public double getxSize() {
		return xSize;
	}

	public void setxSize(double xSize) {
		this.xSize = xSize;
	}

	public double getySize() {
		return ySize;
	}

	public void setySize(double ySize) {
		this.ySize = ySize;
	}

	public int getGridsizeX() {
		return gridsizeX;
	}

	public void setGridsizeX(int gridsizeX) {
		this.gridsizeX = gridsizeX;
	}

	public int getGridsizeY() {
		return gridsizeY;
	}

	public void setGridsizeY(int gridsizeY) {
		this.gridsizeY = gridsizeY;
	}

}