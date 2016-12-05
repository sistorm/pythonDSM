package pythonDSM;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class Main {

	private static String baseParentDirectory = "";
	private static HashMap dependency;

	public Main() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String path = args[0];

		File directory = new File(path);
		// System.out.print(directory.getName() + "\n");

		baseParentDirectory = directory.getParentFile().getAbsolutePath();

		dependency = new HashMap<>();

		HashMap subDependency = buildFileHM(directory.getAbsolutePath(), 1);

		dependency.put(directory, subDependency);

		parseDirectory(directory.toString(), directory.getAbsolutePath(), 0);

		printdependencyMap(dependency, "");

		printdependencyMapToCSV(dependency, "");
	}

	private static void printdependencyMap(HashMap<File, HashMap> dependency, String prefix) {
		for (Entry<File, HashMap> e : dependency.entrySet()) {

			System.out.print(prefix + "{");
			if (e.getKey().isDirectory()) {
				System.out.println(e.getKey().getName());
				printdependencyMap(e.getValue(), prefix + "\t");
			} else {
				System.out.println(e.getKey().getName());

				System.out.print(prefix + "\t{");
				for (Entry<File, Integer> sube : ((HashMap<File, Integer>) e.getValue()).entrySet()) {
					System.out.print("\n\t\t" + prefix + "{");
					System.out.print(sube.getKey().getName() + "," + sube.getValue());
					System.out.println("}");
				}

				System.out.println(prefix + "\t}");
			}

			System.out.println(prefix + "}");
		}

	}
	

	private static void printdependencyMapToCSV(HashMap<File, HashMap> dependency, String prefix) {
		int depth = calculateDepth(dependency);
		
		printHeaderCSV(dependency, depth);
		
	}

	private static void printHeaderCSV(HashMap<File, HashMap> dependency, int maxDepth) {
		
				
		for (int i = 0; i < (maxDepth); i++) {
			System.out.print("\t,");
		}
		
		int count = 0;
		for(Entry<File,HashMap> e : dependency.entrySet()){
			count ++;
			if(e.getKey().isDirectory())
			{
				int subElement = countChildElement(dependency);
				for (int i = 0; i < (subElement); i++) {
					System.out.print("\t,");
				}
			}
			
			System.out.print(e.getKey().getName());
			
			if((count ++) >= dependency.size() )
			{
				System.out.println();
			}
			
		}
		
		
		
		
		
		
	}


private static int countChildElement(HashMap<File, HashMap> dependency) {
		
		int depth = 1;
		
		if(dependency.isEmpty()) 
			return depth;
		
		for (Entry<File, HashMap> e : dependency.entrySet()) {
			if(e.getKey().isDirectory()){
				depth += countChildElement(e.getValue());
			}else{
				depth += 1;
			}
		}
		
		
		return depth;		
	}
	
	
	private static int calculateDepth(HashMap<File, HashMap> dependency) {
		
		int depth = 0;
		
		if(dependency.isEmpty()) 
			return depth;
		
		for (Entry<File, HashMap> e : dependency.entrySet()) {
			if(e.getKey().isDirectory()){
				depth = Math.max(depth, calculateDepth(e.getValue()));
			}
		}
		
		
		return depth+1;		
	}

	private static void parseDirectory(String packageFile, String path, int depth) {
		File directory = new File(path);

		File[] contents = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".pyc"))
					return false;
				if (name.startsWith("."))
					return false;

				return true;
			}
		});

		Arrays.sort(contents, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {

				if (!o1.isDirectory() && !o2.isDirectory() || o1.isDirectory() && o2.isDirectory()) {
					return o1.getName().compareTo(o2.getName());
				}

				if (o1.isDirectory() && !o2.isDirectory()) {
					return 1;
				}

				if (!o1.isDirectory() && o2.isDirectory()) {
					return -1;
				}

				return 0;
			}

		});

		for (File f : contents) {

			if (f.isDirectory()) {

				parseDirectory(packageFile + "." + f, f.getAbsolutePath(), depth + 1);
			} else {
				getFileDepency(packageFile, f);
			}
		}
	}

	private static HashMap buildFileHM(String path, int depth) {

		HashMap<File, HashMap> hMap = new HashMap<>();

		File directory = new File(path);

		File[] contents = directory.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".pyc"))
					return false;
				if (name.startsWith("."))
					return false;

				return true;
			}
		});

		Arrays.sort(contents, new Comparator<File>() {

			@Override
			public int compare(File o1, File o2) {

				if (!o1.isDirectory() && !o2.isDirectory() || o1.isDirectory() && o2.isDirectory()) {
					return o1.getName().compareTo(o2.getName());
				}

				if (o1.isDirectory() && !o2.isDirectory()) {
					return 1;
				}

				if (!o1.isDirectory() && o2.isDirectory()) {
					return -1;
				}

				return 0;
			}

		});

		for (File f : contents) {

			if (f.isDirectory()) {

				hMap.put(f, buildFileHM(f.getAbsolutePath(), depth + 1));

			} else {
				hMap.put(f, new HashMap<>());
			}
		}

		return hMap;
	}

	private static void getFileDepency(String packageFile, File f) {

		HashMap<File, HashMap<File, Integer>> currentPack = dependency;

		String[] splittedPackage = packageFile.split("\\.");
		for (String string : splittedPackage) {
			currentPack = (HashMap) currentPack.get(new File(string));
		}

		FileReader fr = null;
		BufferedReader br = null;

		try {

			fr = new FileReader(f);
			br = new BufferedReader(fr);
			String scurrentLine = "";
			while ((scurrentLine = br.readLine()) != null) {
				if (scurrentLine.contains("import")) {
					if (scurrentLine.contains("from")) {
						String[] split = scurrentLine.split("from ");
						split = split[1].split(" import ");

						String fromPackageStr = split[0];
						HashMap<File, HashMap<File, Integer>> fromPackage = dependency;

						String[] fromsplittedPackage = fromPackageStr.split("\\.");

						String currentRelativePath = "";
						for (String string : fromsplittedPackage) {
							fromPackage = (HashMap) fromPackage
									.get(new File(baseParentDirectory + currentRelativePath + "\\" + string));
							currentRelativePath = currentRelativePath + "\\" + string;
						}

						for (Entry<File, HashMap<File, Integer>> e : fromPackage.entrySet()) {
							if (e.getKey().getName().startsWith(split[1])) {

								currentPack.get(f).put(e.getKey(), 1);
								break;
							}
						}

					} else {
						String[] split = scurrentLine.split("import ");
						for (Entry<File, HashMap<File, Integer>> e : currentPack.entrySet()) {
							if (e.getKey().getName().startsWith(split[1])) {

								System.out.println(split[1]);
								currentPack.get(f).put(e.getKey(), 1);
								break;
							}
						}
					}
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {

			try {

				if (br != null)
					br.close();

				if (fr != null)
					fr.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}
		}

	}

}
