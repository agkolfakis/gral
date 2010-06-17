/*
 * GRAL: GRAphing Library for Java(R)
 *
 * (C) Copyright 2009-2010 Erich Seifert <info[at]erichseifert.de>, Michael Seifert <michael.seifert[at]gmx.net>
 *
 * This file is part of GRAL.
 *
 * GRAL is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GRAL is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GRAL.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.erichseifert.gral.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.geom.Point2D;

import org.junit.Test;

public class PointNDTest {
	public static final double DELTA = 1e-15;

	@Test
	public void testCreate() {
		PointND p;

		// Constructor with Number[]
		p = new PointND(1, 2, 3, 4);
		assertEquals(4, p.getDimensions());

		// Constructor with Point2D
		p = new PointND(new Point2D.Double(1.0, 2.0));
		assertEquals(2, p.getDimensions());
	}

	@Test
	public void testGet() {
		Number[] coordinates = {1, 2, 3, 4};
		PointND p = new PointND(coordinates);

		for (int dim = 0; dim < coordinates.length; dim++) {
			assertEquals(coordinates[dim], p.get(dim));
		}
	}

	@Test
	public void testSet() {
		Number[] coordinates = {1, 2, 3, 4};
		PointND p = new PointND(coordinates);

		int dim = 1;
		p.set(dim, 0.0);
		assertFalse(coordinates[dim].equals(p.get(dim)));
		assertEquals(0.0, p.get(dim));

		Number[] coordinatesNew = {0, 1, 3, 2};
		p.setLocation(coordinatesNew);
		for (int d = 0; d < coordinates.length; d++) {
			assertEquals(coordinatesNew[d], p.get(d));
		}

		try {
			Number[] coordinatesNew2 = {0, 1};
			p.setLocation(coordinatesNew2);
			fail("Expected IllegalArgumentException exception.");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testToString() {
		PointND p = new PointND(1.0, 2.0);
		assertEquals("de.erichseifert.gral.util.PointND[1.0, 2.0]", p.toString());
	}

	@Test
	public void testEquality() {
		PointND p1 = new PointND(1.0, 2.0);
		PointND p2 = new PointND(1.0, 2.0);
		PointND p3 = new PointND(1.0, 2.0, 3.0);
		// Equals
		assertTrue(p1.equals(p2));
		assertFalse(p1.equals(null));
		assertFalse(p2.equals(null));
		assertFalse(p1.equals(p3));
		// Hash code
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@Test
	public void testPoint2D() {
		PointND p4 = new PointND(1.0, 2.0, 3.0, 4.0);

		assertEquals(new Point2D.Double(1.0, 2.0), p4.getPoint2D());
		assertEquals(new Point2D.Double(2.0, 3.0), p4.getPoint2D(1, 2));

		PointND p1 = new PointND(1.0);
		try {
			p1.getPoint2D();
			fail("Expected ArrayIndexOutOfBoundsException exception.");
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}

}