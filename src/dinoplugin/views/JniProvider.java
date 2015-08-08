package dinoplugin.views;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dinoplugin.views.DiffView.FuncModel;

public class JniProvider {
	public static String binaryDirPath = "/usr/local/apache-tomcat-8.0.23/webapps/ROOT/WEB-INF/classes/gsoc-binaries/";
	public static String functionCachePath = "/usr/local/apache-tomcat-8.0.23/webapps/ROOT/WEB-INF/classes/cached-functions/";
	public static String assemblyCachePath = "/usr/local/apache-tomcat-8.0.23/webapps/ROOT/WEB-INF/classes/cached-assembly/";

	public native void getFunctionsJni(String binaryPath, String jsonPath);

	public native void getAssemblyJni(String binaryPath, String jsonPath);

	static {
		System.loadLibrary("dyninstParser");
	}

	public static Boolean isFunctionCached(String fileName) {
		File cacheDir = new File(functionCachePath);
		String[] cachedBinaries = cacheDir.list();

		if (cachedBinaries == null)
			return false;

		for (String s : cachedBinaries) {
			if (s.compareTo(fileName) == 0) {
				return true;
			}
		}
		return false;
	}

	public static Boolean isAssemblyCached(String fileName) {
		File cacheDir = new File(assemblyCachePath);
		String[] cachedBinaries = cacheDir.list();

		if (cachedBinaries == null)
			return false;

		for (String s : cachedBinaries) {
			if (s.compareTo(fileName) == 0) {
				return true;
			}
		}

		return false;
	}

	public static String getAssembly(String fileName) throws IOException {
		// if the functions are not cached, parse them and save them to cache
		if (isAssemblyCached(fileName) == false) {
			try {
				new JniProvider().getAssemblyJni(binaryDirPath + fileName,
						assemblyCachePath + fileName);
			} catch (Exception e) {
				return null;
			}
		}
		// return the cached result
		return FileUtils
				.readFileToString(new File(assemblyCachePath + fileName));
	}

	/**
	 * @param fileName
	 * @param sortMode
	 * @param sortDirection
	 * @return
	 * @throws IOException
	 */

	public static String[] getFunctions(String fileName, String sortMode,
			final String sortDirection) throws IOException {
		// if the functions are not cached, parse them and save them to cache
		if (isFunctionCached(fileName) == false) {

			try {
				new JniProvider().getFunctionsJni(binaryDirPath + fileName,
						functionCachePath + fileName);
			} catch (Exception e) {
				return null;
			}
		}
		// return the cached result
		String source = FileUtils.readFileToString(new File(functionCachePath
				+ fileName));

		if (source.startsWith("error")) {
			return null;
		}

		Gson gson = new Gson();
		java.lang.reflect.Type stringStringMap = new TypeToken<List<FuncModel>>() {
		}.getType();
		List<FuncModel> funcList = gson.fromJson(source, stringStringMap);

		if (sortMode.compareTo("size") == 0) {
			Collections.sort(funcList, new Comparator<FuncModel>() {

				@Override
				public int compare(FuncModel o1, FuncModel o2) {
					if (sortDirection.compareTo("ascending") == 0) {
						return (int) (o1.size - o2.size);
					} else {
						return (int) (o2.size - o1.size);
					}
				}
			});
		} else if (sortMode.compareTo("name") == 0) {
			Collections.sort(funcList, new Comparator<FuncModel>() {

				@Override
				public int compare(FuncModel o1, FuncModel o2) {
					if (sortDirection.compareTo("ascending") == 0) {
						return o1.name.compareTo(o2.name);
					} else {
						return o2.name.compareTo(o1.name);
					}
				}
			});
		} else if (sortMode.compareTo("address") == 0) {
			// sorted by default by address from c++
		}

		ArrayList<String> res = new ArrayList<>();
		for (FuncModel f : funcList) {
			res.add(f.name);
		}

		String[] resArr = new String[res.size()];
		res.toArray(resArr);
		return resArr;
	}

}
