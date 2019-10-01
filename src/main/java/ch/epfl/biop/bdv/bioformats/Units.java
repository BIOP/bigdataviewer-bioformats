package ch.epfl.biop.bdv.bioformats;

import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

public class Units
{
	public static final String MICRONS = "Microns";
	public static final String MILLIMETERS = "Millimeters";

	public static Unit< Length > getLengthUnit( String unit )
	{
		if ( unit == null) {
			return null; }
		if ( unit.equals( MILLIMETERS )) {
			return UNITS.MILLIMETER;
		} else if ( unit.equals( MICRONS )) {
			return UNITS.MICROMETRE;
		} else {
			return null;
		}
	}
}
