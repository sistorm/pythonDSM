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

public class Main2 {

	private static String baseParentDirectory = "";

	private static ArrayList<File> mapIndexes;

	private static ArrayList<ArrayList<Integer>> matrix;

	private static HashMap<File, Integer> fileDepth;

	private static Object baseParentDirectoryFile;

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

		sortbyDependency(1);

		printMatrix(maxDepth);

	}

	private static void sortbyDependency(int depthToSort) {

		HashMap<File, ArrayList<File>> subNodules = new HashMap<>();

		ArrayList<File> arrToSort = new ArrayList<>();
		for (File f : mapIndexes) {

			int dept = fileDepth.get(f);

			if (dept == depthToSort + 1) {
				arrToSort.add(f);
			} else if (dept == depthToSort) {

				subNodules.put(f, new ArrayList<>(arrToSort));

				arrToSort = new ArrayList<>();
			}

		}

		for (Entry<File, ArrayList<File>> module : subNodules.entrySet()) {

			arrToSort = module.getValue();
			File m = module.getKey();

			int mIndex = mapIndexes.indexOf(m);

			for (int i = (arrToSort.size() -1 ); i >= 0; i--) {

				for (int j = 1; j <= i; j++) {

					File jm1File = arrToSort.get(j - 1);
					File jFile = arrToSort.get(j);

					int jm1FileIndex = mapIndexes.indexOf(jm1File);
					int jFileIndex = mapIndexes.indexOf(jFile);

					int jm1FileDependency = matrix.get(jm1FileIndex).get(jFileIndex);

					int jFileDependency = matrix.get(jFileIndex).get(jm1FileIndex);
					
					
					
					
					if (jm1FileDependency >= jFileDependency) {

						{
							File tempInArray = arrToSort.remove(j - 1);

							arrToSort.add(j, tempInArray);
						}
						{
							File tempjm1 = mapIndexes.remove(jm1FileIndex);

							mapIndexes.add(jFileIndex, tempjm1);
						}

						{
							ArrayList<Integer> subModmatrix1 = matrix.remove(jm1FileIndex);

							matrix.add(jFileIndex, subModmatrix1);

							for (int k = 0; k < mapIndexes.size(); ++k) {

								ArrayList<Integer> subModmatrix = matrix.get(k);

								int tempDep = subModmatrix.remove(jm1FileIndex);

								subModmatrix.add(jFileIndex, tempDep);

							}

						}

						int fileToMoveIdx = jm1FileIndex - 1;
						int destinationFileIdx = jFileIndex - 1;
						while (fileToMoveIdx >= 0 && destinationFileIdx >= 0
								&& fileDepth.get(mapIndexes.get(fileToMoveIdx)) > depthToSort + 1) {

							ArrayList<Integer> subModmatrix1 = matrix.remove(fileToMoveIdx);

							matrix.add(destinationFileIdx, subModmatrix1);

							for (int k = 0; k < mapIndexes.size(); ++k) {

								ArrayList<Integer> subModmatrix = matrix.get(k);

								int tempDep = subModmatrix.remove(fileToMoveIdx);

								subModmatrix.add(destinationFileIdx, tempDep);

							}

							{
								File fToMove = mapIndexes.remove(fileToMoveIdx);

								mapIndexes.add(destinationFileIdx, fToMove);
							}

							fileToMoveIdx = fileToMoveIdx - 1;
							destinationFileIdx = destinationFileIdx - 1;

						}
					}
				}
			}
		}
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
									Integer depFileIndex = null;
									Integer currentValue = null;

									File fparentCurrFile = file;
									File fparent = dependantFile;
									int fIndex = mapIndexes.indexOf(fparentCurrFile);
									depFileIndex = mapIndexes.indexOf(fparent);

									while (fparentCurrFile.exists()
											&& !fparentCurrFile.equals(baseParentDirectoryFile)) {
										
										fparent = dependantFile;
										
										while (fparent.exists() && !fparent.equals(baseParentDirectoryFile)) {

											depFileIndex = mapIndexes.indexOf(fparent);

											fIndex = mapIndexes.indexOf(fparentCurrFile);

											currentValue = matrix.get(fIndex).get(depFileIndex);
											matrix.get(fIndex).set(depFileIndex, currentValue + 1);

											fparent = fparent.getParentFile();
										}
										fparentCurrFile = fparentCurrFile.getParentFile();

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
									Integer depFileIndex = null;
									Integer currentValue = null;

									File fparentCurrFile = file;
									File fparent = dependantFile;
									int fIndex = mapIndexes.indexOf(fparentCurrFile);
									depFileIndex = mapIndexes.indexOf(fparent);

									while (fparentCurrFile.exists()
											&& !fparentCurrFile.equals(baseParentDirectoryFile)) {

										fparent = dependantFile;
										
										while (fparent.exists() && !fparent.equals(baseParentDirectoryFile)) {

											depFileIndex = mapIndexes.indexOf(fparent);

											fIndex = mapIndexes.indexOf(fparentCurrFile);

											currentValue = matrix.get(fIndex).get(depFileIndex);
											matrix.get(fIndex).set(depFileIndex, currentValue + 1);

											fparent = fparent.getParentFile();
										}
										fparentCurrFile = fparentCurrFile.getParentFile();

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

	private static boolean contains(File f1, File f2) {

		if (f1.equals(f2))
			return false;

		if (f1.isFile() && f2.isFile())
			return false;

		if (f1.isDirectory() && f2.getAbsolutePath().contains(f1.getAbsolutePath()))
			return true;
		else
			return false;
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

				File dependantFile = mapIndexes.get(k);

				 if(!contains(file, dependantFile)
				 && !contains(dependantFile, file))
				 {

					Integer dependency = subDep.get(k);
	
					print("" + dependency.toString());

				 }else
				 print("0");
				print(",");
			}
			print("\n");
		}
	}

	private static void print(String string) {
		System.out.print(string);

	}

}
