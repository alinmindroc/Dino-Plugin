package dinoplugin.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import difflib.DiffRow;
import difflib.DiffRowGenerator;
import dino_plugin.Activator;
import dinoplugin.handlers.PreferencesHandler;

public class DiffView extends ViewPart {
	public static String functionCacheDir = "/tmp/dino/cached-functions/";
	public static String assemblyCacheDir = "/tmp/dino/cached-assembly/";
	
	public String parserDirPath = null;
	public String assemblyParserPath = null;
	public String functionParserPath = null;

	private void setBinaryDirPathFromPrefs(){
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		parserDirPath = store.getString(PreferencesHandler.PREFS_DIR_KEY);
		
		assemblyParserPath = parserDirPath + "/assembly_parser";
		functionParserPath = parserDirPath + "/function_parser";
	}
	
	/**
	 * check if the file received as parameter was already parsed and its
	 * functions are cached
	 * 
	 * @param fileName
	 * @return
	 */
	public static Boolean isFunctionCached(String fileName) {
		File cacheDir = new File(functionCacheDir);
		String[] cachedBinaries = cacheDir.list();

		if (!cacheDir.exists())
			cacheDir.mkdirs();

		if (cachedBinaries == null)
			return false;

		for (String s : cachedBinaries) {
			if (s.compareTo(fileName) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check if the file received as parameter was already parsed and its
	 * assembly is cached
	 * 
	 * @param fileName
	 * @return
	 */
	public static Boolean isAssemblyCached(String fileName) {
		File cacheDir = new File(assemblyCacheDir);
		String[] cachedBinaries = cacheDir.list();

		if (!cacheDir.exists())
			cacheDir.mkdirs();

		if (cachedBinaries == null)
			return false;

		for (String s : cachedBinaries) {
			if (s.compareTo(fileName) == 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Parse an executable file and save the assembly code in a file in the
	 * assembly cache path
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public String getAssemblyParsedData(File file) throws IOException {
		checkAndCreateCacheDirs();
		setBinaryDirPathFromPrefs();

		if (isAssemblyCached(file.getName()) == false) {
			String[] commands = new String[3];
			commands[0] = assemblyParserPath;
			commands[1] = file.getAbsolutePath();
			commands[2] = assemblyCacheDir + file.getName();

			Process p = null;
			try {
				p = Runtime.getRuntime().exec(commands);
				p.waitFor();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"Could not invoke parser at " + assemblyParserPath + ". Make sure you have set the executable directory path corectly");
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return FileUtils.readFileToString(new File(assemblyCacheDir
				+ file.getName()));
	}

	/**
	 * parse an executable file and get the list of functions in raw json format
	 * 
	 * @param file
	 * @return
	 */
	public static String getFunctionsRawJson(File file) {
		try {
			return FileUtils.readFileToString(new File(functionCacheDir
					+ file.getName()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * parse an executable file and get the list of function names in String
	 * array format
	 * 
	 * @param file
	 * @return
	 */
	public String[] getFunctionsParsedData(File file) throws IOException {
		checkAndCreateCacheDirs();
		setBinaryDirPathFromPrefs();
		
		// if the functions are not cached, parse them and save them to cache
		if (isFunctionCached(file.getName()) == false) {
			
			String[] commands = new String[3];
			commands[0] = functionParserPath;
			commands[1] = file.getAbsolutePath();
			commands[2] = functionCacheDir + file.getName();

			Process p;
			try {
				p = Runtime.getRuntime().exec(commands);
				p.waitFor();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null,
						"Could not invoke parser at " + functionParserPath + ". Make sure you have set the executable directory path corectly");
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		String source = FileUtils.readFileToString(new File(functionCacheDir
				+ file.getName()));

		Gson gson = new Gson();
		java.lang.reflect.Type stringStringMap = new TypeToken<List<FuncModel>>() {
		}.getType();

		List<FuncModel> funcList = null;

		try {
			funcList = gson.fromJson(source, stringStringMap);
		} catch (JsonSyntaxException e) {
			return null;
		}

		ArrayList<String> res = new ArrayList<>();
		for (FuncModel f : funcList) {
			res.add(f.name);
		}

		String[] resArr = new String[res.size()];
		res.toArray(resArr);
		return resArr;
	}

	private class FuncModel {
		public String address;
		public String name;
		public long size;

		public String toString() {
			return address + ":" + name + ", size:" + size + "\n";
		}
	}

	private class AssemblyLineModel {
		public String address;
		public String name;

		public String toString() {
			return address + ": " + name;
		}
	}

	/**
	 * 
	 * Represents a diff row in containing the indexes of the row in the left
	 * and right panels
	 * 
	 */
	private class IndexedDiffRow extends DiffRow {
		public IndexedDiffRow(Tag tag, String oldLine, String newLine) {
			super(tag, oldLine, newLine);
		}

		public IndexedDiffRow(Tag tag, String oldLine, String newLine,
				int index1, int index2) {
			super(tag, oldLine, newLine);
			this.index1 = index1;
			this.index2 = index2;
		}

		public int index1, index2;
	}

	private File selectedFile1 = null;
	private File selectedFile2 = null;

	private List<AssemblyLineModel> originalAssembly = null;
	private List<AssemblyLineModel> newAssembly = null;

	private List<Map<String, String>> assemblyList1 = null;
	private List<Map<String, String>> assemblyList2 = null;

	private Color green = new Color(149, 234, 149);
	private Color yellow = new Color(250, 217, 135);
	private Color red = new Color(239, 153, 153);
	private Color grey = new Color(220, 220, 219);

	private class DiffRendererLeft extends JLabel implements
			ListCellRenderer<IndexedDiffRow> {

		public DiffRendererLeft() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(
				JList<? extends IndexedDiffRow> list, IndexedDiffRow row,
				int index, boolean isSelected, boolean hasFocus) {

			String diffTag = row.getTag().name();
			String labelText = "";
			labelText = String.format("%4d ", row.index1);

			if (assemblyList1 == null) {
				// if only the left function is selected
				labelText += row.getOldLine();
				setBackground(grey);
			} else if (row.getOldLine().length() == 0) {
				labelText = " ";
				setBackground(grey);
			} else if (diffTag.compareTo("CHANGE") == 0) {
				setBackground(yellow);
				labelText += row.getOldLine();
				if (row.getOldLine().length() == 0)
					labelText = " ";
			} else if (diffTag.compareTo("INSERT") == 0) {
				setBackground(green);
				labelText = " ";
			} else if (diffTag.compareTo("DELETE") == 0) {
				setBackground(red);
				labelText += row.getOldLine();
			} else {
				labelText += row.getOldLine();
				setBackground(Color.white);
			}

			setText(labelText);

			return this;
		}
	}

	private class DiffRendererRight extends JLabel implements
			ListCellRenderer<IndexedDiffRow> {

		public DiffRendererRight() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(
				JList<? extends IndexedDiffRow> list, IndexedDiffRow row,
				int index, boolean isSelected, boolean hasFocus) {

			String diffTag = row.getTag().name();
			String labelText = "";
			labelText = String.format("%4d ", row.index2);

			if (row.getNewLine().length() == 0) {
				labelText = " ";
				setBackground(grey);
			} else if (diffTag.compareTo("CHANGE") == 0) {
				setBackground(yellow);
				labelText += row.getNewLine();
				if (row.getNewLine().length() == 0)
					labelText = " ";
			} else if (diffTag.compareTo("INSERT") == 0) {
				setBackground(green);
				labelText += row.getNewLine();
			} else if (diffTag.compareTo("DELETE") == 0) {
				setBackground(red);
				labelText = " ";
			} else {
				labelText += row.getOldLine();
				setBackground(Color.white);
			}

			setText(labelText);

			return this;
		}
	}

	/**
	 * 
	 * Get array of diff rows, representing the diff of two lists of strings. A
	 * line can be matched with another line in the other list through an
	 * operation of add, remove, change or delete.
	 * 
	 * @param originalList
	 * @param revisedList
	 * @return
	 */
	private IndexedDiffRow[] getDiffData(List<String> originalList,
			List<String> revisedList) {

		DiffRowGenerator.Builder builder = new DiffRowGenerator.Builder();
		boolean sideBySide = true;
		builder.showInlineDiffs(!sideBySide);
		builder.columnWidth(100);
		DiffRowGenerator dfg = builder.build();
		List<DiffRow> rows = dfg.generateDiffRows(originalList, revisedList);

		List<IndexedDiffRow> rowsInd = new ArrayList<>();

		int index1 = 0;
		int index2 = 0;

		for (DiffRow d : rows) {
			index1++;
			index2++;

			if (d.getTag() == difflib.DiffRow.Tag.DELETE) {
				index2--;
			}

			if (d.getTag() == difflib.DiffRow.Tag.INSERT) {
				index1--;
			}

			if (d.getTag() == difflib.DiffRow.Tag.CHANGE) {
				if (d.getNewLine().length() == 0)
					index2--;
				if (d.getOldLine().length() == 0)
					index1--;
			}

			rowsInd.add(new IndexedDiffRow(d.getTag(), d.getOldLine(), d
					.getNewLine(), index1, index2));
		}

		IndexedDiffRow result[] = new IndexedDiffRow[rowsInd.size()];
		rowsInd.toArray(result);

		return result;
	}

	/**
	 * 
	 * set data in the assembly lists after the user has selected the functions
	 * to diff
	 * 
	 * @param assembly1
	 * @param assembly2
	 * @param listDiff1
	 * @param listDiff2
	 */
	private void setDiff(List<AssemblyLineModel> assembly1,
			List<AssemblyLineModel> assembly2, JList<IndexedDiffRow> listDiff1,
			JList<IndexedDiffRow> listDiff2) {
		List<String> code1 = new ArrayList<>();
		List<String> code2 = new ArrayList<>();

		if (assembly1 != null) {
			for (AssemblyLineModel l : assembly1) {
				code1.add(l.name);
			}
		}

		if (assembly2 != null) {
			for (AssemblyLineModel l : assembly2) {
				code2.add(l.name);
			}
		}

		listDiff1.setListData(getDiffData(code1, code2));
		listDiff2.setListData(getDiffData(code1, code2));
	}

	private void setAssemblyLists() {
		try {
			Gson gson = new Gson();
			java.lang.reflect.Type stringStringMap = new TypeToken<List<Map<String, String>>>() {
			}.getType();

			String assembly1 = getAssemblyParsedData(selectedFile1);

			try {
				assemblyList1 = gson.fromJson(assembly1, stringStringMap);
			} catch (JsonSyntaxException e) {
				System.err.println(assembly1);
			}

			if (selectedFile2 == null) {
				assemblyList2 = null;
			} else {
				String assembly2 = getAssemblyParsedData(selectedFile2);

				try {
					assemblyList2 = gson.fromJson(assembly2, stringStringMap);
				} catch (JsonSyntaxException e) {
					System.err.println(assembly2);
				}
			}
		} catch (IOException | JsonSyntaxException e1) {
			e1.printStackTrace();
		}
	}

	public static void checkAndCreateCacheDirs() {
		File f = new File(functionCacheDir);
		if (f.exists() == false) {
			if (f.mkdir() == false) {
				System.out.println(f + " could not be created");
			}
		}

		f = new File(assemblyCacheDir);
		if (f.exists() == false) {
			if (f.mkdir() == false) {
				System.out.println(f + " could not be created");
			}
		}
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.EMBEDDED
				| SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(composite);
		final JButton button1 = new JButton("select file 1");
		final JButton button2 = new JButton("select file 2");
		final JButton refreshButton = new JButton("refresh");

		button1.setEnabled(true);
		button2.setEnabled(false);
		refreshButton.setEnabled(true);

		refreshButton
				.setToolTipText("Empty the cache directories and clear the current data");

		final JLabel label1 = new JLabel();
		final JLabel label2 = new JLabel();

		final JList<IndexedDiffRow> diffJList1 = new JList<IndexedDiffRow>();
		final JList<IndexedDiffRow> diffJList2 = new JList<IndexedDiffRow>();

		diffJList1.setDoubleBuffered(true);
		diffJList2.setDoubleBuffered(true);

		final JList<String> funcJList1 = new JList<String>();
		final JList<String> funcJList2 = new JList<String>();

		funcJList1.setLayoutOrientation(JList.VERTICAL);
		funcJList2.setLayoutOrientation(JList.VERTICAL);

		funcJList1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		funcJList2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		button1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				int status = fileChooser.showOpenDialog(null);
				if (status == JFileChooser.APPROVE_OPTION) {
					selectedFile1 = fileChooser.getSelectedFile();
					try {
						String[] data = getFunctionsParsedData(selectedFile1);
						if (data != null) {
							funcJList1.setListData(data);
							label1.setText(selectedFile1.getAbsolutePath());
							button2.setEnabled(true);
							setAssemblyLists();
						}
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});

		button2.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				int status = fileChooser.showOpenDialog(null);
				if (status == JFileChooser.APPROVE_OPTION) {
					selectedFile2 = fileChooser.getSelectedFile();
					try {
						String[] data = getFunctionsParsedData(selectedFile2);
						if (data != null) {
							funcJList2.setListData(data);
							label2.setText(selectedFile2.getAbsolutePath());
							setAssemblyLists();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		refreshButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					FileUtils.cleanDirectory(new File(functionCacheDir));
					FileUtils.cleanDirectory(new File(assemblyCacheDir));

					funcJList1.setListData(new String[0]);
					funcJList2.setListData(new String[0]);

					diffJList1.setListData(new IndexedDiffRow[0]);
					diffJList2.setListData(new IndexedDiffRow[0]);

					label1.setText("");
					label2.setText("");

					button2.setEnabled(false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		funcJList1.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent evt) {
				JList<String> list = (JList<String>) evt.getSource();
				String functionName = (String) list.getSelectedValue();
				String code = "";

				for (Map<String, String> i : assemblyList1) {
					if (i.get(functionName) != null) {
						code = i.get(functionName);
						break;
					}
				}

				Gson gson = new Gson();

				java.lang.reflect.Type stringArray = new TypeToken<List<AssemblyLineModel>>() {
				}.getType();
				originalAssembly = gson.fromJson(code, stringArray);

				setDiff(originalAssembly, newAssembly, diffJList1, diffJList2);
			}
		});

		funcJList2.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent evt) {
				JList<String> list = (JList<String>) evt.getSource();
				String functionName = list.getSelectedValue();
				String code = "";

				for (Map<String, String> i : assemblyList2) {
					if (i.get(functionName) != null) {
						code = i.get(functionName);
						break;
					}
				}

				Gson gson = new Gson();

				java.lang.reflect.Type stringArray = new TypeToken<List<AssemblyLineModel>>() {
				}.getType();
				newAssembly = gson.fromJson(code, stringArray);

				setDiff(originalAssembly, newAssembly, diffJList1, diffJList2);
			}
		});

		diffJList1.setCellRenderer(new DiffRendererLeft());
		diffJList2.setCellRenderer(new DiffRendererRight());

		frame.setLayout(new GridBagLayout());
		frame.setTitle("Diff View");
		frame.setLocationRelativeTo(null);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.weightx = 1;
		c.gridy = 0;

		frame.add(button1, c);

		c.gridx = 1;
		JPanel refreshPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1;
		refreshPanel.add(button2, c2);
		c2.weightx = 0;
		refreshPanel.add(refreshButton, c2);

		frame.add(refreshPanel, c);

		c.gridy = 1;
		c.gridx = 0;
		frame.add(label1, c);
		c.gridx = 1;
		frame.add(label2, c);

		c.gridy = 2;
		c.gridx = 0;
		c.weighty = 0.2;

		frame.add(new JScrollPane(funcJList1), c);
		c.gridx = 1;
		frame.add(new JScrollPane(funcJList2), c);

		JScrollPane scrollPane1 = new JScrollPane(diffJList1);
		JScrollPane scrollPane2 = new JScrollPane(diffJList2);

		scrollPane1.getVerticalScrollBar()
				.setPreferredSize(new Dimension(0, 0));
		scrollPane2.getVerticalScrollBar().setModel(
				scrollPane1.getVerticalScrollBar().getModel());

		c.gridy = 3;
		c.gridx = 0;
		c.weighty = 0.8;

		frame.add(scrollPane1, c);

		c.gridx = 1;
		frame.add(scrollPane2, c);
	}

	@Override
	public void setFocus() {
	}
}