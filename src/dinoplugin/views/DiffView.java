package dinoplugin.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
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
import com.google.gson.reflect.TypeToken;

import difflib.DiffRow;
import difflib.DiffRowGenerator;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class DiffView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "dinoplugin.views.DiffView";

	class FuncModel {
		public String address;
		public String name;
		public long size;

		public String toString() {
			return address + ":" + name + ", size:" + size + "\n";
		}
	}

	class AssemblyLineModel {
		public String address;
		public String name;
		public String destName;
		public String destAddr;

		public String toString() {
			return address + ": " + name;
		}
	}

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

	JButton button1 = new JButton("select file 1");
	JButton button2 = new JButton("select file 2");

	JList<String> list1 = new JList<String>();
	JList<String> list2 = new JList<String>();

	JList<IndexedDiffRow> listDiff1 = new JList<IndexedDiffRow>();
	JList<IndexedDiffRow> listDiff2 = new JList<IndexedDiffRow>();

	String selectedFunction1 = null;
	String selectedFunction2 = null;

	String selectedFile1 = null;
	String selectedFile2 = null;

	List<AssemblyLineModel> originalAssembly = null;
	List<AssemblyLineModel> newAssembly = null;

	List<Map<String, String>> funcsList1 = null;
	List<Map<String, String>> funcsList2 = null;

	private class DiffRendererLeft extends JLabel implements
			ListCellRenderer<IndexedDiffRow> {

		public DiffRendererLeft() {
			setOpaque(true);
		}

		Color green = new Color(149, 234, 149);
		Color yellow = new Color(250, 217, 135);
		Color red = new Color(239, 153, 153);
		Color grey = new Color(220, 220, 219);

		@Override
		public Component getListCellRendererComponent(
				JList<? extends IndexedDiffRow> list, IndexedDiffRow row,
				int index, boolean isSelected, boolean hasFocus) {

			String diffTag = row.getTag().name();
			String labelText = "";
			labelText = String.format("%4d ", row.index1);

			if (funcsList2 == null) {
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

		Color green = new Color(149, 234, 149);
		Color yellow = new Color(250, 217, 135);
		Color red = new Color(239, 153, 153);
		Color grey = new Color(220, 220, 219);

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

	private void setDiff(List<AssemblyLineModel> assembly1,
			List<AssemblyLineModel> assembly2) {
		List<String> code1 = new ArrayList<>();
		List<String> code2 = new ArrayList<>();

		for (AssemblyLineModel l : assembly1) {
			code1.add(l.name);
		}

		if (assembly2 == null) {
			code2 = new ArrayList<>();
		} else {
			for (AssemblyLineModel l : assembly2) {
				code2.add(l.name);
			}
		}

		listDiff1.setListData(getDiffData(code1, code2));
		listDiff2.setListData(getDiffData(code1, code2));
	}

	private void setFunctionAssembly() {
		try {
			Gson gson = new Gson();
			java.lang.reflect.Type stringStringMap = new TypeToken<List<Map<String, String>>>() {
			}.getType();

			funcsList1 = gson.fromJson(JniProvider.getAssembly(selectedFile1),
					stringStringMap);

			if (selectedFile2 == null) {
				funcsList2 = null;
			} else {
				funcsList2 = gson
						.fromJson(JniProvider.getAssembly(selectedFile2),
								stringStringMap);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	/**
	 * The constructor.
	 */
	public DiffView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.EMBEDDED
				| SWT.NO_BACKGROUND);
		Frame frame = SWT_AWT.new_Frame(composite);

		button1.setEnabled(true);
		button2.setEnabled(false);

		button1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				int status = fileChooser.showOpenDialog(null);
				if (status == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					try {
						selectedFile1 = selectedFile.getName();
						list1.setListData(JniProvider.getFunctions(
								selectedFile1, "name", "ascending"));
						button1.setText("(selected:" + selectedFile1 + ")");
						button2.setEnabled(true);
						setFunctionAssembly();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else if (status == JFileChooser.CANCEL_OPTION) {
					System.out.println("canceled");
				}
			}
		});

		button2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				int status = fileChooser.showOpenDialog(null);
				if (status == JFileChooser.APPROVE_OPTION) {
					File selectedFile = fileChooser.getSelectedFile();
					try {
						selectedFile2 = selectedFile.getName();
						list2.setListData(JniProvider.getFunctions(
								selectedFile2, "name", "ascending"));
						button2.setText("(selected:" + selectedFile2 + ")");
						setFunctionAssembly();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				} else if (status == JFileChooser.CANCEL_OPTION) {
					System.out.println("canceled");
				}
			}
		});

		list1.setLayoutOrientation(JList.VERTICAL);
		list2.setLayoutOrientation(JList.VERTICAL);

		list1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		list1.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent evt) {
				JList<String> list = (JList<String>) evt.getSource();
				String functionName = (String) list.getSelectedValue();
				String code = "";

				for (Map<String, String> i : funcsList1) {
					if (i.get(functionName) != null) {
						code = i.get(functionName);
						break;
					}
				}

				Gson gson = new Gson();

				java.lang.reflect.Type stringArray = new TypeToken<List<AssemblyLineModel>>() {
				}.getType();
				originalAssembly = gson.fromJson(code, stringArray);

				// System.out.println("old: " + functionName
				// + originalAssembly);
				// System.out.println("new: " + newAssembly);
				// System.out.println();

				setDiff(originalAssembly, newAssembly);
			}
		});

		list2.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent evt) {
				JList<String> list = (JList<String>) evt.getSource();
				String functionName = list.getSelectedValue();
				String code = "";

				for (Map<String, String> i : funcsList2) {
					if (i.get(functionName) != null) {
						code = i.get(functionName);
						break;
					}
				}

				Gson gson = new Gson();

				java.lang.reflect.Type stringArray = new TypeToken<List<AssemblyLineModel>>() {
				}.getType();
				newAssembly = gson.fromJson(code, stringArray);

				// System.out.println("old: " + originalAssembly);
				// System.out.println("new: " + functionName + newAssembly);
				// System.out.println();

				setDiff(originalAssembly, newAssembly);
			}
		});

		listDiff1.setCellRenderer(new DiffRendererLeft());
		listDiff2.setCellRenderer(new DiffRendererRight());

		frame.setLayout(new GridLayout(0, 2));
		frame.setTitle("Quit button");
		frame.setSize(300, 200);
		frame.setLocationRelativeTo(null);

		frame.add(button1);
		frame.add(button2);

		frame.add(new JScrollPane(list1));
		frame.add(new JScrollPane(list2));

		JScrollPane scrollPane1 = new JScrollPane(listDiff1);
		JScrollPane scrollPane2 = new JScrollPane(listDiff2);

		scrollPane1.getVerticalScrollBar()
				.setPreferredSize(new Dimension(0, 0));
		scrollPane2.getVerticalScrollBar().setModel(
				scrollPane1.getVerticalScrollBar().getModel());

		frame.add(scrollPane1);
		frame.add(scrollPane2);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}
}