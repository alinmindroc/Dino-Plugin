package dinoplugin.handlers;

import javax.swing.JOptionPane;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class HelpHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public HelpHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String body = "Dino plugin v1.0.\n"
				+ "\n"
				+ "1. Diff View: Select two executable files and browse through their functions and assembly code, and get quick diffs between them\n"
				+ "2. Source View: Select an executable file compiled with debug information and its original source file and get a mapping between assembly and source code\n"
				+ "3. Select the directory where the parser binaries are located (specified when installing)\n";
		String title = "Help";

		JOptionPane.showMessageDialog(null, body, title,
				JOptionPane.INFORMATION_MESSAGE);

		return null;
	}
}
