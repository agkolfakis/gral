/*
 * GRAL: GRAphing Library for Java(R)
 *
 * (C) Copyright 2009-2013 Erich Seifert <dev[at]erichseifert.de>,
 * Michael Seifert <michael[at]erichseifert.de>
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
package de.erichseifert.gral.plots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import de.erichseifert.gral.data.AbstractDataSource;
import de.erichseifert.gral.data.Column;
import de.erichseifert.gral.data.DataSource;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.data.Row;
import de.erichseifert.gral.data.statistics.Statistics;
import de.erichseifert.gral.graphics.AbstractDrawable;
import de.erichseifert.gral.graphics.Drawable;
import de.erichseifert.gral.graphics.DrawingContext;
import de.erichseifert.gral.plots.axes.Axis;
import de.erichseifert.gral.plots.axes.AxisRenderer;
import de.erichseifert.gral.plots.axes.LinearRenderer2D;
import de.erichseifert.gral.plots.colors.ColorMapper;
import de.erichseifert.gral.plots.colors.ContinuousColorMapper;
import de.erichseifert.gral.plots.colors.SingleColor;
import de.erichseifert.gral.plots.legends.ValueLegend;
import de.erichseifert.gral.plots.points.AbstractPointRenderer;
import de.erichseifert.gral.plots.points.PointData;
import de.erichseifert.gral.util.GraphicsUtils;
import de.erichseifert.gral.util.PointND;
import de.erichseifert.gral.util.SerializationUtils;


/**
 * <p>Class that displays data as a box-and-whisker plot showing summaries of
 * important statistical values. The data source must provide six columns to
 * the {@code BoxPlot}:<p>
 * <ul>
 *   <li>Box position (for multiple boxes)</li>
 *   <li>Position of the center bar (e.g. median)</li>
 *   <li>Length of the lower whisker and position of the bottom bar
 *   (e.g. minimum)</li>
 *   <li>Position of the bottom edge of the box (e.g. first quartile)</li>
 *   <li>Position of the top edge of the box (e.g. third quartile)</li>
 *   <li>Length of the upper whisker and position of the top bar
 *   (e.g. maximum)</li>
 * </li>
 * <p>The utility method {@link #createBoxData(DataSource)} can be used to
 * obtain common statistics for these properties from the each column of an
 * existing data source.</p>
 *
 * <p>To create a new {@code BoxPlot} simply create a new instance using
 * a data source. Example:</p>
 * <pre>
 * DataTable data = new DataTable(Double.class, Double.class);
 * data.add(10.98, -12.34);
 * data.add( 7.65,  45.67);
 * data.add(43.21,  89.01);
 * DataSource boxData = BoxPlot.createBoxData(data);
 * BoxPlot plot = new BoxPlot(boxData);
 * </pre>
 */
public class BoxPlot extends XYPlot {
	/** Version id for serialization. */
	private static final long serialVersionUID = -3069831535208696337L;

	/**
	 * Class that renders a box and its whiskers in a box-and-whisker plot.
	 */
	public static class BoxWhiskerRenderer extends AbstractPointRenderer {
		/** Version id for serialization. */
		private static final long serialVersionUID = 2944482729753981341L;

		private int columnPosition;
		private int columnBarCenter;
		private int columnBarBottom;
		private int columnBoxBottom;
		private int columnBoxTop;
		private int columnBarTop;

		private double boxWidth;
		private ColorMapper boxBackground;
		private Paint boxColor;
		private transient Stroke boxBorder;

		private Paint whiskerColor;
		private transient Stroke whiskerStroke;

		private double barWidth;
		private Paint barCenterColor;
		private transient Stroke barCenterStroke;

