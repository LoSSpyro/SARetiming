package scheduler;

import java.util.HashMap;
import java.util.Map;
import javafx.util.Pair;

public class SARetiming {
	
	private final Graph initGraph; 
	private Graph graph;
	private float temp;
	
	public SARetiming(Graph graph, float initTemp) {
		initGraph = graph;
		this.graph = graph;
		temp = initTemp;
	}
	
	public boolean doRandomMove() {
		// TODO
		return false;
	}
	
	public Map<Node, Pair<Integer, Integer>> possibleMoves() {
		// TODO
		return new HashMap<Node, Pair<Integer, Integer>>();
	}
	
	public boolean retimeNode(Node node, int iterationShift) {
		// TODO
		return false;
	}
	
	public boolean exitCriterion() {
		// TODO
		return false;
	}
	
	public boolean decideChange(float oldCost, float newCost) {
		// TODO
		return false;
	}
	
	public float getGraphCost(Graph graph) {
		// TODO
		return -1f;
	}
	
}