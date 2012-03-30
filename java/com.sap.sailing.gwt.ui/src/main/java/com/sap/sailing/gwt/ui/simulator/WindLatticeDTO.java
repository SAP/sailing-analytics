package com.sap.sailing.gwt.ui.simulator;

import java.io.Serializable;

import com.sap.sailing.gwt.ui.shared.PositionDTO;

public class WindLatticeDTO implements Serializable {

	/**
	 * Generated uid for serialisation
	 */
	private static final long serialVersionUID = -2110785502151983845L;
	private PositionDTO [][] matrix;
	
	public WindLatticeDTO() {
	}
	
	public void setMatrix( PositionDTO [][] matrix ) {
		this.matrix = matrix;
	}
	
	public PositionDTO[][] getMatrix() {
		return matrix;
	}
}