		/**
		 * Constructor that creates a new instance and initializes it with a
		 * plot as data provider.
		 */
		public BoxWhiskerRenderer() {
			columnPosition = 0;
			columnBarCenter = 1;
			columnBarBottom = 2;
			columnBoxBottom = 3;
			columnBoxTop = 4;
			columnBarTop = 5;
			boxWidth = 0.75;
			boxBackground = new SingleColor(Color.WHITE);
			boxColor = Color.BLACK;
			boxBorder = new BasicStroke(1f);
			whiskerColor = Color.BLACK;
			whiskerStroke = new BasicStroke(1f);
			barWidth = 0.75;
			barCenterColor = Color.BLACK;
			barCenterStroke = new BasicStroke(
				2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		}

		/**
		 * Custom deserialization method.
		 * @param in Input stream.
		 * @throws ClassNotFoundException if a serialized class doesn't exist anymore.
		 * @throws IOException if there is an error while reading data from the
		 *         input stream.
		 */
		private void readObject(ObjectInputStream in)
				throws ClassNotFoundException, IOException {
			// Default deserialization
			in.defaultReadObject();
			// Custom deserialization
			boxBorder = (Stroke) SerializationUtils.unwrap(
					(Serializable) in.readObject());
			whiskerStroke = (Stroke) SerializationUtils.unwrap(
					(Serializable) in.readObject());
			barCenterStroke = (Stroke) SerializationUtils.unwrap(
					(Serializable) in.readObject());
		}

		/**
		 * Custom serialization method.
		 * @param out Output stream.
		 * @throws ClassNotFoundException if a serialized class doesn't exist anymore.
		 * @throws IOException if there is an error while writing data to the
		 *         output stream.
		 */
		private void writeObject(ObjectOutputStream out)
				throws ClassNotFoundException, IOException {
			// Default serialization
			out.defaultWriteObject();
			// Custom serialization
			out.writeObject(SerializationUtils.wrap(boxBorder));
			out.writeObject(SerializationUtils.wrap(whiskerStroke));
			out.writeObject(SerializationUtils.wrap(barCenterStroke));
		}

		/**
		 * Returns the index of the column which is used for the horizontal
		 * position of a box.
		 * @return Index of the column that is used for the horizontal position
		 * of a box.
		 */
		public int getColumnPosition() {
			return columnPosition;
		}

		/**
		 * Sets the index of the column which will be used for the horizontal
		 * position of a box.
		 * @param columnPosition Index of the column that is used for the
		 * horizontal position of a box.
		 */
		public void setColumnPosition(int columnPosition) {
			this.columnPosition = columnPosition;
		}

		/**
		 * Returns the index of the column which is used for the center bar.
		 * @return Index of the column which is used for the center bar.
		 */
		public int getColumnBarCenter() {
			return columnBarCenter;
		}

		/**
		 * Sets the index of the column which will be used for the center bar.
		 * @param columnBarCenter Index of the column which will be used for
		 * the center bar.
		 */
		public void setColumnBarCenter(int columnBarCenter) {
			this.columnBarCenter = columnBarCenter;
		}

		/**
		 * Returns the index of the column which is used for the bottom bar.
		 * @return Index of the column which is used for the bottom bar.
		 */
		public int getColumnBarBottom() {
			return columnBarBottom;
		}

		/**
		 * Sets the index of the column which will be used for the bottom bar.
		 * @param columnBarBottom Index of the column which will be used for
		 * the bottom bar.
		 */
		public void setColumnBarBottom(int columnBarBottom) {
			this.columnBarBottom = columnBarBottom;
		}

		/**
		 * Returns the index of the column which is used for the bottom edge of
		 * the box.
		 * @return Index of the column which is used for the bottom edge of the
		 * box.
		 */
		public int getColumnBoxBottom() {
			return columnBoxBottom;
		}

		/**
		 * Sets the index of the column which will be used for the bottom edge
		 * of the box.
		 * @param columnBoxBottom Index of the column which will be used for
		 * the bottom edge of the box.
		 */
		public void setColumnBoxBottom(int columnBoxBottom) {
			this.columnBoxBottom = columnBoxBottom;
		}

		/**
		 * Returns the index of the column which is used for the top edge of
		 * the box.
		 * @return Index of the column which is used for the top edge of the
		 * box.
		 */
		public int getColumnBoxTop() {
			return columnBoxTop;
		}

		/**
		 * Sets the index of the column which will be used for the top edge of
		 * the box.
		 * @param columnBoxTop Index of the column which will be used for the
		 * top edge of the box.
		 */
		public void setColumnBoxTop(int columnBoxTop) {
			this.columnBoxTop = columnBoxTop;
		}

		/**
		 * Returns the index of the column which is used for the top bar.
		 * @return Index of the column which is used for the top bar.
		 */
		public int getColumnBarTop() {
			return columnBarTop;
		}

		/**
		 * Sets the index of the column which will be used for the top bar.
		 * @param columnBarTop Index of the column which will be used for the
		 * top bar.
		 */
		public void setColumnBarTop(int columnBarTop) {
			this.columnBarTop = columnBarTop;
		}

		/**
		 * Returns the relative width of the box.
		 * @return Relative width of the box.
		 */
		public double getBoxWidth() {
			return boxWidth;
		}

		/**
		 * Sets the relative width of the box.
		 * @param boxWidth Relative width of the box.
		 */
		public void setBoxWidth(double boxWidth) {
			this.boxWidth = boxWidth;
		}

		/**
		 * Returns the mapping which is used to fill the background of a box.
		 * @return {@code ColorMapper} instance which is used to fill the
		 * background of a box.
		 */
		public ColorMapper getBoxBackground() {
			return boxBackground;
		}

		/**
		 * Sets the mapping which will be used to fill the background of a box.
		 * @param color {@code ColorMapper} instance which will be used to fill
		 * the background of a box.
		 */
		public void setBoxBackground(ColorMapper color) {
			this.boxBackground = color;
		}

		/**
		 * Sets the paint which will be used to fill the background of a box.
		 * @param color {@code Paint} instance which will be used to fill the
		 * background of a box.
		 */
		public void setBoxBackground(Paint color) {
			setBoxBackground(new SingleColor(color));
		}

		/**
		 * Returns the paint which is used to fill the border of a box and the
		 * lines of bars.
		 * @return Paint which is used to fill the border of a box and the
		 * lines of bars.
		 */
		public Paint getBoxColor() {
			return boxColor;
		}

		/**
		 * Sets the paint which will be used to fill the border of a box and
		 * the lines of bars.
		 * @param color Paint which will be used to fill the border of a box
		 * and the lines of bars.
		 */
		public void setBoxColor(Paint color) {
			this.boxColor = color;
		}

		/**
		 * Returns the stroke which is used to paint the border of a box and
		 * the lines of the bars.
		 * @return {@code Stroke} instance which is used to paint the border of
		 * a box and the lines of the bars.
		 */
		public Stroke getBoxBorder() {
			return boxBorder;
		}

		/**
		 * Sets the stroke which will be used to paint the border of a box and
		 * the lines of the bars.
		 * @param stroke {@code Stroke} instance which will be used to paint
		 * the border of a box and the lines of the bars.
		 */
		public void setBoxBorder(Stroke stroke) {
			this.boxBorder = stroke;
		}

		/**
		 * Returns the paint which is used to fill the lines of the whiskers.
		 * @return Paint which is used to fill the lines of the whiskers.
		 */
		public Paint getWhiskerColor() {
			return whiskerColor;
		}

		/**
		 * Sets the paint which will be used to fill the lines of the whiskers.
		 * @param color Paint which will be used to fill the lines of the
		 * whiskers.
		 */
		public void setWhiskerColor(Paint color) {
			this.whiskerColor = color;
		}

		/**
		 * Returns the stroke which is used to paint the lines of the whiskers.
		 * @return {@code Stroke} instance which is used to paint the lines of
		 * the whiskers.
		 */
		public Stroke getWhiskerStroke() {
			return whiskerStroke;
		}

		/**
		 * Sets the stroke which will be used to paint the lines of the
		 * whiskers.
		 * @param stroke {@code Stroke} instance which will be used to paint
		 * the lines of the whiskers.
		 */
		public void setWhiskerStroke(Stroke stroke) {
			this.whiskerStroke = stroke;
		}

		/**
		 * Returns the relative width of the bottom and top bars.
		 * @return Relative width of the bottom and top bars.
		 */
		public double getBarWidth() {
			return barWidth;
		}

		/**
		 * Sets the relative width of the bottom and top bars.
		 * @param width Relative width of the bottom and top bars.
		 */
		public void setBarWidth(double width) {
			this.barWidth = width;
		}

		/**
		 * Returns the paint which is used to fill the lines of the center bar.
		 * @return Paint which is used to fill the lines of the center bar.
		 */
		public Paint getBarCenterColor() {
			return barCenterColor;
		}

		/**
		 * Sets the paint which will be used to fill the lines of the center
		 * bar.
		 * @param color Paint which will be used to fill the lines of the
		 * center bar.
		 */
		public void setBarCenterColor(Paint color) {
			this.barCenterColor = color;
		}

		/**
		 * Returns the stroke which is used to paint the lines of the center
		 * bar.
		 * @return {@code Stroke} instance which is used to paint the lines of
		 * the center bar.
		 */
		public Stroke getBarCenterStroke() {
			return barCenterStroke;
		}

		/**
		 * Sets the stroke which will be used to paint the lines of the
		 * center bar.
		 * @param stroke {@code Stroke} instance which will be used to paint
		 * the lines of the center bar.
		 */
		public void setBarCenterStroke(Stroke stroke) {
			this.barCenterStroke = stroke;
		}

		/**
		 * Returns the graphical representation to be drawn for the specified
		 * data value.
		 * @param data Information on axes, renderers, and values.
		 * @param shape Outline that describes the point's shape.
		 * @return Component that can be used to draw the point
		 */
		public Drawable getPoint(final PointData data, final Shape shape) {
			return new AbstractDrawable() {
				/** Version id for serialization. */
				private static final long serialVersionUID = 2765031432328349977L;

				public void draw(DrawingContext context) {
					Axis axisX = data.axes.get(0);
					Axis axisY = data.axes.get(1);
					AxisRenderer axisXRenderer = data.axisRenderers.get(0);
					AxisRenderer axisYRenderer = data.axisRenderers.get(1);
					Row row = data.row;

					// Get the values from data columns
					BoxWhiskerRenderer renderer =  BoxWhiskerRenderer.this;
					int colPos = renderer.getColumnPosition();
					int colBarCenter = renderer.getColumnBarCenter();
					int colBarBottom = renderer.getColumnBarBottom();
					int colBoxBottom = renderer.getColumnBoxBottom();
					int colBoxTop = renderer.getColumnBoxTop();
					int colBarTop = renderer.getColumnBarTop();

					if (!row.isColumnNumeric(colPos) ||
							!row.isColumnNumeric(colBarCenter) ||
							!row.isColumnNumeric(colBarBottom) ||
							!row.isColumnNumeric(colBoxBottom) ||
							!row.isColumnNumeric(colBoxTop) ||
							!row.isColumnNumeric(colBarTop)) {
						return;
					}

					double valueX = ((Number) row.get(colPos)).doubleValue();
					double valueYBarBottom = ((Number) row.get(colBarBottom)).doubleValue();
					double valueYBoxBottom = ((Number) row.get(colBoxBottom)).doubleValue();
					double valueYBarCenter = ((Number) row.get(colBarCenter)).doubleValue();
					double valueYBoxTop = ((Number) row.get(colBoxTop)).doubleValue();
					double valueYBarTop = ((Number) row.get(colBarTop)).doubleValue();

					// Calculate positions in screen units
					double boxWidthRel = getBoxWidth();
					double boxAlign = 0.5;
					// Box X
					double boxXMin = axisXRenderer
						.getPosition(axisX, valueX - boxWidthRel*boxAlign, true, false)
						.get(PointND.X);
					double boxX = axisXRenderer.getPosition(
						axisX, valueX, true, false).get(PointND.X);
					double boxXMax = axisXRenderer
						.getPosition(axisX, valueX + boxWidthRel*boxAlign, true, false)
						.get(PointND.X);
					// Box Y
					double barYbottom = axisYRenderer.getPosition(
						axisY, valueYBarBottom, true, false).get(PointND.Y);
					double boxYBottom = axisYRenderer.getPosition(
						axisY, valueYBoxBottom, true, false).get(PointND.Y);
					double barYCenter = axisYRenderer.getPosition(
						axisY, valueYBarCenter, true, false).get(PointND.Y);
					double boxYTop = axisYRenderer.getPosition(
						axisY, valueYBoxTop, true, false).get(PointND.Y);
					double barYTop = axisYRenderer.getPosition(
						axisY, valueYBarTop, true, false).get(PointND.Y);
					double boxWidth = Math.abs(boxXMax - boxXMin);
					// Bars
					double barWidthRel = getBarWidth();
					double barXMin = boxXMin + (1.0 - barWidthRel)*boxWidth/2.0;
					double barXMax = boxXMax - (1.0 - barWidthRel)*boxWidth/2.0;

					// Create shapes
					// The origin of all shapes is (boxX, boxY)
					Rectangle2D boxBounds = new Rectangle2D.Double(
						boxXMin - boxX, boxYTop - barYCenter,
						boxWidth, Math.abs(boxYTop - boxYBottom));
					Rectangle2D shapeBounds = shape.getBounds2D();
					AffineTransform tx = new AffineTransform();
					tx.translate(boxBounds.getX(), boxBounds.getY());
					tx.scale(boxBounds.getWidth()/shapeBounds.getWidth(),
						boxBounds.getHeight()/shapeBounds.getHeight());
					tx.translate(-shapeBounds.getMinX(), -shapeBounds.getMinY());
					Shape box = tx.createTransformedShape(shape);

					Line2D whiskerMax = new Line2D.Double(
						0.0, boxYTop - barYCenter,
						0.0, barYTop - barYCenter
					);
					Line2D whiskerMin = new Line2D.Double(
						0.0, boxYBottom - barYCenter,
						0.0, barYbottom - barYCenter
					);
					Line2D barMax = new Line2D.Double(
						barXMin - boxX, barYTop - barYCenter,
						barXMax - boxX, barYTop - barYCenter
					);
					Line2D barMin = new Line2D.Double(
						barXMin - boxX, barYbottom - barYCenter,
						barXMax - boxX, barYbottom - barYCenter
					);
					Line2D barCenter = new Line2D.Double(
						boxXMin - boxX, 0.0,
						boxXMax - boxX, 0.0
					);

					// Paint shapes
					Graphics2D graphics = context.getGraphics();
					ColorMapper paintBoxMapper = getBoxBackground();
					Paint paintBox;
					if (paintBoxMapper instanceof ContinuousColorMapper) {
						paintBox = ((ContinuousColorMapper) paintBoxMapper)
							.get(valueX);
					} else {
						Integer index = Integer.valueOf(row.getIndex());
						paintBox = paintBoxMapper.get(index);
					}
					Paint paintStrokeBox = getBoxColor();
					Stroke strokeBox = getBoxBorder();
					Paint paintWhisker = getWhiskerColor();
					Stroke strokeWhisker = getWhiskerStroke();
					Paint paintBarCenter = getBarCenterColor();
					Stroke strokeBarCenter = getBarCenterStroke();
					// Fill box
					GraphicsUtils.fillPaintedShape(
						graphics, box, paintBox, box.getBounds2D());
					// Save current graphics state
					Paint paintOld = graphics.getPaint();
					Stroke strokeOld = graphics.getStroke();
					// Draw whiskers
					graphics.setPaint(paintWhisker);
					graphics.setStroke(strokeWhisker);
					graphics.draw(whiskerMax);
					graphics.draw(whiskerMin);
					// Draw box and bars
					graphics.setPaint(paintStrokeBox);
					graphics.setStroke(strokeBox);
					graphics.draw(box);
					graphics.draw(barMax);
					graphics.draw(barMin);
					graphics.setPaint(paintBarCenter);
					graphics.setStroke(strokeBarCenter);
					graphics.draw(barCenter);
					// Restore previous graphics state
					graphics.setStroke(strokeOld);
					graphics.setPaint(paintOld);
				}
			};
		}

		/**
		 * Returns a {@code Shape} instance that can be used for further
		 * calculations.
		 * @param data Information on axes, renderers, and values.
		 * @return Outline that describes the point's shape.
		 */
		public Shape getPointShape(PointData data) {
			return getShape();
		}

		/**
		 * Returns a graphical representation of the value label to be drawn for
		 * the specified data value.
		 * @param data Information on axes, renderers, and values.
		 * @param shape Outline that describes the bounds for the value label.
		 * @return Component that can be used to draw the value label.
		 */
		public Drawable getValue(final PointData data, final Shape shape) {
			Drawable drawable = new AbstractDrawable() {
				/** Version id for serialization. */
				private static final long serialVersionUID = 6788431763837737592L;

				public void draw(DrawingContext context) {
					// TODO Implement rendering of value label
				}
			};
			return drawable;
		}
	}

