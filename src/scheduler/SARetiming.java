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
	
	public Graph run() {
		// TODO
		return new Graph();
	}
	
	
	private boolean doRandomMove() {
		// TODO
		return false;
	}
	
	private Map<Node, Pair<Integer, Integer>> possibleMoves() {
		// TODO
		return new HashMap<Node, Pair<Integer, Integer>>();
	}
	
	private boolean retimeNode(Node node, int iterationShift) {
		// TODO
		return false;
	}
	
	private boolean exitCriterion() {
		// TODO
		return false;
	}
	
	private boolean decideChange(float oldCost, float newCost) {
		// TODO
		return false;
	}
	
	
	public static float getGraphCost(Graph graph) {
		// TODO
		return -1f;
	}
	
}