package pythonDSM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.plaf.FileChooserUI;

public class Main2 {

	private static String baseParentDirectory = "";

	private static ArrayList<File> mapIndexes;

	private static ArrayList<ArrayList<Integer>> matrix;

	private static HashMap<File, Integer> fileDepth;

	private static Object baseParentDirectoryFile;

	public Main2() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String path = args[0];

		File directory = new File(path);
		// System.out.print(directory.getName() + "\n");

		baseParentDirectory = directory.getParentFile().getAbsolutePath();
		baseParentDirectoryFile = new File(baseParentDirectory);
		mapIndexes = new ArrayList<>();

		matrix = new ArrayList<>();

		fileDepth = new HashMap<>();

		int maxDepth = buildFileDependencies(directory.getAbsolutePath(), 1);

		buildMatrix();

		populateDependencies();

		printMatrix(maxDepth);

	}

	private static void populateDependencies() {
		FileReader fr = null;
		BufferedReader br = null;

		for (int fileIndex = 0; fileIndex < mapIndexes.size(); ++fileIndex) {

			File file = mapIndexes.get(fileIndex);
			if (file.isFile()) {
				try {
					fr = new FileReader(file);
					br = new BufferedReader(fr);
					String scurrentLine = "";
					while ((scurrentLine = br.readLine()) != null) {
						if (scurrentLine.trim().startsWith("#"))
							// this is a comment
							continue;

						if (scurrentLine.contains("import")) {

							if (scurrentLine.contains("from")) {
								String[] split = scurrentLine.split("from ");
								split = split[1].split(" import ");

								String importedFile = split[1];

								String fromPackageStr = split[0];
								String[] fromsplittedPackage = fromPackageStr.split("\\.");

								String absoluteFilePath = baseParentDirectory;
								for (String str : fromsplittedPackage) {
									absoluteFilePath += ("\\" + str);
								}
								absoluteFilePath += ("\\" + importedFile + ".py");

								File dependantFile = new File(absoluteFilePath);
								if (!dependantFile.exists()) {
									// System.err.println("dependantFile not
									// found (\""+dependantFilePath +"\"");
								} else {
									int depFileIndex = mapIndexes.indexOf(dependantFile);
	
									int currentValue = matrix.get(fileIndex).get(depFileIndex);
									matrix.get(fileIndex).set(depFileIndex, currentValue + 1);
									
									
									File fparentdep = file.getParentFile();
									int fIndex = fileIndex;
									
									while(fparentdep.exists() && !fparentdep.equals(baseParentDirectoryFile)){
										fIndex = mapIndexes.indexOf(fparentdep);
										
										currentValue = matrix.get(fIndex).get(depFileIndex);
										matrix.get(fIndex).set(depFileIndex, currentValue + 1);
										
										fparentdep = fparentdep.getParentFile();

									}
									
									
									File fparent = dependantFile.getParentFile();
									while(fparent.exists() && !fparent.equals(baseParentDirectoryFile)){
										depFileIndex = mapIndexes.indexOf(fparent);
										
										currentValue = matrix.get(fileIndex).get(depFileIndex);
										matrix.get(fileIndex).set(depFileIndex, currentValue + 1);
										
										fparent = fparent.getParentFile();
										
										
									}
								}
							} else {
								String[] split = scurrentLine.split("import ");

								String dependantFilePath = file.getParent() + "\\" + split[1] + ".py";

								File dependantFile = new File(dependantFilePath);

								if (!dependantFile.exists()) {
									// System.err.println("dependantFile not
									// found (\""+dependantFilePath +"\"");
								} else {
									int depFileIndex = mapIndexes.indexOf(dependantFile);

									int currentValue = matrix.get(fileIndex).get(depFileIndex);
									matrix.get(fileIndex).set(depFileIndex, currentValue + 1);
									
									
									
									File fparentdep = file.getParentFile();
									int fIndex = fileIndex;
									
									while(fparentdep.exists() && !fparentdep.equals(baseParentDirectoryFile)){
										fIndex = mapIndexes.indexOf(fparentdep);
										
										currentValue = matrix.get(fIndex).get(depFileIndex);
										matrix.get(fIndex).set(depFileIndex, currentValue + 1);
										
										fparentdep = fparentdep.getParentFile();

									}
									
									File fparent = file.getParentFile();
									
									while(fparent.exists() && !fparent.equals(baseParentDirectoryFile)){
										depFileIndex = mapIndexes.indexOf(fparent);
										
										currentValue = matrix.get(fileIndex).get(depFileIndex);
										matrix.get(fileIndex).set(depFileIndex, currentValue + 1);
										
										fparent = fparent.getParentFile();
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
	}

	private static int buildFileDependencies(String absolutePath, int depth) {

		int maxDepth = 1;

		File file = new File(absolutePath);

		fileDepth.put(file, depth);

		if (file.isDirectory()) {
			File[] contents = file.listFiles(new FilenameFilter() {

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
						return -1;
					}

					if (!o1.isDirectory() && o2.isDirectory()) {
						return 1;
					}

					return 0;
				}

			});

			for (File f : contents) {
				maxDepth = Math.max(maxDepth, buildFileDependencies(f.getAbsolutePath(), depth + 1));

			}
		}

		mapIndexes.add(file);

		return maxDepth + 1;

	}

	private static void buildMatrix() {
		for (File f : mapIndexes) {
			matrix.add(new ArrayList<>());
		}

		for (ArrayList<Integer> subDep : matrix) {
			for (File f : mapIndexes) {
				subDep.add(0);
			}
		}

	}

	private static void printMatrix(int maxDepth) {

		ArrayList<ArrayList<File>> headerMatrix = new ArrayList<>();

		for (File f : mapIndexes) {

			Integer fDepth = fileDepth.get(f);

			ArrayList<File> arr = new ArrayList<>();

			for (int i = 1; i < maxDepth; ++i) {
				arr.add(null);
			}

			arr.set(fDepth - 1, f);

			headerMatrix.add(arr);
		}

		/** BEGIN Print column header */

		for (int j = 0; j < maxDepth - 1; ++j) {
			for (int k = 0; k < maxDepth - 1; ++k)
				print(",");

			for (int i = 0; i < headerMatrix.size(); ++i) {
				File f = headerMatrix.get(i).get(j);
				if (f != null)
					print(f.getName());
				print(",");
			}

			print("\n");
		}

		/** END Print column header */

		for (int i = 0; i < matrix.size(); i++) {
			ArrayList<Integer> subDep = matrix.get(i);

			File file = mapIndexes.get(i);
			Integer fDepth = fileDepth.get(file);

			Integer before = fDepth - 1;
			Integer after = maxDepth - 2 - before;

			/** BEGIN print row header */
			for (int b = 0; b < before; b++)
				print(",");
			print(file.getName() + "");
			for (int a = 0; a < after; a++)
				print(",");
			/** END print row header */
			print(",");

			for (int k = 0; k < subDep.size(); k++) {
				Integer dependency = subDep.get(k);

				print("" + dependency.toString() + ",");

			}
			print("\n");
		}
	}

	private static void print(String string) {
		System.out.print(string);

	}

}
