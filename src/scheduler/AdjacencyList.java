package scheduler;

import java.util.HashMap;
import java.util.Vector;

public class AdjacencyList {
	
	public static int[][] getAdjacencyList(HashMap<Node, Integer> nodeToInteger) {
		int size = nodeToInteger.size();
		boolean [][] adjacencyMatrix = new boolean[size][size];
		for (Node nd : nodeToInteger.keySet()) {
			for (Node succ : nd.allSuccessors().keySet()) {
				adjacencyMatrix[nodeToInteger.get(nd)][nodeToInteger.get(succ)] = true;
			}
		}
		
		
		int[][] list = new int[adjacencyMatrix.length][];

		for (int i = 0; i < adjacencyMatrix.length; i++) {
			Vector<Integer> v = new Vector<Integer>();
			for (int j = 0; j < adjacencyMatrix[i].length; j++) {
				if (adjacencyMatrix[i][j]) {
					v.add(new Integer(j));
				}
			}

			list[i] = new int[v.size()];
			for (int j = 0; j < v.size(); j++) {
				Integer in = (Integer) v.get(j);
				list[i][j] = in.intValue();
			}
		}
		
		return list;
	}
}
