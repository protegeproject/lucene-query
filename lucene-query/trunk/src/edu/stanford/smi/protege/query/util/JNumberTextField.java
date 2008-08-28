package edu.stanford.smi.protege.query.util;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Only allows numbers in the textfield.  Can't be blank.
 * When the textfield loses focus, the text is converted into a number, if it fails then the default
 * value (defaults to 0) is put in the textfield.  This ensures that a valid number (integer, double, etc) 
 * will always be present except while the textfield is being editted.
 * You can also tell it to only allow integers.
 * 
 * @author Chris Callendar
 */
public class JNumberTextField extends JTextField implements FocusListener {

	private final double defaultValue;
	private final boolean allowOnlyIntegers;
	
	/**
	 * Initializes the textfield with a default value of 0 and allows doubles.
	 */
	public JNumberTextField() {
		this(0, false);
	}

	/**
	 * Initializes the textfield with a default value of 0.
	 * If <code>allowOnlyIntegers</code> is true then the number
	 * will always be an integer.
	 */
	public JNumberTextField(boolean allowOnlyIntegers) {
		this(0, allowOnlyIntegers);
	}
	
	
	/**
	 * @param intValue default/starting integer value
	 * @param allowOnlyIntegers if only integers are allowed
	 */
	public JNumberTextField(int intValue, boolean allowOnlyIntegers) {
		super(Integer.toString(intValue));
		this.defaultValue = intValue;
		this.allowOnlyIntegers = allowOnlyIntegers;
		init(Integer.toString(intValue));
	}

	/**
	 * @param floatValue default/starting float value
	 */
	public JNumberTextField(float floatValue) {
		super(Float.toString(floatValue));
		this.defaultValue = floatValue;
		this.allowOnlyIntegers = false;
		init(Float.toString(floatValue));
	}

	/**
	 * @param doubleValue default/starting double value
	 */
	public JNumberTextField(double doubleValue) {
		super(Double.toString(doubleValue));
		this.defaultValue = doubleValue;
		this.allowOnlyIntegers = false;
		init(Double.toString(doubleValue));
	}
	
	private void init(String txt) {
		setDocument(new NumberDocument());
		this.addFocusListener(this);
		setText(txt);
		setSelectionStart(0);
		setSelectionEnd(txt.length());
	}
	
	public void focusGained(FocusEvent e) {
	}
	
	public void focusLost(FocusEvent e) {
		setText(String.valueOf(getNumber()));
	}
	
	public int getIntegerText() {
		return (int)getNumber();
	}
	
	public float getFloatText() {
		return (float)getNumber();
	}

	public double getDoubleText() {
		return getNumber();
	}
	
	/**
	 * Gets the parsed double value.  If a {@link NumberFormatException} occurs then the default 
	 * value is returned.
	 * @return double
	 */
	private double getNumber() {
		try {
			return (new Double(getText()).doubleValue());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	class NumberDocument extends PlainDocument {
		
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			str = str.trim();
			if (testInsert(offs, str)) {
				super.insertString(offs, str, a);
			}
		}
		
		/**
		 * Inserts the <code>str</code> at the given offset and tests if it is still a valid
		 * number.  Allows the new string to start with +, -, +., -. or . 
		 * Allowed characters are the {+-[0-9].} 
		 */
		private boolean testInsert(int offs, String str) {
				
			String orig = JNumberTextField.this.getText();
			String newStr = orig.substring(0, offs) + str + orig.substring(offs);
			
			// if we only allow integers then don't allow periods
			boolean ok = false;
			if ("+".equals(newStr) || "-".equals(newStr) || ".".equals(newStr) || 
				"+.".equals(newStr) || "-.".equals(newStr) || isNumber(newStr)) {
				ok = true;
			}
			if (allowOnlyIntegers && (newStr.indexOf('.') != -1)) { 
				ok = false;
			}			
			return ok;
		}
		
		private boolean isNumber(String newStr) {
			try {
				Double.parseDouble(newStr);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}	
	
	public static void main(String[] args) {
		javax.swing.JDialog dlg = new javax.swing.JDialog();
		dlg.setModal(true);
		dlg.setSize(250, 60);
		dlg.setLocation(200, 150);
		JNumberTextField txt = new JNumberTextField(3, true);
		dlg.getContentPane().add(txt, java.awt.BorderLayout.CENTER);
		dlg.setVisible(true);
		
		System.out.println("Int:    " + txt.getIntegerText());
		System.out.println("Float:  " + txt.getFloatText());
		System.out.println("Double: " + txt.getDoubleText());
		
		System.exit(0);
	}

}
