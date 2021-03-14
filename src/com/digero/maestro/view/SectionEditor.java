package com.digero.maestro.view;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.PartSection;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class SectionEditor {

	public static void show(JFrame jf, NoteGraph noteGraph, AbcPart abcPart, int track) {
		@SuppressWarnings("serial")
		class SectionDialog extends JDialog {
			
			private final double[] LAYOUT_COLS = new double[] { 0.10,0.15,0.15,0.15,0.15,0.12,0.18 };
			private double[] LAYOUT_ROWS;
			private AbcPart abcPart;
			private int track;
			private int numberOfSections = 6;
			
			private List<SectionEditorLine> sectionInputs = new ArrayList<SectionEditorLine>(numberOfSections);
			     
	        JButton showVolume = new JButton("Show");
	        
	        //NoteGraph noteGraph = null;
		    
		    public SectionDialog(JFrame jf, final NoteGraph noteGraph, String title, boolean modal, AbcPart abcPart, int track) {
		        super(jf, title, modal);
		        this.abcPart = abcPart;
		        this.track = track;
		        //this.noteGraph = noteGraph;
		        
		        this.setSize(425,250+21*numberOfSections);
		        JPanel panel=new JPanel();
		        
		        LAYOUT_ROWS = new double[3+numberOfSections+10]; 
		        LAYOUT_ROWS[0] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[1] = 20;
		        LAYOUT_ROWS[2] = TableLayoutConstants.PREFERRED;
		        for (int l = 0;l<numberOfSections;l++) {
		        	LAYOUT_ROWS[3+l] = TableLayoutConstants.PREFERRED;		        	
		        }
		        LAYOUT_ROWS[3+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[4+numberOfSections] = TableLayoutConstants.FILL;
		        LAYOUT_ROWS[5+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[6+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[7+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[8+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[9+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[10+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[11+numberOfSections] = TableLayoutConstants.PREFERRED;
		        LAYOUT_ROWS[12+numberOfSections] = TableLayoutConstants.PREFERRED;
		        
		        panel.setLayout(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		        panel.add(new JLabel("<html><b> " + abcPart.getTitle() + ": </b> " + abcPart.getInstrument().toString()+" on track "+track + " </html>"), "0, 0, 6, 0, C, C");
		        panel.add(new JLabel("Enable"), "0, 2, c, c");
		        panel.add(new JLabel("From bar"), "1, 2, c, c");
		        panel.add(new JLabel("To bar"), "2, 2, c, c");
		        panel.add(new JLabel("Octave"), "3, 2, c, c");
		        panel.add(new JLabel("Volume"), "4, 2, c, c");
		        panel.add(new JLabel("Silence"), "5, 2, c, c");
		        panel.add(new JLabel("Fade %"), "6, 2, c, c");
		        
		        for (int j = 0;j<numberOfSections;j++) {
		        	sectionInputs.add(new SectionEditorLine());
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
			        	}
			        	number ++;
			        }
		        }
		        
		        // Tooltips
		        String enable = "<html><b> Enable a specific section edit. </b><br> Pressing APPLY will disable a section if it is bad.</html>";
		        String barA = "<html><b> The start bar (inclusive) where this section edit starts. </b><br> Must be above 0 and greater than the 'From bar' of previous enabled section.</html>";
		        String barB = "<html><b> The end bar (inclusive) where this section edit end. </b><br> Must be equal or greater than the 'From bar'.</html>";
		        String transpose = "<html><b> Transpose this section some octaves up or down. </b><br> Enter a positive or negative number. </html>";
		        String velo = "<html><b> Offset the volume of this section. </b><br> Experiment to find the number that does what you want. <br> Normally a number from -250 to 250. </html>";
		        String silent = "<html><b> Silence this section. </b></html>";
		        String fade = "<html><b> Fade in/out the volume of this section. </b><br> 0 = no fading <br> 100 = fade out full <br> -100 = fade in full <br> 150 = fade out before section ends <br> Etc. etc.. </html>";
		        
		        for (int i = 0;i<numberOfSections;i++) {
		        	sectionInputs.get(i).fade.setToolTipText(fade);
		        	sectionInputs.get(i).silent.setToolTipText(silent);
		        	sectionInputs.get(i).velo.setToolTipText(velo);
		        	sectionInputs.get(i).transpose.setToolTipText(transpose);
		        	sectionInputs.get(i).barB.setToolTipText(barB);
		        	sectionInputs.get(i).barA.setToolTipText(barA);
		        	sectionInputs.get(i).enable.setToolTipText(enable);
		        	
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
		        }
		        
		        showVolume.getModel().addChangeListener(new ChangeListener() {
	                @Override
	                public void stateChanged(ChangeEvent e) {
	                    ButtonModel model = showVolume.getModel();
	                    if (model.isArmed()) {
	                    	noteGraph.setShowingNoteVelocity(true);
	                    } else {
	                    	noteGraph.setShowingNoteVelocity(false);
	                    }
	                }
	            });
		        showVolume.setToolTipText("<html><b> Press and hold to see the note volumes on the track. </b><br> Only edits after clicking APPLY will show. </html>");
		        panel.add(showVolume, "4,"+(3+numberOfSections)+",f,f");
		        
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
										if (ps.startBar > lastEnd && ps.startBar <= ps.endBar && ps.startBar > 0) {
											tm.put(ps.startBar, ps);
											lastEnd = ps.endBar;
											ps.dialogLine = k;
										} else {
											SectionDialog.this.sectionInputs.get(k).enable.setSelected(false);
										}
								} catch (NumberFormatException nfe) {
									SectionDialog.this.sectionInputs.get(k).enable.setSelected(false);
								}
							}
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
					}
					
				});
		        okButton.setToolTipText("<html><b> Apply the effects. </b><br> Note that non-applied effects will not be remembered when closing dialog.<br> Sections that are not enabled will also not be remembered. </html>");
		        panel.add(okButton, "6,"+(3+numberOfSections)+",f,f");
		        panel.add(new JLabel("Enabled sections must be chronological and no overlap."), "0,"+(5+numberOfSections)+", 6," +(5+numberOfSections)+", c, c");
		        panel.add(new JLabel("Bar numbers are inclusive and use original meter."), "0, "+(6+numberOfSections)+", 6, "+(6+numberOfSections)+", c, c");
		        panel.add(new JLabel("No decimal numbers allowed, only whole numbers."), "0, "+(7+numberOfSections)+", 6," +(7+numberOfSections)+", c, c");
		        panel.add(new JLabel("Bar numbers must be positive and greater than zero."), "0, "+(8+numberOfSections)+", 6," +(8+numberOfSections)+", c, c");
		        panel.add(new JLabel("Clicking APPLY will also disable faulty sections."), "0, "+(9+numberOfSections)+", 6," +(9+numberOfSections)+", c, c");
		        JLabel warn1 = new JLabel("Warning: If you have 'Remove initial silence' enabled,");
		        JLabel warn2 = new JLabel("then the bar counter in lower right likely wont match up unless");
		        JLabel warn3 = new JLabel("you preview mode is in 'Original'.");
		        warn1.setForeground(new Color(1f,0f,0f));
		        warn2.setForeground(new Color(1f,0f,0f));
		        warn3.setForeground(new Color(1f,0f,0f));
		        panel.add(warn1, "0," +(10+numberOfSections)+", 6," +(10+numberOfSections)+", c, c");
		        panel.add(warn2, "0," +(11+numberOfSections)+", 6," +(11+numberOfSections)+", c, c");
		        panel.add(warn3, "0," +(12+numberOfSections)+", 6," +(12+numberOfSections)+", c, c");
		        this.getContentPane().add(panel);
		        this.setVisible(true);
		    }
		};
		
		new SectionDialog(jf, noteGraph, "Section editor", true, abcPart, track);
	}
	
}