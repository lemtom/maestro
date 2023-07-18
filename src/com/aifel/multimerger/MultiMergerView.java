package com.aifel.multimerger;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import javax.swing.JScrollPane;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import java.awt.Dimension;

public class MultiMergerView extends JFrame {

	private JPanel contentPane;
	private JScrollPane txtAreaScroll;
	private JScrollPane scrollPane;
	private JButton btnDest;
	private JButton btnSource;
	private JButton btnJoin;
	private JTextArea txtArea;
	private JPanel folderPanel;
	private JLabel lblSource;
	private JLabel lblDest;



	/**
	 * Create the frame.
	 */
	public MultiMergerView() {
		setTitle("ABC Merge Tool");
		setMinimumSize(new Dimension(800, 400));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(400, 200));
		scrollPane.setSize(new Dimension(300, 200));
		scrollPane.setMinimumSize(new Dimension(300, 200));
		contentPane.add(scrollPane, BorderLayout.WEST);
		
		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.NORTH);
		
		JLabel lblNewLabel = new JLabel("Convert single part abc files into multi part abc files");
		panel_1.add(lblNewLabel);
		
		JPanel south = new JPanel();
		south.setLayout(new BorderLayout(0, 0));
		contentPane.add(south, BorderLayout.SOUTH);
		
		JSplitPane splitPane = new JSplitPane();
		south.add(splitPane, BorderLayout.SOUTH);
		
		btnSource = new JButton("Select folder with single part files");
		splitPane.setLeftComponent(btnSource);
		
		btnDest = new JButton("Select multi part destination folder");
		btnDest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		splitPane.setRightComponent(btnDest);
		
		folderPanel = new JPanel();
		south.add(folderPanel, BorderLayout.NORTH);
		folderPanel.setLayout(new BorderLayout(0, 0));
		
		lblSource = new JLabel("Source:");
		folderPanel.add(lblSource, BorderLayout.NORTH);
		
		lblDest = new JLabel("Dest:");
		folderPanel.add(lblDest, BorderLayout.SOUTH);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.EAST);
		
		btnJoin = new JButton("Join & save");
		panel.add(btnJoin);
		
		txtAreaScroll = new JScrollPane();
		contentPane.add(txtAreaScroll, BorderLayout.CENTER);
		
		txtArea = new JTextArea();
		txtArea.setEditable(false);
		txtArea.setWrapStyleWord(true);
		txtArea.setText("Start by selecting BOTH folders.\r\nThen mark 2 or more abc part files.\r\nThen click Join.\r\nThen repeat for other songs.\r\n\r\nBEWARE: It will overwrite files in destination folder, so best to start with a empty destination folder.");
		txtArea.setLineWrap(true);
		txtArea.setColumns(10);
		txtAreaScroll.setViewportView(txtArea);
	}

	public JScrollPane getScrollPane() {
		return scrollPane;
	}
	public JButton getBtnDest() {
		return btnDest;
	}
	public JButton getBtnSource() {
		return btnSource;
	}
	public JButton getBtnJoin() {
		return btnJoin;
	}
	public String getTextFieldText() {
		return txtArea.getText();
	}
	public void setTextFieldText(String text) {
		txtArea.setText(text);
	}
	public String getLblSourceText() {
		return lblSource.getText();
	}
	public void setLblSourceText(String text_1) {
		lblSource.setText(text_1);
	}
	public String getLblDestText() {
		return lblDest.getText();
	}
	public void setLblDestText(String text_2) {
		lblDest.setText(text_2);
	}
	public boolean getBtnJoinEnabled() {
		return btnJoin.isEnabled();
	}
	public void setBtnJoinEnabled(boolean enabled) {
		btnJoin.setEnabled(enabled);
	}
}
