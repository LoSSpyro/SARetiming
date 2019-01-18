package scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javafx.scene.control.RadioMenuItem;
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
		List<Pair<Node, Integer>> possibleMoves = getPossibleMoves();
		int moveIndex = new Random().nextInt(possibleMoves.size());
		Pair<Node, Integer> move = possibleMoves.get(moveIndex);
		
		return retimeNode(move);
	}
	
	private List<Pair<Node, Integer>> getPossibleMoves() {
		List<Pair<Node, Integer>> result = new ArrayList<Pair<Node, Integer>>();
		
		for (Node node : graph) {
			int minIn = Integer.MAX_VALUE;
			for (Integer weight : node.allPredecessors().values()) {
				minIn = Math.min(minIn, weight);
			}
			int minOut = Integer.MAX_VALUE;
			for (Integer weight : node.allSuccessors().values()) {
				minOut = Math.min(minOut, weight);
			}
			
			for (int iterShift = -minIn; iterShift <= minOut; iterShift++) {
				result.add(new Pair<Node, Integer>(node, new Integer(iterShift)));				
			}
		}
		
		return result;
	}
	
	private boolean retimeNode(Pair<Node, Integer> move) {
		return retimeNode(move.getKey(), move.getValue());
	}
	private boolean retimeNode(Node node, int iterationShift) {
		if (!isMoveValid(node, iterationShift)) {
			return false;
		}
		
		for (Node predecessor : node.allPredecessors().keySet()) {
			Integer newWeight = node.allPredecessors().get(predecessor).intValue() + iterationShift;
			node.allPredecessors().put(predecessor, newWeight);
			predecessor.allSuccessors().put(node, newWeight);
		}
		for (Node successor : node.allSuccessors().keySet()) {
			Integer newWeight = node.allSuccessors().get(successor).intValue() - iterationShift;
			node.allSuccessors().put(successor, newWeight);
			successor.allPredecessors().put(node, newWeight);
		}
		
		return true;
	}
	
	private boolean isMoveValid(Node node, int iterationShift) {
		for (Node predecessor : node.allPredecessors().keySet()) {
			int newWeight = node.allPredecessors().get(predecessor).intValue() + iterationShift;
			if (newWeight < 0) {
				return false;
			}
		}
		for (Node successor : node.allSuccessors().keySet()) {
			int newWeight = node.allSuccessors().get(successor).intValue() - iterationShift;
			if (newWeight < 0) {
				return false;
			}
		}
		
		return true;
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