## Dino plugin - GSoC 2015

####Overview:  

The plugin offers two views which can be used to analyse executable files:

1. Diff View:  
You can select two executable files, browse through their functions and get a
diff between the contents of any two functions.

2. Source View:  
Select an executable file (compiled with debug information) and it's source code
file and you are presented with a mapping of source code->assembly code.  
This way, you can easily see how the compiler transforms a line of source code into a
block of many assembly instructions.

The UI is done using Swing.

The **Dyninst framework** was used for parsing executable files: http://www.dyninst.org/

The plugin is based on three C++ programs as "data sources", which are used to get the
assembly code for an executable file. These are generated via the install.sh script.

####Source code:  

1. **DiffView.java**: Contains methods which read assembly data from a file, renderer classes
which implement the diff lists. The algorithm which returns the diff data is from the open
source library **java-diff-utils**: http://code.google.com/p/java-diff-utils/

2. **SourceView.java**: The mapping between source code and assembly code is realized through
the Dyninst Framework's functionality of getting source line information for every assembly
instruction.

3. **function_parser.cc**: returns a list of functions contained in an executable file whose
path is received as a command line parameter

4. **assembly_parser.cc**: returns a list of functions together with their assembly code for
a given executable file

5. **line_parser.cc**: returns a list of mappings {line number in source file : assembly instruction}
for an executable file and source file name given as command line parameters. The source file name
has to be the name of the file from which the executable was compiled.

####How to build: 

To create a new **eclipse plugin project**:  
* Create a new plugin project with view
* Copy the diff view code in the first view
* Add commons.io, diffutils and gson jars to classpath in plugin.xml->runtime
* Add a new view in plugin.xml and add the code for the source view
* Export plugin from File > Export... > Plug-in Development > Deployable plug-ins and fragments

**Installing the eclipse plugin**:  
Copy the .jar file in the dropins folder of the eclipse instalation. This is
usually in ```/usr/share/eclipse```, or in the same folder as the eclipse executable
if you haven't installed eclipse and just start the executable from a directory.

**Installing the binary dependencies**:
-install the dyninst suite (instructions here for Debian based:
https://github.com/alinmindroc/dyninst_parser_GSOC)
-run the complex script ```./install.sh```, which should create the three executables
in ```/tmp/dino``` (g++ >= 4.7 needed)

**Using the plugin**:
Open eclipse, go to window->show view->other...->Dino Category and select the
Diff View and the Source View.
If the Dino Category doesn't appear, try restarting eclipse from a
console with the -clean argument.




