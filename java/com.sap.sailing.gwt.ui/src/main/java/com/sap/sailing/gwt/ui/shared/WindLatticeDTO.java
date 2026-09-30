package com.sap.sailing.gwt.ui.shared;

import java.io.Serializable;

import com.sap.sse.common.Position;

public class WindLatticeDTO implements Serializable {

	/**
	 * Generated uid for serialisation
	 */
	private static final long serialVersionUID = -2110785502151983845L;
	private Position [][] matrix;
	
	public WindLatticeDTO() {
	}
	
	public void setMatrix( Position [][] matrix ) {
		this.matrix = matrix;
	}

	public Position[][] getMatrix() {
		return matrix;
	}
}