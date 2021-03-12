package com.digero.maestro.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.abc.PartSection;

import info.clearthought.layout.TableLayout;
import info.clearthought.layout.TableLayoutConstants;

public class SectionEditor {

	public static void show(JFrame jf, NoteGraph noteGraph, AbcPart abcPart, int track) {
		@SuppressWarnings("serial")
		class SectionDialog extends JDialog {
			
			private final double[] LAYOUT_COLS = new double[] { 0.10,0.15,0.15,0.15,0.15,0.12,0.18 };
			private final double[] LAYOUT_ROWS = new double[] { TableLayoutConstants.PREFERRED,20,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.FILL,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED,TableLayoutConstants.PREFERRED};
			private AbcPart abcPart;
			private int track;
			private boolean active = false;
			
			JCheckBox enable0 = new JCheckBox();
	        JTextField barA0 = new JTextField("0");
	        JTextField barB0 = new JTextField("0");
	        JTextField transpose0 = new JTextField("0");
	        JTextField velo0 = new JTextField("0");
	        JCheckBox silent0 = new JCheckBox();
	        JCheckBox fadeout0 = new JCheckBox();
	        
	        JCheckBox enable1 = new JCheckBox();
	        JTextField barA1 = new JTextField("0");
	        JTextField barB1 = new JTextField("0");
	        JTextField transpose1 = new JTextField("0");
	        JTextField velo1 = new JTextField("0");
	        JCheckBox silent1 = new JCheckBox();
	        JCheckBox fadeout1 = new JCheckBox();
	        
	        JCheckBox enable2 = new JCheckBox();
	        JTextField barA2 = new JTextField("0");
	        JTextField barB2 = new JTextField("0");
	        JTextField transpose2 = new JTextField("0");
	        JTextField velo2 = new JTextField("0");
	        JCheckBox silent2 = new JCheckBox();
	        JCheckBox fadeout2 = new JCheckBox();
	        
	        JCheckBox enable3 = new JCheckBox();
	        JTextField barA3 = new JTextField("0");
	        JTextField barB3 = new JTextField("0");
	        JTextField transpose3 = new JTextField("0");
	        JTextField velo3 = new JTextField("0");
	        JCheckBox silent3 = new JCheckBox();
	        JCheckBox fadeout3 = new JCheckBox();
	        
	        JCheckBox enable4 = new JCheckBox();
	        JTextField barA4 = new JTextField("0");
	        JTextField barB4 = new JTextField("0");
	        JTextField transpose4 = new JTextField("0");
	        JTextField velo4 = new JTextField("0");
	        JCheckBox silent4 = new JCheckBox();
	        JCheckBox fadeout4 = new JCheckBox();
	        
	        JCheckBox enable5 = new JCheckBox();
	        JTextField barA5 = new JTextField("0");
	        JTextField barB5 = new JTextField("0");
	        JTextField transpose5 = new JTextField("0");
	        JTextField velo5 = new JTextField("0");
	        JCheckBox silent5 = new JCheckBox();
	        JCheckBox fadeout5 = new JCheckBox();
	        
	        NoteGraph noteGraph = null;
		    
		    public SectionDialog(JFrame jf, NoteGraph noteGraph, String title, boolean modal, AbcPart abcPart, int track) {
		        super(jf, title, modal);
		        this.abcPart = abcPart;
		        this.track = track;
		        this.noteGraph = noteGraph;
		        TreeMap<Integer, PartSection> tree = abcPart.sections.get(track);
		        if (tree != null) {
			        int number = 0;
			        for(Entry<Integer, PartSection> entry : tree.entrySet()) {
			        	PartSection ps = entry.getValue();
			        	if (number == 0) {
			        		enable0.setSelected(true);
			        		barA0.setText(""+ps.startBar);
			        		barB0.setText(""+ps.endBar);
			        		transpose0.setText(""+ps.octaveStep);
			        		velo0.setText(""+ps.volumeStep);
			        		silent0.setSelected(ps.silence);
			        		fadeout0.setSelected(ps.fadeout);
			        	} else if (number == 1) {
			        		enable1.setSelected(true);
			        		barA1.setText(""+ps.startBar);
			        		barB1.setText(""+ps.endBar);
			        		transpose1.setText(""+ps.octaveStep);
			        		velo1.setText(""+ps.volumeStep);
			        		silent1.setSelected(ps.silence);
			        		fadeout1.setSelected(ps.fadeout);
			        	} else if (number == 2) {
			        		enable2.setSelected(true);
			        		barA2.setText(""+ps.startBar);
			        		barB2.setText(""+ps.endBar);
			        		transpose2.setText(""+ps.octaveStep);
			        		velo2.setText(""+ps.volumeStep);
			        		silent2.setSelected(ps.silence);
			        		fadeout2.setSelected(ps.fadeout);
			        	} else if (number == 3) {
			        		enable3.setSelected(true);
			        		barA3.setText(""+ps.startBar);
			        		barB3.setText(""+ps.endBar);
			        		transpose3.setText(""+ps.octaveStep);
			        		velo3.setText(""+ps.volumeStep);
			        		silent3.setSelected(ps.silence);
			        		fadeout3.setSelected(ps.fadeout);
			        	} else if (number == 4) {
			        		enable4.setSelected(true);
			        		barA4.setText(""+ps.startBar);
			        		barB4.setText(""+ps.endBar);
			        		transpose4.setText(""+ps.octaveStep);
			        		velo4.setText(""+ps.volumeStep);
			        		silent4.setSelected(ps.silence);
			        		fadeout4.setSelected(ps.fadeout);
			        	} else if (number == 5) {
			        		enable5.setSelected(true);
			        		barA5.setText(""+ps.startBar);
			        		barB5.setText(""+ps.endBar);
			        		transpose5.setText(""+ps.octaveStep);
			        		velo5.setText(""+ps.volumeStep);
			        		silent5.setSelected(ps.silence);
			        		fadeout5.setSelected(ps.fadeout);
			        	} else {
			        		System.err.println("Too many sections in treemap in section-editor.");
			        	}
			        	number ++;
			        }
		        }
		        
		        this.setSize(425,350);
		        JPanel panel=new JPanel();
		        panel.setLayout(new TableLayout(LAYOUT_COLS, LAYOUT_ROWS));
		        panel.add(new JLabel(abcPart.getTitle()+": "+abcPart.getInstrument().toString()+" on track "+track), "0, 0, 6, 0, C, C");
		        panel.add(new JLabel("Enable"), "0, 2, c, c");
		        panel.add(new JLabel("From bar"), "1, 2, c, c");
		        panel.add(new JLabel("To bar"), "2, 2, c, c");
		        panel.add(new JLabel("Octave"), "3, 2, c, c");
		        panel.add(new JLabel("Volume"), "4, 2, c, c");
		        panel.add(new JLabel("Silence"), "5, 2, c, c");
		        panel.add(new JLabel("Fadeout"), "6, 2, c, c");
		        
		        panel.add(enable0, "0,3,C,C");
		        panel.add(barA0, "1,3,f,f");
		        panel.add(barB0, "2,3,f,f");
		        panel.add(transpose0, "3,3,f,f");
		        panel.add(velo0, "4,3,f,f");
		        panel.add(silent0, "5,3,c,f");
		        panel.add(fadeout0, "6,3,c,f");
		        
		        panel.add(enable1, "0,4,C,C");
		        panel.add(barA1, "1,4,f,f");
		        panel.add(barB1, "2,4,f,f");
		        panel.add(transpose1, "3,4,f,f");
		        panel.add(velo1, "4,4,f,f");
		        panel.add(silent1, "5,4,c,f");
		        panel.add(fadeout1, "6,4,c,f");
		        
		        panel.add(enable2, "0,5,C,C");
		        panel.add(barA2, "1,5,f,f");
		        panel.add(barB2, "2,5,f,f");
		        panel.add(transpose2, "3,5,f,f");
		        panel.add(velo2, "4,5,f,f");
		        panel.add(silent2, "5,5,c,f");
		        panel.add(fadeout2, "6,5,c,f");
		        
		        panel.add(enable3, "0,6,C,C");
		        panel.add(barA3, "1,6,f,f");
		        panel.add(barB3, "2,6,f,f");
		        panel.add(transpose3, "3,6,f,f");
		        panel.add(velo3, "4,6,f,f");
		        panel.add(silent3, "5,6,c,f");
		        panel.add(fadeout3, "6,6,c,f");
		        
		        panel.add(enable4, "0,7,C,C");
		        panel.add(barA4, "1,7,f,f");
		        panel.add(barB4, "2,7,f,f");
		        panel.add(transpose4, "3,7,f,f");
		        panel.add(velo4, "4,7,f,f");
		        panel.add(silent4, "5,7,c,f");
		        panel.add(fadeout4, "6,7,c,f");
		        
		        panel.add(enable5, "0,8,C,C");
		        panel.add(barA5, "1,8,f,f");
		        panel.add(barB5, "2,8,f,f");
		        panel.add(transpose5, "3,8,f,f");
		        panel.add(velo5, "4,8,f,f");
		        panel.add(silent5, "5,8,c,f");
		        panel.add(fadeout5, "6,8,c,f");
		        
		        JButton okButton = new JButton("APPLY");
		        //okButton.setPreferredSize(new Dimension(SECTIONBUTTON_WIDTH, SECTIONBUTTON_WIDTH));
		        //okButton.setMargin( new Insets(5, 5, 5, 5) );
		        okButton.addActionListener(new ActionListener() {
		        	
					@Override
					public void actionPerformed(ActionEvent e) {
						TreeMap<Integer, PartSection> tm = new TreeMap<Integer, PartSection>();
						int lastEnd = 0;
						if (SectionDialog.this.enable0.isSelected()) {
							PartSection ps = new PartSection();
							try {
									ps.octaveStep =  Integer.parseInt(transpose0.getText());
									ps.volumeStep =  Integer.parseInt(velo0.getText());
									ps.startBar = Integer.parseInt(barA0.getText());
									ps.endBar = Integer.parseInt(barB0.getText());
									ps.silence = silent0.isSelected();
									ps.fadeout = fadeout0.isSelected();
									if (ps.endBar > lastEnd && ps.startBar <= ps.endBar) {
										tm.put(ps.startBar, ps);
										lastEnd = ps.endBar; 
									} else {
										SectionDialog.this.enable0.setSelected(false);
									}
							} catch (NumberFormatException nfe) {
								//System.err.println("NumberFormatException in first section in section-editor.");
								SectionDialog.this.enable0.setSelected(false);
							}
						}
						if (SectionDialog.this.enable1.isSelected()) {
							PartSection ps = new PartSection();
							try {
								ps.octaveStep =  Integer.parseInt(transpose1.getText());
								ps.volumeStep =  Integer.parseInt(velo1.getText());
								ps.startBar = Integer.parseInt(barA1.getText());
								ps.endBar = Integer.parseInt(barB1.getText());
								ps.silence = silent1.isSelected();
								ps.fadeout = fadeout1.isSelected();
								if (ps.endBar > lastEnd && ps.startBar <= ps.endBar) {
									tm.put(ps.startBar, ps);
									lastEnd = ps.endBar;
								} else {
									SectionDialog.this.enable1.setSelected(false);
								}
							} catch (NumberFormatException nfe) {
								//System.err.println("NumberFormatException in second section in section-editor.");
								SectionDialog.this.enable1.setSelected(false);
							}
						}
						if (SectionDialog.this.enable2.isSelected()) {
							PartSection ps = new PartSection();
							try {
								ps.octaveStep =  Integer.parseInt(transpose2.getText());
								ps.volumeStep =  Integer.parseInt(velo2.getText());
								ps.startBar = Integer.parseInt(barA2.getText());
								ps.endBar = Integer.parseInt(barB2.getText());
								ps.silence = silent2.isSelected();
								ps.fadeout = fadeout2.isSelected();
								if (ps.endBar > lastEnd && ps.startBar <= ps.endBar) {
									tm.put(ps.startBar, ps);
									lastEnd = ps.endBar;
								} else {
									SectionDialog.this.enable2.setSelected(false);
								}
							} catch (NumberFormatException nfe) {
								//System.err.println("NumberFormatException in third section in section-editor.");
								SectionDialog.this.enable2.setSelected(false);
							}
						}
						if (SectionDialog.this.enable3.isSelected()) {
							PartSection ps = new PartSection();
							try {
								ps.octaveStep =  Integer.parseInt(transpose3.getText());
								ps.volumeStep =  Integer.parseInt(velo3.getText());
								ps.startBar = Integer.parseInt(barA3.getText());
								ps.endBar = Integer.parseInt(barB3.getText());
								ps.silence = silent3.isSelected();
								ps.fadeout = fadeout3.isSelected();
								if (ps.endBar > lastEnd && ps.startBar <= ps.endBar) {
									tm.put(ps.startBar, ps);
									lastEnd = ps.endBar;
								} else {
									SectionDialog.this.enable3.setSelected(false);
								}
							} catch (NumberFormatException nfe) {
								//System.err.println("NumberFormatException in fourth section in section-editor.");
								SectionDialog.this.enable3.setSelected(false);
							}
						}
						if (SectionDialog.this.enable4.isSelected()) {
							PartSection ps = new PartSection();
							try {
								ps.octaveStep =  Integer.parseInt(transpose4.getText());
								ps.volumeStep =  Integer.parseInt(velo4.getText());
								ps.startBar = Integer.parseInt(barA4.getText());
								ps.endBar = Integer.parseInt(barB4.getText());
								ps.silence = silent4.isSelected();
								ps.fadeout = fadeout4.isSelected();
								if (ps.endBar > lastEnd && ps.startBar <= ps.endBar) {
									tm.put(ps.startBar, ps);
									lastEnd = ps.endBar;
								} else {
									SectionDialog.this.enable4.setSelected(false);
								}
							} catch (NumberFormatException nfe) {
								//System.err.println("NumberFormatException in fifth section in section-editor.");
								SectionDialog.this.enable4.setSelected(false);
							}
						}
						if (SectionDialog.this.enable5.isSelected()) {
							PartSection ps = new PartSection();
							try {
								ps.octaveStep =  Integer.parseInt(transpose5.getText());
								ps.volumeStep =  Integer.parseInt(velo5.getText());
								ps.startBar = Integer.parseInt(barA5.getText());
								ps.endBar = Integer.parseInt(barB5.getText());
								ps.silence = silent5.isSelected();
								ps.fadeout = fadeout5.isSelected();
								if (ps.endBar > lastEnd && ps.startBar <= ps.endBar) {
									tm.put(ps.startBar, ps);
									lastEnd = ps.endBar;
								} else {
									SectionDialog.this.enable5.setSelected(false);
								}
							} catch (NumberFormatException nfe) {
								//System.err.println("NumberFormatException in sixth section in section-editor.");
								SectionDialog.this.enable5.setSelected(false);
							}
						}
						if (lastEnd == 0) {
							SectionDialog.this.abcPart.sections.set(SectionDialog.this.track, null);
							SectionDialog.this.abcPart.sectionsModified.set(SectionDialog.this.track, null);
						} else {
							SectionDialog.this.abcPart.sections.set(SectionDialog.this.track, tm);
							boolean[] booleanArray = new boolean[lastEnd+1];
							for(int i = 0; i<lastEnd+1;i++) {
								Entry<Integer, PartSection> entry = tm.floorEntry(i+1);
								booleanArray[i] = entry != null && entry.getValue().startBar <= i+1 && entry.getValue().endBar >= i+1;
							}
							
							SectionDialog.this.abcPart.sectionsModified.set(SectionDialog.this.track, booleanArray);
						}
						SectionDialog.this.abcPart.sectionEdited(SectionDialog.this.track);
						//SectionDialog.this.noteGraph.repaint();
					}
					
				});
		        panel.add(okButton, "6,9,f,f");
		        panel.add(new JLabel("Enabled sections must be chronological and no overlap."), "0, 11, 6, 11, c, c");
		        panel.add(new JLabel("Bar numbers are inclusive and use original meter."), "0, 12, 6, 12, c, c");
		        panel.add(new JLabel("No decimal numbers allowed, only whole numbers."), "0, 13, 6, 13, c, c");
		        panel.add(new JLabel("Bar numbers must be positive and greater than zero."), "0, 14, 6, 14, c, c");
		        panel.add(new JLabel("Clicking APPLY will also disable faulty sections."), "0, 15, 6, 15, c, c");
		        JLabel warn1 = new JLabel("Warning: If you have 'Remove initial silence' enabled,");
		        JLabel warn2 = new JLabel("then the bar counter in lower right wont match up unless");
		        JLabel warn3 = new JLabel("you preview mode is in 'Original'.");
		        warn1.setForeground(new Color(1f,0f,0f));
		        warn2.setForeground(new Color(1f,0f,0f));
		        warn3.setForeground(new Color(1f,0f,0f));
		        panel.add(warn1, "0, 16, 6, 16, c, c");
		        panel.add(warn2, "0, 17, 6, 17, c, c");
		        panel.add(warn3, "0, 18, 6, 18, c, c");
		        this.getContentPane().add(panel);
		        this.setVisible(true);
		    }
		};
		
		SectionDialog dia = new SectionDialog(jf, noteGraph, "Section editor", true, abcPart, track);
	}
	
}
