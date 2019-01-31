package scheduler;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class minII {

	public static Integer getMinII(Graph g, boolean print) {
		ElementaryCyclesSearch ecs = new ElementaryCyclesSearch(g);
		List<List<Node>> cycles = ecs.getElementaryCycles();
		
		for (Node nd : g) {
			if (nd.containsItSelf()) {
				List<Node> cycle = new Vector<Node>();
				cycle.add(nd);
				cycles.add(cycle);
			}
		}
		if (print) {
			System.out.println("Found following cycles:");
			System.out.println("------------------------");
			for (int i = 0; i < cycles.size(); i++) {
				List<Node> cycle = cycles.get(i);
				for (int j = 0; j < cycle.size(); j++) {
					Node node = cycle.get(j);
					if (j < cycle.size() - 1) {
						System.out.print(node + " -> ");
					} else {
						System.out.println(node);
					}
				}
				System.out.print("\n");
			}
			System.out.println("------------------------");
		}
		
		Vector<Integer> IIs = new Vector<Integer>();;
		for (int i = 0; i < cycles.size(); i++) {
			float sum = 0f;
			int iterationShifts = 0;
			Node predNode = null;
			Node startNode = null;
			for (Node nd : cycles.get(i)) {
				sum += nd.getDelay();
				
				if (predNode != null)
					iterationShifts += predNode.getSuccWeight(nd);
				else
					startNode = nd;
				
				predNode = nd;
			}
			iterationShifts += predNode.getSuccWeight(startNode);
			Integer result = (int) Math.ceil(sum/(float)iterationShifts);
			IIs.add(result);
		}
		return Collections.max(IIs);
	}
}
