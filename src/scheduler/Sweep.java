package scheduler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import scheduler.SARetiming.SARetimingResultPackage;

public class Sweep {
	
	public static void multipleRuns(Graph graph, int runs, boolean allowShiftsGr1) {
		SARetiming sa = new SARetiming(graph);
		sa.setAllowShiftsGr1(allowShiftsGr1);
		
		SARetimingResultPackage[] resultPackages = new SARetimingResultPackage[runs];
		float avExeTime = 0;
		float bestCost = Float.MAX_VALUE;
		float worstCost = Float.MIN_VALUE;
		
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < runs; i++) {
			SARetimingResultPackage resultPackage = sa.run(0);
			resultPackages[i] = resultPackage;
			
			avExeTime += (float) resultPackage.wallclock;
			if (resultPackage.bestCost < bestCost) {
				bestCost = resultPackage.bestCost;
			}
			if (resultPackage.bestCost > worstCost) {
				worstCost = resultPackage.bestCost;
				resultPackage.printDiagnose();
			}
			
			System.out.println("Sweep:\t" + ((float) (i+1) / (float) runs * 100f) + "% done");
		}
		long wallclock = System.currentTimeMillis() - startTime;
		
		avExeTime /= 1000f * (float) runs;

		System.out.println("\n\n\n\nTotal sweep runtime:\t" + ((float) wallclock / 1000f) + "s");
		System.out.println("Average execution time:\t" + avExeTime + "s");
		System.out.println("Best result:\t\t" + bestCost);
		System.out.println("Worst result:\t\t" + worstCost);
	}
	
	public static void sweep(int runsPerGraph, float stopTemp, boolean allowShiftsGr1, List<String> graphBlacklist) {
		System.out.println("\n\n\nDoing sweep over all graphs with " + runsPerGraph + " runs per graph.\nAllowShiftsGr1 = " + allowShiftsGr1 + "\n\n");
		
		File folder = new File("graphs");
		File[] listOfFiles = folder.listFiles();
		List<String> graphFiles = new ArrayList<String>(listOfFiles.length);
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String graphName = listOfFiles[i].getName().substring(0, listOfFiles[i].getName().lastIndexOf(".dot"));
				System.out.println("Found file\t" + listOfFiles[i].getName());
				boolean blacklisted = false;
				for (String item : graphBlacklist) {
					if (item.equals(graphName)) {
						System.out.println("\tGraph is on blacklist and will not be included in the sweep.");
						blacklisted = true;
						break;
					}
				}
				if (!blacklisted) {
					graphFiles.add(graphName);
				}
			}
		}
		System.out.println("\n\n\n");
		
		long startTime = System.currentTimeMillis();
		
		try {
			String dateTime = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(Calendar.getInstance().getTime());
			BufferedWriter writer = new BufferedWriter(new FileWriter("results/SweepResults_" + dateTime + ".csv"));
			
			writer.write("Graph,Size,Loose nodes,Loose node shift max,Allow shifts >1,Stop temperature,"
					+ "Run,Initial temperature,Runtime,"
					+ "Initial II,Initial shift sum,Best II,Best shift sum,"
					+ "Initial shift max,Initial cost,Best shift max,Best cost,"
					+ "SA II,SA shift sum,SA shift max,SA cost,"
					+ "Worst II,Worst shift sum,Worst shift max,Worst cost\n");
			
			int graphCounter = 1;
			StringBuilder skippedGraphs = new StringBuilder().append("\nSkipped graphs:\n");
			
			for (String graphName : graphFiles) {
				Graph graph = new Dot_reader(true).parse("graphs/" + graphName + ".dot");
				SARetiming sa = new SARetiming(graph);
				sa.setStopTemp(stopTemp);
				sa.setAllowShiftsGr1(allowShiftsGr1);
				int skipCounter = 0;
				
				for (int run = 1; run <= runsPerGraph; run++) {
					System.out.print(new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()));
					System.out.println(" - Running graph " + graphCounter + "/" + graphFiles.size() + ": " + graphName + ", run " + run + "/" + runsPerGraph);
					try {
						SARetimingResultPackage res = sa.run(0);
						writer.write(compileSweepResultsLine(graphName, run, res, allowShiftsGr1));
					} catch (IllegalArgumentException e) {
						System.err.println("Critical problem while running " + graphName + ". Skipping.");
						skipCounter++;
					}
				}
				
				if (skipCounter > 0) {
					skippedGraphs.append(graphName).append(": ").append(skipCounter).append(" times\n");
				}
				
				graphCounter++;
			}
			
			if (skippedGraphs.length() < 20) {
				skippedGraphs.append("None :-)");
			}
			writer.write(skippedGraphs.toString());
			String time = new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis() - startTime - 3600000));
			writer.write("\n\nSweep duration: " + time);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		String time = new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis() - startTime - 3600000));
		System.out.println("\n\n\nSweep complete.\nIt took " + time + " hours");
	}
	
	private static String compileSweepResultsLine(String graphName, int run, SARetimingResultPackage res, boolean allowShiftsGr1) {
		StringBuilder sb = new StringBuilder();
		String c = ",";
		
		// Graph,Size,Loose nodes,Loose node shift max,Allow shifts >1,Stop temperature
		// Run,Initial temperature,Runtime,
		// Initial II,Initial shift sum,Best II,Best shift sum,
		// Initial shift max,Initial cost,Best shift max,Best cost,
		// SA II,SA shift sum,SA shift max,SA cost,
		// Worst II,Worst shift sum,Worst shift max,Worst cost
		sb.append(graphName)
				.append(c).append(res.graphSize)
				.append(c).append(res.foundLooseNodes)
				.append(c).append(res.looseNodeShiftMax)
				.append(c).append(allowShiftsGr1)
				.append(c).append(res.stopTemp)
				.append(c).append(run)
				.append(c).append(res.initTemp)
				.append(c).append(res.wallclock)
				.append(c).append(res.initII)
				.append(c).append(res.initShiftSum)
				.append(c).append(res.bestII)
				.append(c).append(res.bestShiftSum)
				.append(c).append(res.initShiftMax)
				.append(c).append(res.initCost)
				.append(c).append(res.bestShiftMax)
				.append(c).append(res.bestCost)
				.append(c).append(res.saII)
				.append(c).append(res.saShiftSum)
				.append(c).append(res.saShiftMax)
				.append(c).append(res.saCost)
				.append(c).append(res.worstII)
				.append(c).append(res.worstShiftSum)
				.append(c).append(res.worstShiftMax)
				.append(c).append(res.worstCost)
				.append("\n");
		
		return sb.toString();
	}
	
}