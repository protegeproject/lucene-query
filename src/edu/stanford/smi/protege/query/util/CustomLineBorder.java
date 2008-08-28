package edu.stanford.smi.protege.query.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Allows you to choose a color for each side and a thickness for each side.
 *
 * @see EmptyBorder
 * @see LineBorder 
 * @author Chris Callendar
 * @date 14-Sep-06
 */
public class CustomLineBorder extends EmptyBorder {

	private final int TOP = 0, LEFT = 1, BOTTOM = 2, RIGHT = 3;
	protected final Color[] colors = new Color[4]; 
	
	public CustomLineBorder(Color color, int top, int left, int bottom, int right) {
		super(top, left, bottom, right);
		setLineColors(color, color, color, color);
	}

	public CustomLineBorder(Color topColor, Color leftColor, Color bottomColor, Color rightColor, int thickness) {
		super(thickness, thickness, thickness, thickness);
		setLineColors(topColor, leftColor, bottomColor, rightColor);
	}
	
	
	public CustomLineBorder(Color topColor, Color leftColor, Color bottomColor, Color rightColor, 
							int top, int left, int bottom, int right) {
		super(top, left, bottom, right);
		setLineColors(topColor, leftColor, bottomColor, rightColor);
	}
	
	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();
        if ((colors[TOP] != null) && (top > 0)) {
        	g.setColor(colors[TOP]);
        	g.fillRect(x, y, width, top);
        }
        if ((colors[LEFT] != null) && (left > 0)) {
        	g.setColor(colors[LEFT]);
        	g.fillRect(x, y, left, height);
        }
        if ((colors[BOTTOM] != null) && (bottom > 0)) {
        	g.setColor(colors[BOTTOM]);
        	g.fillRect(x, y + height - bottom, width, bottom);
        }
        if ((colors[RIGHT] != null) && (right > 0)) {
	        g.setColor(colors[RIGHT]);
	        g.fillRect(x + width - right, y, right, height);
        }
        g.setColor(oldColor);
	}
	
	/**
	 * Sets the line colors - can be null.
	 */
	public void setLineColors(Color topColor, Color leftColor, Color bottomColor, Color rightColor) {
		colors[TOP] = topColor;
		colors[LEFT] = leftColor;
		colors[BOTTOM] = bottomColor;
		colors[RIGHT] = rightColor;
	}
	
	/**
	 * Sets the thicknesses for each side of the border.
	 */
	public void setLineThicknesses(int top, int left, int bottom, int right) {
        this.top = top; 
        this.right = right;
        this.bottom = bottom;
        this.left = left;
	}

	public static void main(String[] args) {
		JDialog dlg = new JDialog();
		dlg.setModal(true);

		JPanel pnl = new JPanel(new GridLayout(1, 2, 10, 10));
		pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JLabel lbl = new JLabel("Test", JLabel.CENTER);
		lbl.setBorder(new CustomLineBorder(Color.blue, Color.red, Color.green, Color.orange, 4, 2, 4, 2));
		pnl.add(lbl);
		lbl = new JLabel("Test2", JLabel.CENTER);
		lbl.setBorder(new CustomLineBorder(null, null, Color.lightGray, null, 2));
		pnl.add(lbl);
		dlg.getContentPane().add(pnl, BorderLayout.CENTER);
		
		dlg.setSize(400, 400);
		dlg.setLocation(400, 250);
		dlg.setVisible(true);
		System.exit(0);
	}

}