	/**
	 * A legend implementation for box-and-whisker plots that displays all
	 * values of the data source as items.
	 */
	public static class BoxPlotLegend extends ValueLegend {
		/** Version id for serialization. */
		private static final long serialVersionUID = 1517792984459627757L;
		/** Source for dummy data. */
		@SuppressWarnings("unchecked")
		private static final DataSource DUMMY_DATA = new AbstractDataSource(
				Double.class, Double.class, Double.class,
				Double.class, Double.class, Double.class) {
			/** Version id for serialization. */
			private static final long serialVersionUID = -8233716728143117368L;

			/** Positions of x position, center bar, bottom bar, box bottom,
			box top, and top bar. */
			private final Double[] values = { 0.5, 0.0, 0.0, 1.0, 1.0 };

			/**
			 * Returns the number of rows of the data source.
			 * @return number of rows in the data source.
			 */
			public int getRowCount() {
				return 1;
			}

			/**
			 * Returns the value with the specified row and column index.
			 * @param col index of the column to return
			 * @param row index of the row to return
			 * @return the specified value of the data cell
			 */
			public Comparable<?> get(int col, int row) {
				if (col == 0) {
					return Double.valueOf(row + 1);
				}
				return values[col - 1];
			}
		};

		/** Associated plot. */
		private final BoxPlot plot;

