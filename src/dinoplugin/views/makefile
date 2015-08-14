install_dir = /tmp/dino/

all: assembly_parser line_parser function_parser

assembly_parser: assembly_parser.cc
	g++ -g -std=c++0x assembly_parser.cc -o $(install_dir)assembly_parser -I/usr/include/dyninst -lparseAPI -linstructionAPI -lsymtabAPI -lsymLite -ldynDwarf -ldynElf -lcommon -L/usr/include/dwarf.h -ldwarf

line_parser: line_parser.cc
	g++ -g -std=c++0x line_parser.cc -o $(install_dir)line_parser -I/usr/include/dyninst -lparseAPI -linstructionAPI -lsymtabAPI -lsymLite -ldynDwarf -ldynElf -lcommon -L/usr/include/dwarf.h -ldwarf

function_parser: function_parser.cc
	g++ -g -std=c++0x function_parser.cc -o $(install_dir)function_parser -I/usr/include/dyninst -lparseAPI -linstructionAPI -lsymtabAPI -lsymLite -ldynDwarf -ldynElf -lcommon -L/usr/include/dwarf.h -ldwarf
