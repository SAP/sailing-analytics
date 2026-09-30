package com.sap.sailing.domain.markpassingcalculation.splining;

import java.util.ArrayList;
import java.util.List;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;

/**
 * A vector in the 2 dimensional plane that supports numerous vector operations.
 * @author Martin Hanysz
 *
 */
public class Vector2D {
	private List<Double> components;
	
	/**
	 * Creates a {@link Vector2D} with the given components.
	 * It is possible to give more than 2 dimensions, but only the x and y dimensions
	 * are used for the vector operations.
	 * @param components - the component values of the vector to create
	 */
	public Vector2D(double ... components) {
		this.components = new ArrayList<Double>(2);
		for (int i = 0; i < 2; i++) {
			this.components.add(components[i]);
		}
	}
	
	/**
	 * Creates a {@link Vector2D} with the given components.
	 * It is possible to give more than 2 dimensions, but only the x and y dimensions
	 * are used for the vector operations.
	 * @param components - a {@link List} of the component values of the vector to create
	 */
	public Vector2D(List<Double> components) {
		this.components = new ArrayList<Double>();
		for (int i = 0; i < 2; i++) {
			this.components.add(components.get(i));
		}
	}
	
	/**
	 * A shortcut for creating a {@link Vector2D} from a {@link Position}.
	 * @param position - a {@link Position} to create a {@link Vector2D} for
	 */
	public Vector2D(Position position) {
		this(position.getLngDeg(), position.getLatDeg());
	}

	/**
	 * A shortcut for creating a {@link Vector2D} from a {@link Bearing}.
	 * The result is a vector of the given length that points into the direction of the given bearing
	 * @param direction - the {@link Bearing} the resulting {@link Vector2D} should point to
	 * @param length - the desired length of the resulting {@link Vector2D}
	 */
	public Vector2D(Bearing direction, double length) {
		this(calculateXFromBearing(direction, length), calculateYFromBearing(direction, length));
	}

	private static double calculateXFromBearing(Bearing bearing,
			double length) {
		double degrees = bearing.getDegrees();
		if (degrees == 0.0 || degrees == 180.0) {
			// no x component
			return 0.0;
		}

		if (degrees > 0.0 && degrees < 90.0) {
			return Math.sin(degrees % 90.0) * length;
		} else if (degrees == 90.0) {
			return length;
		} else if (degrees > 90.0 && degrees < 180.0) {
			return Math.cos(degrees % 90.0) * length;
		} else if (degrees > 180.0 && degrees < 270.0) {
			return -Math.sin(degrees % 90.0) * length;
		} else if (degrees == 270.0) {
			return -length;
		} else if (degrees > 270.0 && degrees < 360.0) {
			return -Math.cos(degrees % 90.0) * length;
		} else {
			throw new IllegalArgumentException("The given Bearing was greater than 360 degrees.");
		}
	}

	private static double calculateYFromBearing(Bearing bearing,
			double length) {
		double degrees = bearing.getDegrees();
		if (degrees == 90.0 || degrees == 270.0) {
			// no y component
			return 0.0;
		} 

		if (degrees == 0.0) {
			return length;
		} else if (degrees > 0.0 && degrees < 90.0) {
			return Math.cos(degrees % 90.0) * length;
		} else if (degrees > 90.0 && degrees < 180.0) {
			return -Math.sin(degrees % 90.0) * length;
		} else if (degrees > 180.0 && degrees < 270.0) {
			return -Math.cos(degrees % 90.0) * length;
		} else if (degrees == 180.0) {
			return -length;
		} else if (degrees > 270.0 && degrees < 360.0) {
			return Math.sin(degrees % 90.0) * length;
		} else {
			throw new IllegalArgumentException("The given Bearing was greater than 360 degrees.");
		}
	}

	/**
	 * Get a {@link Vector2D} that is perpendicular to this vector.
	 * @return a {@link Vector2D} perpendicular to this one
	 */
	public Vector2D getPerpendicularVector() {
		return new Vector2D(-this.y(), this.y());
	}
	
	/**
	 * Adds the given {@link Vector2D} to this vector.
	 * @param v - the {@link Vector2D} to add to this vector
	 * @return the {@link Vector2D} representing the result of adding the given vector to this one
	 */
	public Vector2D add(Vector2D v) {
		return new Vector2D(x() + v.x(), y() + v.y());
	}
	
	/**
	 * Subtracts the given {@link Vector2D} from this vector.
	 * @param v - the {@link Vector2D} to subtract from this vector
	 * @return the {@link Vector2D} representing the result of subtracting the given vector from this one
	 */
	public Vector2D subtract(Vector2D v) {
		return new Vector2D(x() - v.x(), y() - v.y());
	}
	
	/**
	 * Multiplies this vector with the given scalar.
	 * @param scalar -  the scalar to multiply this vector with
	 * @return the {@link Vector2D} representing the result of multiplying this vector with the given scalar
	 */
	public Vector2D multiply(double scalar) {
		return new Vector2D(x() * scalar, y() * scalar);
	}
	
	/**
	 * Divides this vector by the given scalar.
	 * @param scalar -  the scalar to divide this vector by
	 * @return the {@link Vector2D} representing the result of dividing this vector by the given scalar
	 */
	public Vector2D divide(double scalar) {
		return multiply(1.0/scalar);
	}
	
	/**
	 * Creates the dot product of this vector and the given {@link Vector2D}.
	 * @param v - the {@link Vector2D} to create the dot product with
	 * @return the dot product of this vector and the given one
	 */
	public double dotProduct(Vector2D v) {
		return x()*v.x() + y()*v.y();
	}
	
	/**
	 * Returns the length of this vector.
	 * @return the length of this vector
	 */
	public double getLength() {
		return Math.sqrt(Math.pow(x(), 2) + Math.pow(y(), 2));
	}
	
	/**
	 * Calculates the distance from the point represented by this vector to the point represented by the given {@link Vector2D}.
	 * @param p - the {@link Vector2D} representing the point to calculate the distance to
	 * @return the distance from the point represented by this vector to the point represented by the given vector
	 */
	public double getDistanceToPoint(Vector2D p) {
		return p.subtract(this).getLength();
	}
	
	/**
	 * Returns the value of the component with the given index.
	 * @param i -  the index of the component value to return
	 * @return the value of the component at index i
	 */
	public double get(int i) {
		return components.get(i);
	}
	
	/**
	 * Returns the x component of this vector.
	 * @return the x component of this vector
	 */
	public double x() {
		return components.get(0);
	}
	
	/**
	 * Returns the y component of this vector.
	 * @return the y component of this vector
	 */
	public double y() {
		return components.get(1);
	}

	/**
	 * Sets the value of the component at index i to d
	 * @param i - the index of the component to set
	 * @param d - the value to set the specified component to
	 */
	public void set(int i, double d) {
		components.set(i, d);
	}
	
	@Override
	public String toString() {
		return "Vector2D: " + components.toString();
	}

	/**
	 * Returns a normalized version of this vector.
	 * @return the normalized version of this vector
	 */
	public Vector2D normalize() {
		return divide(getLength());
	}
}