package dinoplugin.handlers;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;

import dino_plugin.Activator;

public class PreferencesHandler extends AbstractHandler {
	public static final String PREFS_DIR_KEY = "dino_binary_dir";

	/**
	 * The constructor.
	 */
	public PreferencesHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		Object[] options = { "Yes", "No" };
		int selectedValue;

		if (store.contains(PREFS_DIR_KEY)) {
			selectedValue = JOptionPane.showOptionDialog(
					null,
					"Current binary directory is "
							+ store.getString(PREFS_DIR_KEY) + ", change it?",
					"Set binary directory", JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

			if (selectedValue != 0)
				return null;
		}

		JFileChooser dirChooser = new JFileChooser();
		dirChooser.setCurrentDirectory(new File("/"));
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int status = dirChooser.showOpenDialog(null);

		if (status == JFileChooser.APPROVE_OPTION) {
			File selectedFile = dirChooser.getSelectedFile();
			store.putValue(PREFS_DIR_KEY, selectedFile.getAbsolutePath());
		}

		return null;
	}
}