		/**
		 * Initializes a new instance with the specified plot.
		 * @param plot Associated plot.
		 */
		public BoxPlotLegend(BoxPlot plot) {
			this.plot = plot;
		}

		/**
		 * Returns a symbol for rendering a legend item.
		 * @param row Data row.
		 * @return A drawable object that can be used to display the symbol.
		 */
		public Drawable getSymbol(final Row row) {
			return new AbstractSymbol(this) {
				/** Version id for serialization. */
				private static final long serialVersionUID = 1906894939358065143L;

				/**
				 * Draws the {@code Drawable} with the specified drawing context.
				 * @param context Environment used for drawing
				 */
				public void draw(DrawingContext context) {
					DataSource data = row.getSource();

					BoxWhiskerRenderer pointRenderer =
							(BoxWhiskerRenderer) plot.getPointRenderer(data);
					if (pointRenderer == null) {
						return;
					}

					Row symbolRow = new Row(DUMMY_DATA, row.getIndex());
					Rectangle2D bounds = getBounds();

					double boxWidthRel = pointRenderer.getBoxWidth();

					double posX = ((Number) row.get(0)).doubleValue();
					Axis axisX = new Axis(posX - boxWidthRel/2.0, posX + boxWidthRel/2.0);
					AxisRenderer axisRendererX = new LinearRenderer2D();
					axisRendererX.setShape(new Line2D.Double(
							bounds.getMinX(), bounds.getMaxY(),
							bounds.getMaxX(), bounds.getMaxY()));
					Axis axisY = new Axis(1.0, 2.0);
					AxisRenderer axisRendererY = new LinearRenderer2D();
					axisRendererY.setShape(new Line2D.Double(
							bounds.getMinX(), bounds.getMaxY(),
							bounds.getMinX(), bounds.getMinY()));

					PointData pointData = new PointData(
						Arrays.asList(axisX, axisY),
						Arrays.asList(axisRendererX, axisRendererY),
						symbolRow, 0);
					Shape shape = pointRenderer.getPointShape(pointData);
					Drawable drawable = pointRenderer.getPoint(pointData, shape);
					Drawable labelDrawable = pointRenderer.getValue(pointData, shape);

					DataPoint point = new DataPoint(pointData,
						new PointND<Double>(bounds.getCenterX(),
						bounds.getCenterY()), drawable, shape, labelDrawable);

					Graphics2D graphics = context.getGraphics();
					graphics.draw(bounds);
					Point2D pos = point.position.getPoint2D();
					AffineTransform txOrig = graphics.getTransform();
					graphics.translate(pos.getX(), pos.getY());
					point.drawable.draw(context);
					graphics.setTransform(txOrig);
				}
			};
		}
	}

