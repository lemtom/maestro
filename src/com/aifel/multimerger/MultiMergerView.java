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

public class MultiMergerView extends JFrame {

	private JPanel contentPane;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private JButton btnDest;
	private JButton btnSource;
	private JButton btnJoin;



	/**
	 * Create the frame.
	 */
	public MultiMergerView() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.WEST);
		
		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.NORTH);
		
		JLabel lblNewLabel = new JLabel("Convert single part abc files into multi part abc files");
		panel_1.add(lblNewLabel);
		
		JSplitPane splitPane = new JSplitPane();
		contentPane.add(splitPane, BorderLayout.SOUTH);
		
		btnSource = new JButton("Select folder with single part files");
		splitPane.setLeftComponent(btnSource);
		
		btnDest = new JButton("Select multi part destination folder");
		btnDest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		splitPane.setRightComponent(btnDest);
		
		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.EAST);
		
		btnJoin = new JButton("Join");
		panel.add(btnJoin);
		
		textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setText("Start by by selecting BOTH folders.\r\n\r\nThen mark 2 or more abc part files.\r\n\r\nThen click Join.\r\n\r\nThen repeat for other songs.\r\n\r\nBEWARE: It will overwrite files in destination folder, so best to start with a empty destination folder.");
		contentPane.add(textArea, BorderLayout.CENTER);
		textArea.setColumns(10);
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
		return textArea.getText();
	}
	public void setTextFieldText(String text) {
		textArea.setText(text);
	}
}
