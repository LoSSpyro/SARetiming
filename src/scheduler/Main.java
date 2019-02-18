package scheduler;

public class Main {

	public static void main(String[] args) {
		RC rc = null;
		if (args.length>1){
			System.out.println("Reading resource constraints from "+args[1]+"\n");
			rc = new RC();
			rc.parse(args[1]);
		}
		
		Dot_reader dr = new Dot_reader(true);
		if (args.length < 1) {
			System.err.printf("Usage: scheduler dotfile%n");
			System.exit(-1);
		} else {
			System.out.println("Reading "+args[0]);
			System.out.println();
		}
		
		Graph g = dr.parse(args[0]);
		//System.out.printf("%s%n", g.diagnose());
		System.out.println("Number of nodes: " + g.size());
		
		//defaultMain(g, args);
		saRetiming(g, args);
	}
	
	public static void saRetiming(Graph graph, String[] args) {
		System.out.println("\n\nRunning SA Retiming\n");
		
		SARetiming sa = new SARetiming(graph);
		sa.setAllowShiftsGr1(true);
		//sa.setSAParams(100, 0.1f, 10);
		
		long startTime = System.currentTimeMillis();
		Graph result = sa.run(false);	// knapp 50% langsamer mit Ausgaben
		result.draw("modGraphs/result_", args[0]);
		float calcTime = (float) (System.currentTimeMillis() - startTime) / 1000f;
		System.out.println("Calculation time: " + calcTime + "s");
		sa.evaluate(args[0], calcTime);
		
		//System.out.println(result.diagnose());
	}
	
	public static void defaultMain(Graph g, String[] args) {
		Scheduler s = new ASAP();
		Schedule sched = s.schedule(g);
		System.out.printf("%nASAP%n%s%n", sched.diagnose());
		System.out.printf("cost = %s%n", sched.cost());
		
		sched.draw("schedules/ASAP_" + args[0].substring(args[0].lastIndexOf("/")+1));
		
		s = new ALAP();
		sched = s.schedule(g);
		System.out.printf("%nALAP%n%s%n", sched.diagnose());
		System.out.printf("cost = %s%n", sched.cost());
		
		sched.draw("schedules/ALAP_" + args[0].substring(args[0].lastIndexOf("/")+1));
	}
	
}