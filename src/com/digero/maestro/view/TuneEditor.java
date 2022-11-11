package com.digero.maestro.view;

import java.awt.Color;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.digero.maestro.abc.AbcSong;
import com.digero.maestro.abc.TuneLine;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class TuneEditor {
	
	protected static Point lastLocation = new Point(100, 100);

	public static void show(JFrame jf, AbcSong abcSong) {
		@SuppressWarnings("serial")
		class TuneDialog extends JDialog {
			
			private final double[] LAYOUT_COLS = new double[] { 0.16,0.21,0.21,0.21,0.21 };
			private double[] LAYOUT_ROWS;
			private AbcSong abcSong;			
			
			private List<TuneEditorLine> tuneInputs = new ArrayList<TuneEditorLine>(SectionEditor.numberOfSections);
			     
	        JButton copySections = new JButton("Copy");
	        JButton pasteSections = new JButton("Paste");
	        		    
		    public TuneDialog(JFrame jf, String title, boolean modal, AbcSong abcSong) {
		        super(jf, title, modal);
		        this.abcSong = abcSong;
		        
		        TuneDialog.this.addWindowListener(new WindowAdapter() {

		            @Override
		            public void windowClosing(WindowEvent we) {
		            	TuneEditor.lastLocation = TuneDialog.this.getLocation();
		            }
		        });
		        
		        int w = 400;
		        int h = 271+21*SectionEditor.numberOfSections;
		        this.setSize(w,h);
		        JPanel panel=new JPanel();
		        
		        LAYOUT_ROWS = new double[3+SectionEditor.numberOfSections+1+10]; 
		        LAYOUT_ROWS[0] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[1] = 20;
		        LAYOUT_ROWS[2] = TableLayoutConstants.PREFERRED;
		        for (int l = 0;l<SectionEditor.numberOfSections;l++) {
		        	LAYOUT_ROWS[3+l] = TableLayoutConstants.PREFERRED;		        	
		        }
		        LAYOUT_ROWS[3+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[4+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[5+SectionEditor.numberOfSections] = TableLayoutConstants.FILL;
		        LAYOUT_ROWS[6+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[7+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[8+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[9+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[10+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[11+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[12+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[13+SectionEditor.numberOfSections] = TableLayoutConstants.PREFERRED;
		        
		        panel.setLayout(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		        //panel.add(new JLabel("<html><b> " + abcSong.getTitle() + "</html>"), "0, 0, 6, 0, C, C");
		        panel.add(new JLabel("Enable"), "0, 2, c, c");
		        panel.add(new JLabel("From bar"), "1, 2, c, c");
		        panel.add(new JLabel("To bar"), "2, 2, c, c");
		        panel.add(new JLabel("Seminote"), "3, 2, c, c");
		        panel.add(new JLabel("Tempo"), "4, 2, c, c");
		        //panel.add(new JLabel("Remove"), "4, 2, c, c");
		        
		        for (int j = 0;j<SectionEditor.numberOfSections;j++) {
		        	TuneEditorLine l = new TuneEditorLine();
		        	tuneInputs.add(l);
		        }
		        
		        TreeMap<Integer, TuneLine> tree = abcSong.tuneBars;
		        if (tree != null) {
			        int number = 0;
			        boolean useDialogLineNumbers = true;
			        for(Entry<Integer, TuneLine> entry : tree.entrySet()) {
			        	TuneLine ps = entry.getValue();
			        	if (ps.dialogLine == -1) {
			        		useDialogLineNumbers = false;
			        	}
			        	if (useDialogLineNumbers) {
			        		number = ps.dialogLine;
			        	}
		        		if (number >= SectionEditor.numberOfSections || number < 0) {
		        			System.err.println("Too many sections in treemap in tune-editor, or line numbers was badly edited in .msx file.");
		        		} else {
			        		tuneInputs.get(number).enable.setSelected(true);
			        		tuneInputs.get(number).barA.setText(""+ps.startBar);
			        		tuneInputs.get(number).barB.setText(""+ps.endBar);
			        		tuneInputs.get(number).transpose.setText(""+ps.seminoteStep);
			        		tuneInputs.get(number).tempo.setText(""+ps.tempo);
			        		//tuneInputs.get(number).remove.setSelected(ps.remove);
			        	}
			        	number ++;
			        }
		        }
		        		        
		        // Tooltips
		        String enable = "<html><b> Enable a specific section edit. </b><br> Pressing APPLY will disable a section if it is bad.</html>";
		        String barA = "<html><b> The start bar (inclusive) where this section edit starts. </b><br> Must be above 0 and greater than the 'From bar' of previous enabled section.</html>";
		        String barB = "<html><b> The end bar (inclusive) where this section edit end. </b><br> Must be equal or greater than the 'From bar'.</html>";
		        String transpose = "<html><b> Transpose this area some seminotes up or down. </b><br> Enter a positive or negative number. </html>";
		        //String remove = "<html><b> Remove this area. </b></html>";
		        String tempo = "<html><b> Change Tempo. </b></html>";
		        
		        
		        for (int i = 0;i<SectionEditor.numberOfSections;i++) {
		        	tuneInputs.get(i).tempo.setToolTipText(tempo);
		        	//tuneInputs.get(i).remove.setToolTipText(remove);
		        	tuneInputs.get(i).transpose.setToolTipText(transpose);
		        	tuneInputs.get(i).barB.setToolTipText(barB);
		        	tuneInputs.get(i).barA.setToolTipText(barA);
		        	tuneInputs.get(i).enable.setToolTipText(enable);
		        	
		        	tuneInputs.get(i).barA.setHorizontalAlignment(JTextField.CENTER);
		        	tuneInputs.get(i).barB.setHorizontalAlignment(JTextField.CENTER);
		        	tuneInputs.get(i).transpose.setHorizontalAlignment(JTextField.CENTER);
		        	tuneInputs.get(i).tempo.setHorizontalAlignment(JTextField.CENTER);
		        	
		        	panel.add(tuneInputs.get(i).enable, "0,"+(3+i)+",C,C");
			        panel.add(tuneInputs.get(i).barA, "1,"+(3+i)+",f,f");
			        panel.add(tuneInputs.get(i).barB, "2,"+(3+i)+",f,f");
			        panel.add(tuneInputs.get(i).transpose, "3,"+(3+i)+",f,f");
			        panel.add(tuneInputs.get(i).tempo, "4,"+(3+i)+",f,f");
			        //panel.add(tuneInputs.get(i).remove, "5,"+(3+i)+",c,f");
		        }
		        		        
		        copySections.getModel().addActionListener(new ActionListener() {
	                @Override
	                public void actionPerformed(ActionEvent e) {
	                	for (int i = 0; i < SectionEditor.numberOfSections; i++) {
	                		SectionEditor.clipboardStart[i] = tuneInputs.get(i).barA.getText();
	                		SectionEditor.clipboardEnd[i] = tuneInputs.get(i).barB.getText();
	                		SectionEditor.clipboardEnabled[i] = tuneInputs.get(i).enable.isSelected();
	                	}
	                	SectionEditor.clipboardArmed = true;
	                	pasteSections.setEnabled(SectionEditor.clipboardArmed);
	                }
	            });
		        copySections.setToolTipText("<html><b> Copy the section starts and ends.</html>");
		        panel.add(copySections, "1,"+(3+SectionEditor.numberOfSections)+",1,"+(3+SectionEditor.numberOfSections)+",f,f");
		        
		        pasteSections.getModel().addActionListener(new ActionListener() {
	                @Override
	                public void actionPerformed(ActionEvent e) {
	                	if (!SectionEditor.clipboardArmed) return; 
	                	for (int i = 0; i < SectionEditor.numberOfSections; i++) {
	                		tuneInputs.get(i).barA.setText(SectionEditor.clipboardStart[i]);
	                		tuneInputs.get(i).barB.setText(SectionEditor.clipboardEnd[i]);
	                		tuneInputs.get(i).enable.setSelected(SectionEditor.clipboardEnabled[i]);
	                	}
	                }
	            });
		        pasteSections.setToolTipText("<html><b> Paste the section starts and ends.</html>");
		        panel.add(pasteSections, "2,"+(3+SectionEditor.numberOfSections)+",2,"+(3+SectionEditor.numberOfSections)+",f,f");
		        pasteSections.setEnabled(SectionEditor.clipboardArmed);
		        
		        JButton okButton = new JButton("APPLY");
		        okButton.addActionListener(new ActionListener() {
		        	
					@Override
					public void actionPerformed(ActionEvent e) {
						TreeMap<Integer, TuneLine> tm = new TreeMap<Integer, TuneLine>();
						
						int lastEnd = 0;
						for (int k = 0;k<SectionEditor.numberOfSections;k++) {
							if (TuneDialog.this.tuneInputs.get(k).enable.isSelected()) {
								TuneLine ps = new TuneLine();
								try {
										ps.seminoteStep =  Integer.parseInt(tuneInputs.get(k).transpose.getText());
										if (ps.seminoteStep > 36) {
											ps.seminoteStep = 36;
											tuneInputs.get(k).transpose.setText("36");
										}
										if (ps.seminoteStep < -36) {
											ps.seminoteStep = -36;
											tuneInputs.get(k).transpose.setText("-36");
										}
										ps.startBar = Integer.parseInt(tuneInputs.get(k).barA.getText());
										ps.endBar = Integer.parseInt(tuneInputs.get(k).barB.getText());
										ps.tempo = Integer.parseInt(tuneInputs.get(k).tempo.getText());
										//ps.remove = tuneInputs.get(k).remove.isSelected();
										boolean soFarSoGood = true;
										for (TuneLine psC : tm.values()) {
											if (!(ps.startBar > psC.endBar || ps.endBar < psC.startBar)) {
												soFarSoGood = false;
											}
										}
										if (ps.startBar > 0 && ps.startBar <= ps.endBar && soFarSoGood) {
											tm.put(ps.startBar, ps);
											if (ps.endBar > lastEnd) lastEnd = ps.endBar;
											ps.dialogLine = k;
										} else {
											TuneDialog.this.tuneInputs.get(k).enable.setSelected(false);
										}
								} catch (NumberFormatException nfe) {
									TuneDialog.this.tuneInputs.get(k).enable.setSelected(false);
								}
							}
						}
						
						if (lastEnd == 0) {
							TuneDialog.this.abcSong.tuneBars = null;
							TuneDialog.this.abcSong.tuneBarsModified = null;
						} else {
							TuneDialog.this.abcSong.tuneBars = tm;
							
							boolean[] booleanArray = new boolean[lastEnd+1];
							for(int m = 0; m<lastEnd+1;m++) {
								Entry<Integer, TuneLine> entry = tm.floorEntry(m+1);
								booleanArray[m] = entry != null && entry.getValue().startBar <= m+1 && entry.getValue().endBar >= m+1;
							}
														
							TuneDialog.this.abcSong.tuneBarsModified = booleanArray;
						}
						TuneDialog.this.abcSong.tuneEdited();
						//System.err.println(Thread.currentThread().getName());
					}
				});
		        okButton.setToolTipText("<html><b> Apply the effects. </b><br> Note that non-applied effects will not be remembered when closing dialog.<br> Sections that are not enabled will likewise also not be remembered. </html>");
		        panel.add(okButton, "4,"+(4+SectionEditor.numberOfSections)+", 4, "+(4+SectionEditor.numberOfSections)+",f,f");
		        panel.add(new JLabel("Enabled sections must have no overlap."), "0,"+(6+SectionEditor.numberOfSections)+", 4," +(6+SectionEditor.numberOfSections)+", c, c");
		        panel.add(new JLabel("Bar numbers are inclusive and use original MIDI bars."), "0, "+(7+SectionEditor.numberOfSections)+", 4, "+(7+SectionEditor.numberOfSections)+", c, c");
		        panel.add(new JLabel("No decimal numbers allowed, only whole numbers."), "0, "+(8+SectionEditor.numberOfSections)+", 4," +(8+SectionEditor.numberOfSections)+", c, c");
		        panel.add(new JLabel("Bar numbers must be positive and greater than zero."), "0, "+(9+SectionEditor.numberOfSections)+", 4," +(9+SectionEditor.numberOfSections)+", c, c");
		        panel.add(new JLabel("Clicking APPLY will also disable faulty sections."), "0, "+(10+SectionEditor.numberOfSections)+", 4," +(10+SectionEditor.numberOfSections)+", c, c");
		        		        
		        JLabel warn1 = new JLabel("Warning: If 'Remove initial silence' is enabled or the");
		        JLabel warn2 = new JLabel("meter is modified, then the bar counter in lower-right might");
		        JLabel warn3 = new JLabel("not match up, unless your preview mode is in 'Original'.");
		        warn1.setForeground(new Color(1f,0f,0f));
		        warn2.setForeground(new Color(1f,0f,0f));
		        warn3.setForeground(new Color(1f,0f,0f));
		        panel.add(warn1, "0," +(11+SectionEditor.numberOfSections)+", 4," +(11+SectionEditor.numberOfSections)+", c, c");
		        panel.add(warn2, "0," +(12+SectionEditor.numberOfSections)+", 4," +(12+SectionEditor.numberOfSections)+", c, c");
		        panel.add(warn3, "0," +(13+SectionEditor.numberOfSections)+", 4," +(13+SectionEditor.numberOfSections)+", c, c");
		        
		        this.getContentPane().add(panel);
		        Window window = SwingUtilities.windowForComponent(this);
		        if (window != null) {
		        	// Lets keep the dialog inside the screen, in case the screen changed resolution since it was last popped up
			        int maxX = window.getBounds().width - w;
			        int maxY = window.getBounds().height - h;
			        int x = Math.max(0, Math.min(maxX, TuneEditor.lastLocation.x));
			        int y = Math.max(0, Math.min(maxY, TuneEditor.lastLocation.y));
			        this.setLocation(new Point(x,y));
		        } else {
		        	this.setLocation(TuneEditor.lastLocation);
		        }
		        this.setVisible(true);
		        //this.setResizable(true);
		        //System.err.println(Thread.currentThread().getName()); Swing event thread
		    }
		};
		
		new TuneDialog(jf, "Tune editor", true, abcSong);
	}
}