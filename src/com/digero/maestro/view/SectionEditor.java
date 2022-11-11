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

import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.PartSection;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class SectionEditor {
	
	protected static Point lastLocation = new Point(100,100);
	static final int numberOfSections = 10;
	static boolean clipboardArmed = false;
	static String[] clipboardStart = new String[numberOfSections];
	static String[] clipboardEnd = new String[numberOfSections];
	static boolean[] clipboardEnabled = new boolean[numberOfSections];

	public static void show(JFrame jf, NoteGraph noteGraph, AbcPart abcPart, int track, final boolean percussion, final ArrayList<DrumPanel> dPanels) {
		@SuppressWarnings("serial")
		class SectionDialog extends JDialog {
			
			private final double[] LAYOUT_COLS = new double[] { 0.068,0.102,0.102,0.102,0.102,0.0816,0.1224,0.08,0.08,0.08,0.08 };
			private double[] LAYOUT_ROWS;
			private AbcPart abcPart;
			private int track;
			
			
			private List<SectionEditorLine> sectionInputs = new ArrayList<SectionEditorLine>(numberOfSections);
			private SectionEditorLine nonSectionInput = new SectionEditorLine();
			     
	        JButton showVolume = new JButton("Show volume");
	        JButton copySections = new JButton("Copy");
	        JButton pasteSections = new JButton("Paste");
	        
	        //NoteGraph noteGraph = null;
		    
		    public SectionDialog(JFrame jf, final NoteGraph noteGraph, String title, boolean modal, AbcPart abcPart, int track) {
		        super(jf, title, modal);
		        this.abcPart = abcPart;
		        this.track = track;
		        //this.noteGraph = noteGraph;
		        
		        SectionDialog.this.addWindowListener(new WindowAdapter() {

		            @Override
		            public void windowClosing(WindowEvent we) {
		            	SectionEditor.lastLocation = SectionDialog.this.getLocation();
		            }
		        });
		        int w = 625;
		        int h = 271+21*numberOfSections;
		        this.setSize(w,h);
		        JPanel panel=new JPanel();
		        
		        LAYOUT_ROWS = new double[3+numberOfSections+1+10]; 
		        LAYOUT_ROWS[0] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[1] = 20;
		        LAYOUT_ROWS[2] = TableLayoutConstants.PREFERRED;
		        for (int l = 0;l<numberOfSections;l++) {
		        	LAYOUT_ROWS[3+l] = TableLayoutConstants.PREFERRED;		        	
		        }
		        LAYOUT_ROWS[3+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[4+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[5+numberOfSections] = TableLayoutConstants.FILL;
		        LAYOUT_ROWS[6+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[7+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[8+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[9+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[10+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[11+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[12+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[13+numberOfSections] = TableLayoutConstants.PREFERRED;
		        
		        panel.setLayout(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		        panel.add(new JLabel("<html><b> " + abcPart.getTitle() + ": </b> " + abcPart.getInstrument().toString()+" on track "+track + " </html>"), "0, 0, 6, 0, C, C");
		        panel.add(new JLabel("Enable"), "0, 2, c, c");
		        panel.add(new JLabel("From bar"), "1, 2, c, c");
		        panel.add(new JLabel("To bar"), "2, 2, c, c");
		        panel.add(new JLabel("Octave"), "3, 2, c, c");
		        panel.add(new JLabel("Volume"), "4, 2, c, c");
		        panel.add(new JLabel("Silence"), "5, 2, c, c");
		        panel.add(new JLabel("Fade %"), "6, 2, c, c");
		        JTextField octDouble = new JTextField("Octave doubling");
		        octDouble.setEditable(false);
		        octDouble.setHorizontalAlignment(JTextField.CENTER);
		        panel.add(octDouble, "7, 1, 10, 1, f, f");
		        //panel.add(new JLabel("Octave doubling"), "8, 1, 9, 1, c, c");
		        panel.add(new JLabel("2 down"), "7, 2, c, c");
		        panel.add(new JLabel("1 down"), "8, 2, c, c");
		        panel.add(new JLabel("1 up"), "9, 2, c, c");
		        panel.add(new JLabel("2 up"), "10, 2, c, c");
		        JTextField nonSection = new JTextField("Rest of the track");
		        nonSection.setEditable(false);
		        nonSection.setHorizontalAlignment(JTextField.CENTER);
		        panel.add(nonSection, "1, "+(3+numberOfSections)+", 2, "+(3+numberOfSections)+", f, f");
		        //panel.add(new JLabel("Rest of the track"), "1, "+(3+numberOfSections)+", 2, "+(3+numberOfSections)+", c, c");
		        
		        for (int j = 0;j<numberOfSections;j++) {
		        	SectionEditorLine l = new SectionEditorLine();
		        	l.transpose.setEnabled(!percussion);
		        	l.doubling0.setEnabled(!percussion);
		        	l.doubling1.setEnabled(!percussion);
		        	l.doubling2.setEnabled(!percussion);
		        	l.doubling3.setEnabled(!percussion);
		        	sectionInputs.add(l);
		        }
		        
		        TreeMap<Integer, PartSection> tree = abcPart.sections.get(track);
		        if (tree != null) {
			        int number = 0;
			        boolean useDialogLineNumbers = true;
			        for(Entry<Integer, PartSection> entry : tree.entrySet()) {
			        	PartSection ps = entry.getValue();
			        	if (ps.dialogLine == -1) {
			        		useDialogLineNumbers = false;
			        	}
			        	if (useDialogLineNumbers) {
			        		number = ps.dialogLine;
			        	}
		        		if (number >= numberOfSections || number < 0) {
		        			System.err.println("Too many sections in treemap in section-editor, or line numbers was badly edited in .msx file.");
		        		} else {
			        		sectionInputs.get(number).enable.setSelected(true);
			        		sectionInputs.get(number).barA.setText(""+ps.startBar);
			        		sectionInputs.get(number).barB.setText(""+ps.endBar);
			        		sectionInputs.get(number).transpose.setText(""+ps.octaveStep);
			        		sectionInputs.get(number).velo.setText(""+ps.volumeStep);
			        		sectionInputs.get(number).silent.setSelected(ps.silence);
			        		sectionInputs.get(number).fade.setText(""+ps.fade);
			        		sectionInputs.get(number).doubling0.setSelected(ps.doubling[0]);
			        		sectionInputs.get(number).doubling1.setSelected(ps.doubling[1]);
			        		sectionInputs.get(number).doubling2.setSelected(ps.doubling[2]);
			        		sectionInputs.get(number).doubling3.setSelected(ps.doubling[3]);
			        	}
			        	number ++;
			        }
		        }
		        
		        PartSection ps = abcPart.nonSection.get(track);
		        nonSectionInput.silent.setSelected(ps != null && ps.silence);
        		nonSectionInput.doubling0.setSelected(ps != null && ps.doubling[0]);
        		nonSectionInput.doubling1.setSelected(ps != null && ps.doubling[1]);
        		nonSectionInput.doubling2.setSelected(ps != null && ps.doubling[2]);
        		nonSectionInput.doubling3.setSelected(ps != null && ps.doubling[3]);
        		nonSectionInput.doubling0.setEnabled(!percussion);
        		nonSectionInput.doubling1.setEnabled(!percussion);
        		nonSectionInput.doubling2.setEnabled(!percussion);
        		nonSectionInput.doubling3.setEnabled(!percussion);
		        
		        // Tooltips
		        String enable = "<html><b> Enable a specific section edit. </b><br> Pressing APPLY will disable a section if it is bad.</html>";
		        String barA = "<html><b> The start bar (inclusive) where this section edit starts. </b><br> Must be above 0 and greater than the 'From bar' of previous enabled section.</html>";
		        String barB = "<html><b> The end bar (inclusive) where this section edit end. </b><br> Must be equal or greater than the 'From bar'.</html>";
		        String transpose = "<html><b> Transpose this section some octaves up or down. </b><br> Enter a positive or negative number. </html>";
		        String velo = "<html><b> Offset the volume of this section. </b><br> Experiment to find the number that does what you want. <br> Normally a number from -250 to 250. </html>";
		        String silent = "<html><b> Silence this section. </b></html>";
		        String fade = "<html><b> Fade in/out the volume of this section. </b><br> 0 = no fading <br> 100 = fade out full <br> -100 = fade in full <br> 150 = fade out before section ends <br> Etc. etc.. </html>";
		        String d0 = "<html><b> Double all notes in this section 2 octaves below.</b></html>";
		        String d1 = "<html><b> Double all notes in this section 1 octave below.</b></html>";
		        String d2 = "<html><b> Double all notes in this section 1 octave above.</b></html>";
		        String d3 = "<html><b> Double all notes in this section 2 octaves above.</b></html>";
		        
		        for (int i = 0;i<numberOfSections;i++) {
		        	sectionInputs.get(i).fade.setToolTipText(fade);
		        	sectionInputs.get(i).silent.setToolTipText(silent);
		        	sectionInputs.get(i).velo.setToolTipText(velo);
		        	sectionInputs.get(i).transpose.setToolTipText(transpose);
		        	sectionInputs.get(i).barB.setToolTipText(barB);
		        	sectionInputs.get(i).barA.setToolTipText(barA);
		        	sectionInputs.get(i).enable.setToolTipText(enable);
		        	sectionInputs.get(i).doubling0.setToolTipText(d0);
		        	sectionInputs.get(i).doubling1.setToolTipText(d1);
		        	sectionInputs.get(i).doubling2.setToolTipText(d2);
		        	sectionInputs.get(i).doubling3.setToolTipText(d3);
		        	
		        	sectionInputs.get(i).barA.setHorizontalAlignment(JTextField.CENTER);
		        	sectionInputs.get(i).barB.setHorizontalAlignment(JTextField.CENTER);
		        	sectionInputs.get(i).transpose.setHorizontalAlignment(JTextField.CENTER);
		        	sectionInputs.get(i).velo.setHorizontalAlignment(JTextField.CENTER);
		        	sectionInputs.get(i).fade.setHorizontalAlignment(JTextField.CENTER);
		        	
		        	panel.add(sectionInputs.get(i).enable, "0,"+(3+i)+",C,C");
			        panel.add(sectionInputs.get(i).barA, "1,"+(3+i)+",f,f");
			        panel.add(sectionInputs.get(i).barB, "2,"+(3+i)+",f,f");
			        panel.add(sectionInputs.get(i).transpose, "3,"+(3+i)+",f,f");
			        panel.add(sectionInputs.get(i).velo, "4,"+(3+i)+",f,f");
			        panel.add(sectionInputs.get(i).silent, "5,"+(3+i)+",c,f");
			        panel.add(sectionInputs.get(i).fade, "6,"+(3+i)+",f,f");
			        panel.add(sectionInputs.get(i).doubling0, "7,"+(3+i)+",c,f");
			        panel.add(sectionInputs.get(i).doubling1, "8,"+(3+i)+",c,f");
			        panel.add(sectionInputs.get(i).doubling2, "9,"+(3+i)+",c,f");
			        panel.add(sectionInputs.get(i).doubling3, "10,"+(3+i)+",c,f");
		        }
		        
		        nonSectionInput.silent.setToolTipText(silent);
		        nonSectionInput.doubling0.setToolTipText(d0);
	        	nonSectionInput.doubling1.setToolTipText(d1);
	        	nonSectionInput.doubling2.setToolTipText(d2);
	        	nonSectionInput.doubling3.setToolTipText(d3);
	        	panel.add(nonSectionInput.silent, "5,"+(3+numberOfSections)+",c,f");
		        panel.add(nonSectionInput.doubling0, "7,"+(3+numberOfSections)+",c,f");
		        panel.add(nonSectionInput.doubling1, "8,"+(3+numberOfSections)+",c,f");
		        panel.add(nonSectionInput.doubling2, "9,"+(3+numberOfSections)+",c,f");
		        panel.add(nonSectionInput.doubling3, "10,"+(3+numberOfSections)+",c,f");
		        
		        copySections.getModel().addActionListener(new ActionListener() {
	                @Override
	                public void actionPerformed(ActionEvent e) {
	                	for (int i = 0; i < numberOfSections; i++) {
	                		clipboardStart[i] = sectionInputs.get(i).barA.getText();
	                		clipboardEnd[i] = sectionInputs.get(i).barB.getText();
	                		clipboardEnabled[i] = sectionInputs.get(i).enable.isSelected();
	                	}
	                	clipboardArmed = true;
	                	pasteSections.setEnabled(clipboardArmed);
	                }
	            });
		        copySections.setToolTipText("<html><b> Copy the section starts and ends.</html>");
		        panel.add(copySections, "1,"+(4+numberOfSections)+",1,"+(4+numberOfSections)+",f,f");
		        
		        pasteSections.getModel().addActionListener(new ActionListener() {
	                @Override
	                public void actionPerformed(ActionEvent e) {
	                	if (!clipboardArmed) return; 
	                	for (int i = 0; i < numberOfSections; i++) {
	                		sectionInputs.get(i).barA.setText(clipboardStart[i]);
	                		sectionInputs.get(i).barB.setText(clipboardEnd[i]);
	                		sectionInputs.get(i).enable.setSelected(clipboardEnabled[i]);
	                	}
	                }
	            });
		        pasteSections.setToolTipText("<html><b> Paste the section starts and ends.</html>");
		        panel.add(pasteSections, "2,"+(4+numberOfSections)+",2,"+(4+numberOfSections)+",f,f");
		        pasteSections.setEnabled(clipboardArmed);
		        
		        showVolume.getModel().addChangeListener(new ChangeListener() {
	                @Override
	                public void stateChanged(ChangeEvent e) {
	                    ButtonModel model = showVolume.getModel();
	                    if (model.isArmed()) {
	                    	noteGraph.setShowingNoteVelocity(true);
	                    	if (dPanels != null) {
	                    		for (DrumPanel drum : dPanels) {
	                    			drum.updateVolume(true);
	                    		}
	                    	}
	                    } else {
	                    	noteGraph.setShowingNoteVelocity(false);
	                    	if (dPanels != null) {
	                    		for (DrumPanel drum : dPanels) {
	                    			drum.updateVolume(false);
	                    		}
	                    	}
	                    }
	                }
	            });
		        showVolume.setToolTipText("<html><b> Press and hold to see the note volumes on the track. </b><br> Only edits after clicking APPLY will show. </html>");
		        panel.add(showVolume, "4,"+(4+numberOfSections)+",5,"+(4+numberOfSections)+",f,f");
		        
		        JButton okButton = new JButton("APPLY");
		        okButton.addActionListener(new ActionListener() {
		        	
					@Override
					public void actionPerformed(ActionEvent e) {
						TreeMap<Integer, PartSection> tm = new TreeMap<Integer, PartSection>();
						
						int lastEnd = 0;
						for (int k = 0;k<numberOfSections;k++) {
							if (SectionDialog.this.sectionInputs.get(k).enable.isSelected()) {
								PartSection ps = new PartSection();
								try {
										ps.octaveStep =  Integer.parseInt(sectionInputs.get(k).transpose.getText());
										ps.volumeStep =  Integer.parseInt(sectionInputs.get(k).velo.getText());
										ps.startBar = Integer.parseInt(sectionInputs.get(k).barA.getText());
										ps.endBar = Integer.parseInt(sectionInputs.get(k).barB.getText());
										ps.silence = sectionInputs.get(k).silent.isSelected();
										ps.fade = Integer.parseInt(sectionInputs.get(k).fade.getText());
										ps.doubling[0] = sectionInputs.get(k).doubling0.isSelected();
										ps.doubling[1] = sectionInputs.get(k).doubling1.isSelected();
										ps.doubling[2] = sectionInputs.get(k).doubling2.isSelected();
										ps.doubling[3] = sectionInputs.get(k).doubling3.isSelected();
										boolean soFarSoGood = true;
										for (PartSection psC : tm.values()) {
											if (!(ps.startBar > psC.endBar || ps.endBar < psC.startBar)) {
												soFarSoGood = false;
											}
										}
										if (ps.startBar > 0 && ps.startBar <= ps.endBar && soFarSoGood) {
											tm.put(ps.startBar, ps);
											if (ps.endBar > lastEnd) lastEnd = ps.endBar;
											ps.dialogLine = k;
										} else {
											SectionDialog.this.sectionInputs.get(k).enable.setSelected(false);
										}
								} catch (NumberFormatException nfe) {
									SectionDialog.this.sectionInputs.get(k).enable.setSelected(false);
								}
							}
						}
						PartSection ps = new PartSection();
						try {
								ps.silence = nonSectionInput.silent.isSelected();
								ps.doubling[0] = nonSectionInput.doubling0.isSelected();
								ps.doubling[1] = nonSectionInput.doubling1.isSelected();
								ps.doubling[2] = nonSectionInput.doubling2.isSelected();
								ps.doubling[3] = nonSectionInput.doubling3.isSelected();
								if (ps.silence || ps.doubling[0] || ps.doubling[1] || ps.doubling[2] || ps.doubling[3]) {
									SectionDialog.this.abcPart.nonSection.set(SectionDialog.this.track, ps);
								} else {
									SectionDialog.this.abcPart.nonSection.set(SectionDialog.this.track, null);
								}
						} catch (NumberFormatException nfe) {
						}
						
						if (lastEnd == 0) {
							SectionDialog.this.abcPart.sections.set(SectionDialog.this.track, null);
							SectionDialog.this.abcPart.sectionsModified.set(SectionDialog.this.track, null);
						} else {
							SectionDialog.this.abcPart.sections.set(SectionDialog.this.track, tm);
							boolean[] booleanArray = new boolean[lastEnd+1];
							for(int m = 0; m<lastEnd+1;m++) {
								Entry<Integer, PartSection> entry = tm.floorEntry(m+1);
								booleanArray[m] = entry != null && entry.getValue().startBar <= m+1 && entry.getValue().endBar >= m+1;
							}
							
							SectionDialog.this.abcPart.sectionsModified.set(SectionDialog.this.track, booleanArray);
						}
						SectionDialog.this.abcPart.sectionEdited(SectionDialog.this.track);
						//SectionDialog.this.noteGraph.repaint();
						//System.err.println(Thread.currentThread().getName());
					}
				});
		        okButton.setToolTipText("<html><b> Apply the effects. </b><br> Note that non-applied effects will not be remembered when closing dialog.<br> Sections that are not enabled will likewise also not be remembered. </html>");
		        panel.add(okButton, "9,"+(4+numberOfSections)+", 10, "+(4+numberOfSections)+",f,f");
		        panel.add(new JLabel("Enabled sections must have no overlap."), "0,"+(6+numberOfSections)+", 6," +(6+numberOfSections)+", c, c");
		        panel.add(new JLabel("Bar numbers are inclusive and use original MIDI bars."), "0, "+(7+numberOfSections)+", 6, "+(7+numberOfSections)+", c, c");
		        panel.add(new JLabel("No decimal numbers allowed, only whole numbers."), "0, "+(8+numberOfSections)+", 6," +(8+numberOfSections)+", c, c");
		        panel.add(new JLabel("Bar numbers must be positive and greater than zero."), "0, "+(9+numberOfSections)+", 6," +(9+numberOfSections)+", c, c");
		        panel.add(new JLabel("Clicking APPLY will also disable faulty sections."), "0, "+(10+numberOfSections)+", 6," +(10+numberOfSections)+", c, c");
		        		        
		        JLabel warn1 = new JLabel("Warning: If 'Remove initial silence' is enabled or the");
		        JLabel warn2 = new JLabel("meter is modified, then the bar counter in lower-right might");
		        JLabel warn3 = new JLabel("not match up, unless your preview mode is in 'Original'.");
		        warn1.setForeground(new Color(1f,0f,0f));
		        warn2.setForeground(new Color(1f,0f,0f));
		        warn3.setForeground(new Color(1f,0f,0f));
		        panel.add(warn1, "0," +(11+numberOfSections)+", 6," +(11+numberOfSections)+", c, c");
		        panel.add(warn2, "0," +(12+numberOfSections)+", 6," +(12+numberOfSections)+", c, c");
		        panel.add(warn3, "0," +(13+numberOfSections)+", 6," +(13+numberOfSections)+", c, c");
		        
		        panel.add(new JLabel("Doubling works by copying all"), "7,"+(6+numberOfSections)+", 10," +(6+numberOfSections)+", c, c");
		        panel.add(new JLabel("notes and pasting them 1 or 2"), "7, "+(7+numberOfSections)+", 10, "+(7+numberOfSections)+", c, c");
		        panel.add(new JLabel("octaves from their original pitch."), "7, "+(8+numberOfSections)+", 10," +(8+numberOfSections)+", c, c");
		        
		        panel.add(new JLabel("The last line under the sections is all"), "7, "+(10+numberOfSections)+", 10," +(10+numberOfSections)+", c, c");
		        panel.add(new JLabel("notes that is not covered by sections."), "7, "+(11+numberOfSections)+", 10," +(11+numberOfSections)+", c, c");
		        
		        this.getContentPane().add(panel);
		        Window window = SwingUtilities.windowForComponent(this);
		        if (window != null) {
		        	// Lets keep the dialog inside the screen, in case the screen changed resolution since it was last popped up
			        int maxX = window.getBounds().width - w;
			        int maxY = window.getBounds().height - h;
			        int x = Math.max(0, Math.min(maxX, SectionEditor.lastLocation.x));
			        int y = Math.max(0, Math.min(maxY, SectionEditor.lastLocation.y));
			        this.setLocation(new Point(x,y));
		        } else {
		        	this.setLocation(SectionEditor.lastLocation);
		        }
		        this.setVisible(true);
		        //System.err.println(Thread.currentThread().getName()); Swing event thread
		    }
		};
		
		new SectionDialog(jf, noteGraph, "Section editor", true, abcPart, track);
	}
	
	public static void clearClipboard() {
		clipboardArmed = false;
	}	
}