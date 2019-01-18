package scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SARetiming {
	
	// maximum change for the index shift of a node with wither no predecessors or successors (so can be infinitely shifted)
	public static final int LOOSE_NODE_SHIFT_MAX = 4;
	
	private final Graph initGraph; 
	private Graph graph;
	private float initTemp;
	private float stopTemp;
	private float temp;
	private int innerLoopIterations;
	
	public SARetiming(Graph graph) {
		initGraph = graph;
	}
	
	public Graph run(float initTemp, float stopTemp, int innerNum) {
		setSAParams(initTemp, stopTemp, innerNum);
		return run();
	}
	public Graph run() {
		graph = initGraph;
		temp = initTemp;
		
		while (temp > stopTemp) {
			int acceptedChanges = 0;
			System.out.println("\tDoing " + innerLoopIterations + " loops with temperature " + temp);
			
			for (int cntInner = 0; cntInner < innerLoopIterations; cntInner++) {
				//System.out.println("\t\t" + cntInner);
				Graph candidate = doRandomMove();
				float deltaCost = getGraphCost(candidate) - getGraphCost(graph);
				double r = Math.random();
				if (r < Math.exp(-deltaCost / temp)) {
					// accept candidate
					graph = candidate;
					acceptedChanges++;
				}
			}
			updateTemp((float) acceptedChanges / (float) innerLoopIterations);
		}
		System.out.println("Finished with temperature " + temp + " < " + stopTemp + "\n\n\n");
		
		return new Graph();
	}
	
	void updateTemp(float alpha) {
		float y = 0.8f;
		if (alpha > 0.96f) {
			y = 0.5f;
		} else if (alpha > 0.8f) {
			y = 0.9f;
		} else if (alpha > 0.15f) {
			y = 0.95f;
		}
		
		System.out.println("\t\tChange acceptance rate alpha is " + alpha + ". T *= " + y);
		temp *= y;
	}
	
	public void setSAParams(float initTemp, float stopTemp, int innerNum) {
		this.initTemp = initTemp;
		this.stopTemp = stopTemp;
		innerLoopIterations = (int) Math.round(innerNum * Math.pow(initGraph.size(), 4./3.));
	}
	
	
	private Graph doRandomMove() {
		Graph result = graph.clone();
		
		List<RetimingMove> possibleMoves = getPossibleMoves(result);
		int moveIndex = new Random().nextInt(possibleMoves.size());
		RetimingMove move = possibleMoves.get(moveIndex);
		
		boolean success = move.execute();
		
		return success ? result : null;
	}
	
	private List<RetimingMove> getPossibleMoves(Graph graph) {
		List<RetimingMove> result = new ArrayList<RetimingMove>();
		
		for (Node node : graph) {
			//System.out.println(node.diagnose());
			int minIn = Integer.MAX_VALUE;
			for (Integer weight : node.allPredecessors().values()) {
				//System.out.println("\t\t\tpred weight: " + weight);
				minIn = Math.min(minIn, weight);
			}
			//System.out.println("\t\t\tminIn = " + minIn);
			int minOut = Integer.MAX_VALUE;
			for (Integer weight : node.allSuccessors().values()) {
				//System.out.println("\t\t\tsucc weight: " + weight);
				minOut = Math.min(minOut, weight);
			}
			//System.out.println("\t\t\tminOut = " + minOut);

			if (minIn == Integer.MAX_VALUE) {
				System.err.println("Warning: Found loose node (no predecessors). Using max shift " + LOOSE_NODE_SHIFT_MAX);
				minIn = -LOOSE_NODE_SHIFT_MAX;
			}
			if (minOut == Integer.MAX_VALUE) {
				System.err.println("Warning: Found loose node (no successors). Using max shift " + LOOSE_NODE_SHIFT_MAX);
				minIn = LOOSE_NODE_SHIFT_MAX;
			}
			
			for (int iterShift = -minIn; iterShift <= minOut; iterShift++) {
				result.add(new RetimingMove(node, new Integer(iterShift)));				
			}
		}
		
		return result;
	}
	
	
	public static float getGraphCost(Graph graph) {
		// TODO
		return -1f;
	}
	
	
	private class RetimingMove {
		
		private final Node node;
		private final int iterationShift;
		private boolean wasExecuted;
		
		public RetimingMove(final Node node, final int iterationShift) {
			this.node = node;
			this.iterationShift = iterationShift;
			wasExecuted = false;
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
		
		public boolean execute() {
			if (!isMoveValid(node, iterationShift) || wasExecuted) {
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
			
			wasExecuted = true;
			return true;
		}
		
		public boolean reverse() {
			if (!wasExecuted) {
				return false;
			}
			
			for (Node predecessor : node.allPredecessors().keySet()) {
				Integer oldWeight = node.allPredecessors().get(predecessor).intValue() - iterationShift;
				node.allPredecessors().put(predecessor, oldWeight);
				predecessor.allSuccessors().put(node, oldWeight);
			}
			for (Node successor : node.allSuccessors().keySet()) {
				Integer oldWeight = node.allSuccessors().get(successor).intValue() + iterationShift;
				node.allSuccessors().put(successor, oldWeight);
				successor.allPredecessors().put(node, oldWeight);
			}
			
			wasExecuted = false;
			return true;
		}
		
	}
	
}