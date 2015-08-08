#include <jni.h>
#include <stdio.h>
#include <map>
#include <vector>
#include <unordered_map>
#include <sstream>
#include <memory>
#include <fstream>
#include "CodeObject.h"
#include "CFG.h"
#include "../../dinoplugin_views_JniProvider.h"

using namespace std;
using namespace Dyninst;
using namespace ParseAPI;

JNIEXPORT void JNICALL Java_dinoplugin_views_JniProvider_getFunctionsJni
(JNIEnv * env, jobject obj, jstring jBinaryPath, jstring jJsonPath){
	char *binaryPath = (char*) env->GetStringUTFChars(jBinaryPath, 0);
	char *jsonPath = (char*) env->GetStringUTFChars(jJsonPath, 0);

	stringstream outstream;
	ofstream jsonStream;
	jsonStream.open(jsonPath);

	vector<Function *> funcs;
	SymtabCodeSource *sts;
	CodeObject *co;
	CodeRegion *cr;

	SymtabAPI::Symtab *symTab;
	std::string binaryPathStr(binaryPath);
	bool isParsable = SymtabAPI::Symtab::openFile(symTab, binaryPathStr);

	if(isParsable == false){
		const char *error = "error: file can not be parsed";
		jsonStream << error;
		jsonStream.close();
		return;
	}

	sts = new SymtabCodeSource(binaryPath);
	co = new CodeObject(sts);

	//parse the binary given as a command line arg
	co->parse();

	//get list of all functions in the binary
	const CodeObject::funclist &all = co->funcs();

	//iterate the ContainerWrapper
	auto fit = all.begin();
	for(int i=0; fit != all.end(); ++fit, i++){
		Function *f = *fit;

		Address startAddr = f->addr();
		Address lastAddr;

		auto fbl = f->exitBlocks().end();
		if(f->exitBlocks().empty() == false){
			fbl--;
			Block *b = *fbl;
			lastAddr = b->last();
		} else {
			continue;
		}

		if(startAddr >= lastAddr)
			continue;

		outstream << "{\"address\":\"" << hex << f->addr() << "\",\"name\":\"" << f->name();
		outstream << "\",\"size\":\"" << dec << lastAddr - startAddr << "\"}," << endl;
	}

	string resp = outstream.str();

	if(resp.size() == 0){
		const char *error = "error: no functions parsed";
		jsonStream << error;
		jsonStream.close();
		return;
	}

	resp.pop_back();
	resp.pop_back();

	resp.insert(0, "[");
	resp.append("]");

	jsonStream << resp;
	jsonStream.close();
}