	/**
	 * Initializes a new box-and-whisker plot with the specified data source.
	 * @param data Data to be displayed.
	 */
	public BoxPlot(DataSource data) {
		setLegend(new BoxPlotLegend(this));

		getPlotArea().setSettingDefault(XYPlotArea2D.GRID_MAJOR_X, false);
		getAxisRenderer(AXIS_X).setSetting(AxisRenderer.TICKS_SPACING, 1.0);
		getAxisRenderer(AXIS_X).setSetting(AxisRenderer.TICKS_MINOR, false);
		getAxisRenderer(AXIS_X).setIntersection(-Double.MAX_VALUE);
		getAxisRenderer(AXIS_Y).setIntersection(-Double.MAX_VALUE);

		add(data);
		autoscaleAxes();
	}

	/**
	 * Extracts statistics from the columns of an data source that are commonly
	 * used for box-and-whisker plots. The result is a new data source
	 * containing <i>column index</i>, <i>median</i>, <i>mininum</i>, <i>first
	 * quartile</i>, <i>third quartile</i>, and <i>maximum</i> for each column.
	 * @param data Original data source
	 * @return New data source with (columnIndex, median, min, quartile1,
	 *         quartile3, max)
	 */
	@SuppressWarnings("unchecked")
	public static DataSource createBoxData(DataSource data) {
		if (data == null) {
			throw new NullPointerException(
				"Cannot extract statistics from null data source.");
		}

		DataTable stats = new DataTable(Integer.class, Double.class,
			Double.class, Double.class, Double.class, Double.class);

		// Generate statistical values for each column
		for (int c = 0; c < data.getColumnCount(); c++) {
			Column col = data.getColumn(c);
			if (!col.isNumeric()) {
				continue;
			}
			stats.add(
				c + 1,
				col.getStatistics(Statistics.MEDIAN),
				col.getStatistics(Statistics.MIN),
				col.getStatistics(Statistics.QUARTILE_1),
				col.getStatistics(Statistics.QUARTILE_3),
				col.getStatistics(Statistics.MAX)
			);
		}
		return stats;
	}

