package edu.stanford.smi.protege.query.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * A {@link JButton} that can show a {@link JProgressBar} inside it (hiding the text and icon).
 *
 * @author Chris Callendar
 * @date 15-Sep-06
 */
public class JProgressButton extends JButton {

	protected final int MIN = 0;
	protected final int MAX = 20;
	private long sleep = 50;
	private ProgressRunnable runnable = null;
	private JProgressBar bar;
	
	class ProgressRunnable extends Thread {
		private boolean running = false;
		public void run() {
			// already running?
			if (running)
				return;
			
			bar.setVisible(true);
			String text = getText();
			Icon icon = getIcon();
			setText(null);
			setIcon(null);
			
			running = true;
			while (running) {
				bar.setValue((bar.getValue() + 1) % bar.getMaximum());
				bar.repaint();
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			bar.setVisible(false);
			setText(text);
			setIcon(icon);
		}
		
		public synchronized void stopRunning() {
			running = false;
		}
	}
	
	public JProgressButton() {
		super();
		initialize();
	}

	public JProgressButton(Action a) {
		super(a);
		initialize();
	}

	public JProgressButton(String text) {
		super(text);
		initialize();
	}

	public JProgressButton(Icon icon) {
		super(icon);
		initialize();
	}

	public JProgressButton(String text, Icon icon) {
		super(text, icon);
		initialize();
	}

	private void initialize() {
		setLayout(new BorderLayout(0, 0));		
		bar = new JProgressBar(MIN, MAX);
		bar.setVisible(false);
		bar.setStringPainted(true);
		bar.setBackground(Color.white);
		bar.setForeground(Color.green);
		add(bar, BorderLayout.CENTER);
	}
	
	/**
	 * Sets the speed.
	 * @param speed an int from 1 - 10, 10 is fastest
	 */
	public void setSpeed(int speed) {
		int speedInverse = 11 - Math.max(1, Math.min(10, speed));	// from 1 - 10
		sleep = speedInverse * 10L;
	}
	
	/**
	 * Sets the foreground and background colors for the progress bar.
	 * @param foreground
	 * @param background
	 */
	public void setProgressColors(Color foreground, Color background) {
		bar.setForeground(foreground);
		bar.setBackground(background);
	}
	
	/**
	 * Makes the progress bar visible.
	 */
	public void showProgressBar() {
		showProgressBar(null);
	}
	
	/**
	 * Makes the progress bar visible and paints the string onto the progress bar.
	 * @param msg the String to paint on the progress bar
	 */
	public void showProgressBar(String msg) {
		bar.setStringPainted((msg != null));
		bar.setString(msg); 
		runnable = new ProgressRunnable();
		runnable.start();
	}
	
	/**
	 * Hides the progress bar.
	 */
	public void hideProgressBar() {
		if (runnable != null) {
			runnable.stopRunning();
		}
	}
	
	public static void main(String[] args) {
		JDialog dlg = new JDialog();
		dlg.setModal(true);
		dlg.setTitle("Test");
		
		JPanel pnl = new JPanel();
		pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		final JProgressButton btn = new JProgressButton();
		Action action = new AbstractAction("Start") {
			public void actionPerformed(ActionEvent e) {
				btn.showProgressBar();
				new Thread(new Runnable() {
					public void run() {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e1) {
						}
						btn.hideProgressBar();
					}
				}).start();
			}
		};
		btn.setAction(action);
		btn.setPreferredSize(new Dimension(200, 24));
		pnl.add(btn);
		dlg.getContentPane().add(pnl, BorderLayout.CENTER);
		
		dlg.setSize(550, 150);
		dlg.setVisible(true);
		System.exit(0);
		
	}

}
