package com.aifel.multimerger;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileSystemView;

import javax.swing.border.EmptyBorder;
import javax.swing.border.Border;

import com.digero.common.abc.LotroInstrument;

public class MultiMerger {
	
	public File sourceFolder = new File(System.getProperty("user.home"));
	public File destFolder = new File(System.getProperty("user.home"));
	public ActionListener actionSource = getSourceActionListener();
	public ActionListener actionDest = getDestActionListener();
	public ActionListener actionJoin = getJoinActionListener();
		
	private static final Pattern INFO_PATTERN = Pattern.compile("^([A-Z]):\\s*(.*)\\s*$");
	private static final int INFO_TYPE = 1;
	private static final int INFO_VALUE = 2;
	
	private static MultiMergerView frame = null;
	private static MultiMerger logic = null;
	
	JList<File> theList = null; 

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new MultiMergerView();
					logic = new MultiMerger();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	MultiMerger() {
		frame.getBtnDest().addActionListener(actionDest);
		frame.getBtnSource().addActionListener(actionSource);
		frame.getBtnJoin().addActionListener(actionJoin);
		frame.getScrollPane().getVerticalScrollBar().setUnitIncrement(22);
        refresh();
	}
	
	void refresh () {
        Component c = getGui(sourceFolder.listFiles(new AbcFileFilter()),false);
        frame.setLblSourceText("Source: "+sourceFolder.getAbsolutePath());
        frame.setLblDestText("Destination: "+destFolder.getAbsolutePath());
        frame.getScrollPane().setViewportView(c);
        frame.setBtnJoinEnabled(c != null);
        //frame.pack();
        frame.repaint();
	}
	
