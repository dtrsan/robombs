package robombs.game.gui;

import java.awt.Font;

import robombs.game.util.*;

import com.threed.jpct.*;
import com.threed.jpct.util.*;

/**
 * A simple table component with no borders, no resizing, no nothing...just data
 * organized in rows and columns.
 */
public class Table extends GUIComponent {

	private int xs = 0;
	private int ys = 0;
	private int xe = 0;
	private int ye = 0;
	private int rows = 0;
	private int cols = 0;
	private GUIListener bl = null;
	private Object[][] content = null;
	private int[] sizeCol = null;
	private float[] sizeRow = null;
	private String label = "";
	private boolean clicked = false;

	private GLFont font = GLFont.getGLFont(new Font("Arial", Font.BOLD, 15));

	/**
	 * Create a new table. The rows and columns will be sized according to the
	 * size of the actual table.
	 * 
	 * @param name
	 *            the name of the table (not displayed)
	 * @param rows
	 *            the number of rows
	 * @param cols
	 *            the number of columns
	 * @param xs
	 *            x-value of the upper left corner
	 * @param ys
	 *            y-value of the upper left corner
	 * @param xe
	 *            x-value of the lower right corner
	 * @param ye
	 *            y-value of the lower right corner
	 */
	public Table(String name, int rows, int cols, int xs, int ys, int xe, int ye) {
		this.xs = xs;
		this.ys = ys;
		this.xe = xe;
		this.ye = ye;
		
		this.xpos=xs;
		this.ypos=ys;
		
		this.rows = rows;
		this.cols = cols;
		this.label = name;

		content = new Object[rows][cols];
		sizeCol = new int[cols];
		sizeRow = new float[rows];
		for (int i = 0; i < cols; i++) {
			setColumnSize(i, (xe - xs) / cols);
		}
		for (int i = 0; i < rows; i++) {
			setRowSize(i, (float) (ye - ys) / (float) rows);
		}
	}

	public void setFontSize(int size) {
		font = GLFont.getGLFont(new Font("Arial", Font.BOLD, size));
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	/**
	 * Sets the content object of a cell. The object's value will be displayed
	 * using its toString()-method.
	 * 
	 * @param row
	 *            the row of the cell
	 * @param col
	 *            the column of the cell
	 * @param content
	 *            the cell's content
	 */
	public void setCell(int row, int col, Object content) {
		if (row >= 0 && row < rows && col >= 0 && col < cols) {
			this.content[row][col] = content;
		}
	}

	/**
	 * Gets the content object of a cell.
	 * 
	 * @param row
	 *            the row of the cell
	 * @param col
	 *            the column of the cell
	 * @return Object the cell's content
	 */
	public Object getCell(int row, int col) {
		if (row >= 0 && row < rows && col >= 0 && col < cols) {
			return content[row][col];
		}
		return null;
	}

	/**
	 * Inserts a cell by inserting a row at the given position.
	 * 
	 * @param row
	 *            the row of the cell
	 * @param col
	 *            the column of the cell
	 * @param content
	 *            the new cell's content
	 */
	public void insertCell(int row, int col, Object content) {
		for (int y = 1; y <= row; y++) {
			this.content[y - 1][col] = this.content[y][col];
		}
		setCell(row, col, content);
	}

	/**
	 * Sets the height of a row.
	 * 
	 * @param row
	 *            the row
	 * @param size
	 *            the new height
	 */
	public void setRowSize(int row, float size) {
		sizeRow[row] = size;
	}

	/**
	 * Sets the width of a column.
	 * 
	 * @param col
	 *            the column
	 * @param size
	 *            the new width
	 */
	public void setColumnSize(int col, int size) {
		sizeCol[col] = size;
	}

	public int getWidth() {
		int x = 0;
		for (int i = 0; i < cols; i++) {
			x += sizeCol[i];
		}
		return x;
	}

	public int getHeight() {
		float y = 0;
		for (int i = 0; i < rows; i++) {
			y += sizeRow[i];
		}
		return (int) y;
	}

	/**
	 * Clears the table. Rows and columns will remain but the data will be
	 * cleared.
	 */
	public void clear() {
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				content[y][x] = null;
			}
		}
	}

	/**
	 * Sets a listener that will be informed of clicks into a cell.
	 * 
	 * @param bl
	 *            the GUIListener
	 */
	public void setListener(GUIListener bl) {
		this.bl = bl;
	}

	public boolean evaluateInput(MouseMapper mouse, KeyMapper keyMapper) {
		boolean has = super.evaluateInput(mouse, keyMapper);
		if (!has && isVisible()) {
			if (mouse.buttonDown(0)) {
				// Klick...
				if (!clicked) {
					int x = mouse.getMouseX();
					int y = mouse.getMouseY();
					if (x > xs + getParentX() && x < xe + getParentX() && y > ys + getParentY() && y < ye + getParentY()) {
						int row = 0;
						for (int yi = 0; yi < rows; yi++) {
							float yr = 0;
							for (int i = 0; i < yi; i++) {
								yr += sizeRow[i];
							}
							if (y > yr + getParentY() + ys) {
								row = yi;
							} else {
								break;
							}
						}
						if (bl != null) {
							bl.elementChanged(label, Integer.toString(row));
						}
					}
					clicked = true;
				}
			} else {
				clicked = false;
			}
		}
		return false;
	}

	public void draw(FrameBuffer buffer) {
		if (visible) {
			for (int y = 0; y < rows; y++) {
				float yp = getParentY() + ys;
				for (int i = y; i > 0; i--) {
					yp += sizeRow[i - 1];
				}

				for (int x = 0; x < cols; x++) {
					int xp = getParentX() + xs;
					for (int i = x; i > 0; i--) {
						xp += sizeCol[i - 1];
					}

					Object c = content[y][x];
					if (c != null) {
						TextBlitter.blitText(font, buffer, c.toString(), xp, (int) yp, null);
					}
				}
			}
			super.draw(buffer);
		}
	}
}
