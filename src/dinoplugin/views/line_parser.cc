#include <fstream>
#include <string>
#include "Symtab.h"
#include "Archive.h"
#include "CodeObject.h"
#include "CFG.h"
#include "InstructionDecoder.h"
#include "Instruction.h"
#include "LineInformation.h"

using namespace Dyninst;
using namespace SymtabAPI;
using namespace ParseAPI;
using namespace InstructionAPI;

int main(int argc, char **argv){
	if(argc != 3){
		printf("usage: %s <executable path> <source file path>\n", argv[0]);
		return -1;
	}

	char *binaryPath = argv[1];
	char *sourceFile = argv[2];

	std::string executableFile(binaryPath);
	std::string sourceFileStr(sourceFile);

	Symtab *obj = NULL;
	Symtab::openFile(obj, executableFile);

	Module *mod;
	obj->findModuleByName(mod, sourceFile);

	if(mod == NULL){
		stringstream out;

		vector<Module *> v;
		obj->getAllModules(v);

		out << "No module with name \"" << sourceFileStr << "\" found." << endl;
		out << "Original source file names: " << endl;

		for(int i=0; i<v.size(); i++)
			out << v[i]->fullName() << endl;

		cout << out.str();
		return -1;
	}

	LineInformation *info = mod->getLineInformation();

	if(info == NULL){
		stringstream out;

		out << "No line information found for module \"" << sourceFileStr << "\"." << endl;
		out << "Make sure that the executable was compiled with debug information" << endl;

		cout << out.str();
		return -1;
	}

	SymtabCodeSource *sts = new SymtabCodeSource(obj);
	const unsigned char* memBuffer = (const unsigned char*)sts->getPtrToInstruction(mod->addr());
	InstructionDecoder decoder(memBuffer, InstructionDecoder::maxInstructionLength, obj->getArchitecture());

	vector<pair<int, string>> lineVector;

	//print every line
	LineInformation::const_iterator iter;
	for(iter = info->begin(); iter != info->end(); iter++){
		const std::pair<Offset, Offset> addrRange = iter->first;
		LineNoTuple lt = iter->second;

		Address start = addrRange.first;
		Address end = addrRange.second;
		int lineNumber = lt.second;
		while(start <= end){
			Instruction::Ptr instr = decoder.decode((const unsigned char *)sts->getPtrToInstruction(start));
			start += instr->size();
			lineVector.push_back(make_pair(lineNumber, instr->format()));
		}
	}

	stringstream out;

	out << "[" << endl;
	for(vector<pair<int, string>>::iterator it=lineVector.begin(); it!= lineVector.end(); it++){
		out << "{\"content\":\"" << it->second << "\",\"lineNumber\":" << it->first << "}," << endl;
	}

	string res = out.str();
	res.pop_back();
	res.pop_back();
	res.append("\n]\n");

	cout << res;
	return 0;
}