	private void join() throws IOException {
		List<File> theFiles = theList.getSelectedValuesList();
		if (theFiles != null && theFiles.size() > 1) {
			List<List<String>> oldContent = new ArrayList();
			for (File theFile : theFiles) {
				List<String> lines = Files.readAllLines(Paths.get(theFile.toURI()), StandardCharsets.UTF_8);
				oldContent.add(lines);
			}
			List<String> newContent = new ArrayList();
			int x = 1;
			int fileNo = 0;
			String Q = "";
			boolean mismatch = false;
			for (List<String> lines : oldContent) {
				boolean meta = true;
				LotroInstrument instr = null;
				for (String line : lines) {
					Matcher infoMatcher = INFO_PATTERN.matcher(line);
					boolean isX = false;
					if (infoMatcher.matches()) {
						char type = Character.toUpperCase(infoMatcher.group(INFO_TYPE).charAt(0));
						String value = infoMatcher.group(INFO_VALUE).trim();
						
						if (type == 'X') {
							meta = false;
							newContent.add("X: "+x);
							newContent.add("%%Orig filename was "+theFiles.get(fileNo).getName());
							x++;
							isX = true;
						} else if (type == 'T') {
							instr = LotroInstrument.findInstrumentName(value, null);
							if (instr == null) {
								// instr was not found in the part name, lets check the filename
								instr = LotroInstrument.findInstrumentName(theFiles.get(fileNo).getName(), null);
								if (instr != null) {
									line += "["+instr+"]";
								} else if (instr == null) {
									instr = LotroInstrument.findInstrumentNameAggressively(theFiles.get(fileNo).getName(), null);
									if (instr != null) line += "[["+instr+"]]";// Double [[ means there is significant chance it got the instrument wrong
								}
							}
						} else if (type == 'Q') {
							if (!"".equals(Q) && !Q.equals(value)) {
								mismatch = true;
							}
							Q = value;
						}
					}
					if (!isX && (!meta || x == 1)) {
						// X is not written here, and only header meta info from first file.
						newContent.add(line);
					}
				}
				fileNo++;
			}
			
			if (mismatch) {
				int misresult = JOptionPane.showConfirmDialog(frame, "All these files do not seem to belong to same song. Continue with merge?", "Tempo mismatch", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				
				switch (misresult) {
					case JOptionPane.YES_OPTION:
						break;
					case JOptionPane.NO_OPTION:
						frame.setTextFieldText("Cancelled merge.");
						return;
					case JOptionPane.CANCEL_OPTION:
						frame.setTextFieldText("Cancelled merge.");
						return;
				}
			}
			
			String n1 = theFiles.get(0).getName();
			int dot = n1.lastIndexOf('.');
			if (dot > 0)
				n1 = n1.substring(0, dot);
			
			String n2 = theFiles.get(theFiles.size()-1).getName();
			dot = n2.lastIndexOf('.');
			if (dot > 0)
				n2 = n2.substring(0, dot);
			
			String newName = getLongestCommonSubstring(n1, n2);
			if (newName.length() == 0) newName = "mySong";
			newName += ".abc";
			File newFile = new File(destFolder, newName);
			if (newFile.exists()) {
				int result = JOptionPane.showConfirmDialog(frame, "The file "+newFile.getAbsolutePath()+" exist already. Do you want to overwrite it?", "Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				
				switch (result) {
					case JOptionPane.YES_OPTION:
						break;
					case JOptionPane.NO_OPTION:
						frame.setTextFieldText("Cancelled save.");
						return;
					case JOptionPane.CANCEL_OPTION:
						frame.setTextFieldText("Cancelled save.");
						return;
				}				
			}
			newFile.createNewFile();
			FileWriter writer = new FileWriter(newFile); 
			
			frame.setTextFieldText("Writing new file:\n "+newFile.getAbsolutePath()+"\n\n The song has "+(x-1)+" parts.");
			String info = "Writing new file:\n "+newFile.getAbsolutePath()+"\n\n The song has "+(x-1)+" parts.\n\n";
			for (String line : newContent) {
				writer.write(line + System.lineSeparator());
				info += System.lineSeparator() + line;
			}
			writer.close();
			frame.setTextFieldText(info);
		} else {
			frame.setTextFieldText("Please select at least 2 abc files..");
		}
	}
	
	public JScrollPane getGui(File[] all, boolean vertical) {
		if (all.length == 0) return null;
        theList = new JList<File>(all);
        theList.setCellRenderer(new FileRenderer(!vertical));

        if (!vertical) {
        	theList.setLayoutOrientation(javax.swing.JList.HORIZONTAL_WRAP);
        	theList.setVisibleRowCount(-1);
        } else {
        	theList.setVisibleRowCount(9);
        }
        return new JScrollPane(theList);
    }
	
	class AbcFileFilter implements FileFilter {

		public boolean accept(File file) {
	        String name = file.getName().toLowerCase();
	        return name.endsWith(".abc") || name.endsWith(".txt");
	    }
	}
	
	private String getLongestCommonSubstring(String str1, String str2){
		int m = str1.length();
		int n = str2.length();

		int max = 0;

		int[][] dp = new int[m][n];
		int endIndex=-1;
		for(int i=0; i<m; i++){
			for(int j=0; j<n; j++){
				if(str1.charAt(i) == str2.charAt(j)){

					// If first row or column
					if(i==0 || j==0){
						dp[i][j]=1;
					}else{
						// Add 1 to the diagonal value
						dp[i][j] = dp[i-1][j-1]+1;
					}

					if(max < dp[i][j])
					{
						max = dp[i][j];
						endIndex=i;
					}
				}

			}
		}
		// We want String upto endIndex, we are using endIndex+1 in substring.
		return str1.substring(endIndex-max+1,endIndex+1);
	}

	class FileRenderer extends DefaultListCellRenderer {

	    private boolean pad;
	    private Border padBorder = new EmptyBorder(3,3,3,3);

	    FileRenderer(boolean pad) {
	        this.pad = pad;
	    }

	    @Override
	    public Component getListCellRendererComponent(
	        JList list,
	        Object value,
	        int index,
	        boolean isSelected,
	        boolean cellHasFocus) {

	        Component c = super.getListCellRendererComponent(
	            list,value,index,isSelected,cellHasFocus);
	        JLabel l = (JLabel)c;
	        File f = (File)value;
	        l.setText(f.getName());
	        l.setIcon(FileSystemView.getFileSystemView().getSystemIcon(f));
	        if (pad) {
	            l.setBorder(padBorder);
	        }

	        return l;
	    }
	}
	
	public ActionListener getSourceActionListener () {
		return new ActionListener()
		{
			JFileChooser openFileChooser;

			@Override public void actionPerformed(ActionEvent e)
			{
				if (openFileChooser == null)
				{
					openFileChooser = new JFileChooser(sourceFolder);
					openFileChooser.setMultiSelectionEnabled(false);
					//openFileChooser.setFileFilter(new ExtensionFileFilter("ABC files", "abc", "txt"));
					openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				}

				int result = openFileChooser.showOpenDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					sourceFolder = openFileChooser.getSelectedFile();
					refresh();
				}
			}
		};
	}
	
	public ActionListener getDestActionListener () {
		return new ActionListener()
		{
			JFileChooser openFileChooser;

			@Override public void actionPerformed(ActionEvent e)
			{
				if (openFileChooser == null)
				{
					openFileChooser = new JFileChooser(destFolder);
					openFileChooser.setMultiSelectionEnabled(false);
					openFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				}

				int result = openFileChooser.showOpenDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					destFolder = openFileChooser.getSelectedFile();
					refresh();
				}
			}
		};
	}
	
	private ActionListener getJoinActionListener() {
		return new ActionListener()
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				try {
					join();
				} catch (IOException e1) {
					e1.printStackTrace();
					frame.setTextFieldText("An error occured:\n\n"+e1.toString());
				}
			}
		};
	}
}