	@Override
	public void add(int index, DataSource source, boolean visible) {
		if (getData().size() > 0) {
			throw new IllegalArgumentException(
				"This plot type only supports a single data source."); //$NON-NLS-1$
		}
		super.add(index, source, visible);
		setLineRenderer(source, null);
		setPointRenderer(source, new BoxWhiskerRenderer());
	}

	@Override
	public void autoscaleAxis(String axisName) {
		if (AXIS_X.equals(axisName) || AXIS_Y.equals(axisName)) {
			Axis axis = getAxis(axisName);
			if (axis == null || !axis.isAutoscaled()) {
				return;
			}

			List<DataSource> sources = getData();
			if (sources.isEmpty()) {
				return;
			}
			DataSource data = getData().get(0);

			if (AXIS_X.equals(axisName)) {
				Column col0 = data.getColumn(0);
				axis.setRange(
					col0.getStatistics(Statistics.MIN) - 0.5,
					col0.getStatistics(Statistics.MAX) + 0.5);
			} else if (AXIS_Y.equals(axisName)) {
				double yMin = data.getColumn(2).getStatistics(Statistics.MIN);
				double yMax = data.getColumn(5).getStatistics(Statistics.MAX);
				double ySpacing = 0.05*(yMax - yMin);
				axis.setRange(yMin - ySpacing, yMax + ySpacing);
			}
		} else {
			super.autoscaleAxis(axisName);
		}
	}
}
