package dinoplugin.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class SourceView extends ViewPart {

	public static String lineParserPath = "/home/alin/line_parser";

	List<Color> colorArray = new ArrayList<Color>();

	AssemblyLine[] assemblyData;
	String[] sourceCodeData;

	ArrayList<Integer> assemblySelectedIndices = new ArrayList<>();

	class AssemblyLine {
		public String content;
		public int lineNumber;

		@Override
		public String toString() {
			return content + " " + lineNumber;
		}
	}

	public String getAssemblyJson(String binaryPath, String sourcePath) {

		String[] commands = new String[3];
		commands[0] = lineParserPath;
		commands[1] = binaryPath;
		commands[2] = sourcePath;

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(commands);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String line, source = "";

		BufferedReader in = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		try {
			while ((line = in.readLine()) != null) {
				source += line + "\n";
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return source;
	}

	public AssemblyLine[] getLines(String binaryPath, String sourcePath) {
		String source = getAssemblyJson(binaryPath, sourcePath);

		java.lang.reflect.Type lineArray = new TypeToken<AssemblyLine[]>() {
		}.getType();

		try {
			AssemblyLine[] parsedData = new Gson().fromJson(source, lineArray);
			return parsedData;
		} catch (JsonSyntaxException e) {
			return null;
		}
	}

	class AssemblyRenderer extends JLabel implements
			ListCellRenderer<AssemblyLine> {

		public AssemblyRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(
				JList<? extends AssemblyLine> list, AssemblyLine row,
				int index, boolean isSelected, boolean hasFocus) {

			Font f = this.getFont();
			f = f.deriveFont(Font.PLAIN);
			setFont(f);

			if (isSelected) {
				setBackground(Color.ORANGE);
				f = this.getFont();
				f = f.deriveFont(Font.BOLD);
				setFont(f);
			} else {
				setBackground(colorArray
						.get(row.lineNumber % colorArray.size()));
			}

			setText(row.content);

			return this;
		}
	}

	class SourceRenderer extends JLabel implements ListCellRenderer<String> {

		public SourceRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(
				JList<? extends String> list, String row, int index,
				boolean isSelected, boolean hasFocus) {

			Font f = this.getFont();
			f = f.deriveFont(Font.PLAIN);
			setFont(f);

			if (isSelected) {
				setBackground(Color.ORANGE);
				f = this.getFont();
				f = f.deriveFont(Font.BOLD);
				setFont(f);
			} else {
				index++;

				setBackground(Color.white);
				for (AssemblyLine l : assemblyData) {
					if (l.lineNumber == index) {
						setBackground(colorArray.get(index % colorArray.size()));
					}
				}
			}
			row = row.replaceAll("\t", "    ");
			if (row.length() == 0)
				row = " ";

			setText(row);

			return this;
		}
	}

	String executablePath;
	String sourceFilePath;

	private int[] getPrimitiveArray(ArrayList<Integer> arrayList) {
		int[] result = new int[arrayList.size()];
		int c = 0;
		for (int i : arrayList) {
			result[c++] = i;
		}
		return result;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.EMBEDDED
				| SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(composite);

		frame.setLayout(new GridBagLayout());
		frame.setTitle("Source Code View");
		frame.setLocationRelativeTo(null);

		String[] colors = { "c3e6e6", "d5c3e6", "e6c3c3", "d5e6c3", "fafab1",
				"e8e8dc", "f0d5c0", "c0f0c1", "facaca", "c9c9f5" };

		for (String s : colors) {
			colorArray.add(new Color(Integer.parseInt(s, 16)));
		}

		final JList<AssemblyLine> assemblyList = new JList<AssemblyLine>();
		final JList<String> codeList = new JList<String>();
		JScrollPane scrollPaneAssembly;
		JScrollPane scrollPaneCode;

		final JButton button1 = new JButton("Select executable file");
		final JButton button2 = new JButton("Select source file");

		button1.setEnabled(true);
		button2.setEnabled(false);

		final JLabel label1 = new JLabel();
		final JLabel label2 = new JLabel();

		button1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				int status = fileChooser.showOpenDialog(null);
				if (status == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					executablePath = selectedFile.getAbsolutePath();

					label1.setText(executablePath);

					assemblyList.clearSelection();
					codeList.clearSelection();

					// clear data for right label
					assemblyData = null;
					codeList.setListData(new String[0]);
					sourceFilePath = "";
					label2.setText(sourceFilePath);
					button2.setEnabled(true);
				}
			}
		});

		button2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				JFileChooser fileChooser = new JFileChooser();
				int status = fileChooser.showOpenDialog(null);
				if (status == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					sourceFilePath = selectedFile.getAbsolutePath();

					AssemblyLine[] retVal = getLines(executablePath,
							sourceFilePath);

					if (retVal == null) {
						String error = getAssemblyJson(executablePath,
								sourceFilePath);
						if (error.length() == 0)
							error = "Selected executable can not be parsed";
						JOptionPane.showMessageDialog(null, error);
					} else {
						assemblyData = retVal;

						assemblyList.setListData(assemblyData);
						assemblyList.setCellRenderer(new AssemblyRenderer());

						ArrayList<String> sourceCode = null;

						BufferedReader br;
						try {
							br = new BufferedReader(new FileReader(
									sourceFilePath));

							String line;

							sourceCode = new ArrayList<>();

							while ((line = br.readLine()) != null) {
								sourceCode.add(line);
							}

						} catch (Exception e) {
						}

						assemblyList.clearSelection();
						codeList.clearSelection();

						sourceCodeData = new String[sourceCode.size()];
						sourceCode.toArray(sourceCodeData);

						label2.setText(sourceFilePath);

						codeList.setListData(sourceCodeData);
						codeList.setCellRenderer(new SourceRenderer());
					}
				}
			}
		});

		assemblyList.setLayoutOrientation(JList.VERTICAL);
		codeList.setLayoutOrientation(JList.VERTICAL);
		codeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		codeList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent evt) {
				int sourceLine = codeList.getSelectedIndex() + 1;

				ArrayList<Integer> asmList = new ArrayList<Integer>();
				int c = 0;

				for (int i = 0; i < assemblyData.length; i++) {
					if (assemblyData[i].lineNumber == sourceLine) {
						asmList.add(i);
					}
				}

				assemblyList.setSelectedIndices(getPrimitiveArray(asmList));

				if (asmList.size() > 0) {
					int minIndex = assemblyList.getMinSelectionIndex();
					int maxIndex = assemblyList.getMaxSelectionIndex();

					if (minIndex > 0)
						minIndex--;
					if (maxIndex < assemblyData.length - 1)
						maxIndex++;

					assemblyList.scrollRectToVisible(assemblyList
							.getCellBounds(minIndex, maxIndex));
				}
			}
		});

		assemblyList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent evt) {
				if (assemblyList.getSelectedValue() != null
						&& assemblyData != null) {
					int selectedSourceLine = assemblyList.getSelectedValue().lineNumber;

					ArrayList<Integer> asmLinesList = new ArrayList<>();

					for (int i = 0; i < assemblyData.length; i++) {
						if (assemblyData[i].lineNumber == selectedSourceLine) {
							asmLinesList.add(i);
						}
					}

					codeList.setSelectedIndex(selectedSourceLine - 1);
					if (asmLinesList.size() > 0) {
						int minIndex = selectedSourceLine - 1;
						int maxIndex = selectedSourceLine - 1;

						if (minIndex > 0)
							minIndex--;
						if (maxIndex < sourceCodeData.length - 1)
							maxIndex++;

						codeList.scrollRectToVisible(codeList.getCellBounds(
								minIndex, maxIndex));
					}
				}
			}
		});

		scrollPaneAssembly = new JScrollPane(assemblyList);
		scrollPaneCode = new JScrollPane(codeList);
		scrollPaneAssembly
				.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.weightx = 1;
		c.gridy = 0;

		frame.add(button1, c);

		c.gridx = 1;

		frame.add(button2, c);

		c.gridy = 1;
		c.gridx = 0;
		frame.add(label1, c);

		c.gridx = 1;
		frame.add(label2, c);

		c.weighty = 1;
		c.gridy = 2;
		c.gridx = 0;

		frame.add(scrollPaneAssembly, c);
		c.gridx = 1;

		frame.add(scrollPaneCode, c);
	}

	@Override
	public void setFocus() {
	}

}
