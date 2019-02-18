package scheduler;

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
	
}