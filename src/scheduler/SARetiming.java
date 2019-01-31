package scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SARetiming {
	
	public static final float DEFAULT_STOP_TEMP = 1f;
	// maximum change for the index shift of a node with wither no predecessors or successors (so can be infinitely shifted)
	public static final int LOOSE_NODE_SHIFT_MAX = 5;
	
	private final Graph initGraph; 
	private Graph graph;
	private float initTemp;
	private float stopTemp;
	private float temp;
	private int innerLoopIterations;
	private boolean foundLooseNodes;
	
	public SARetiming(Graph graph) {
		initGraph = graph;
		this.graph = graph;
		initTemp = findInitTemp();
		stopTemp = DEFAULT_STOP_TEMP;
		innerLoopIterations = (int) Math.round(10 * Math.pow(initGraph.size(), 4./3.));
		foundLooseNodes = false;
	}
	
	public Graph run(float initTemp, float stopTemp, int innerNum) {
		setSAParams(initTemp, stopTemp, innerNum);
		return run();
	}
	public Graph run() {
		graph = initGraph;
		float oldCost = getGraphCost(graph);
		temp = initTemp;
		foundLooseNodes = false;
		float initCost = oldCost;
		float minCost = oldCost;
		
		while (temp > stopTemp) {
			int acceptedChanges = 0;
			System.out.println("\tDoing " + innerLoopIterations + " loops with temperature " + temp);
			
			for (int cntInner = 0; cntInner < innerLoopIterations; cntInner++) {
				//System.out.println("\t\t" + cntInner);
				Graph candidate = doRandomMove();
				float newCost = getGraphCost(candidate);
				if (newCost < minCost) {
					minCost = newCost;
				}
				float deltaCost = newCost - oldCost; // < 0: improvement
				double r = Math.random();
				if (r < Math.exp(-deltaCost / temp)) {
					// accept candidate
					graph = candidate;
					oldCost = newCost;
					acceptedChanges++;
					System.out.println("\t\t\tAccepted move with probability " + (Math.round(100*Math.exp(-deltaCost / temp))) + "% (deltaC = " + deltaCost + ")");
				} else {
					System.out.println("\t\t\tRejected move");
				}
			}
			updateTemp((float) acceptedChanges / (float) innerLoopIterations);
		}
		System.out.println("\n\nFinished with temperature " + temp + " < " + stopTemp);
		System.out.println("Initial temperature was " + initTemp);
		System.out.println("Initial cost was " + initCost);
		System.out.println("MinII is " + minII.getMinII(graph, true));
		System.out.println("Final cost is " + oldCost);
		System.out.println("Minimal found cost was " + minCost);
		if (minCost < oldCost) {
			System.err.println("Caution: the best found solution wasn't delivered!");
		}
		if (foundLooseNodes) {
			System.err.println("Warning: Found loose node (no predecessors and/or successors). Used max shift " + LOOSE_NODE_SHIFT_MAX);
		}
		System.out.println("\n\n\n");
		
		return graph;
	}
	
	private float findInitTemp() {
		graph = initGraph;
		for (int tries = 0; tries < 5; tries++) {
			int n = initGraph.size();
			float[] costs = new float[n];
			float average = 0f;
			for (int i = 0; i < n; i++) {
				graph = doRandomMove();
				float cost = getGraphCost(graph);
				costs[i] = cost;
				average += cost;
			}
			average /= (float) n;
			float standardDeviation = 0;
			for (int i = 0; i < n; i++) {
				float diffAv = costs[i] - average;
				standardDeviation += diffAv * diffAv;
			}
			standardDeviation /= (float) n;
			initTemp = 20*standardDeviation;
			
			if (initTemp > 1) {
				break;
			}
			System.err.println("Calculated initital temperature of " + initTemp + ". Trying again...");
		}
		
		System.out.println("Initial temperature determined as " + initTemp);
		return initTemp;
	}
	
	private void updateTemp(float alpha) {
		float y = 0.8f;
		if (alpha > 0.96f) {
			y = 0.5f;
		} else if (alpha > 0.8f) {
			y = 0.9f;
		} else if (alpha > 0.15f) {
			y = 0.95f;
		}
		
		System.out.println("\tChange acceptance rate alpha is " + alpha + ". T *= " + y);
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
		
		System.out.println("\t\tExecuting random " + move);
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
				foundLooseNodes = true;
				minIn = -LOOSE_NODE_SHIFT_MAX;
			}
			if (minOut == Integer.MAX_VALUE) {
				foundLooseNodes = true;
				minOut = LOOSE_NODE_SHIFT_MAX;
			}
			
			for (int iterShift = -minIn; iterShift <= minOut; iterShift++) {
				if (iterShift == 0) {
					continue;
				}
				result.add(new RetimingMove(node, new Integer(iterShift)));
			}
		}
		
		return result;
	}
	
	
	public static float getGraphCost(Graph graph) {
		// TODO
		float achievedII = longestZeroWeightedPath(graph);
		float shiftSum = shiftSum(graph);
		float weightedShiftSum = (float) (1 - Math.exp(-shiftSum/100f));
		float cost = achievedII + weightedShiftSum;
		System.out.println("\t\t\tCost = " + cost);
		System.out.println("weightedShiftSum = " + weightedShiftSum);
		return cost;
	}
	
	public static int longestZeroWeightedPath(Graph graph) {
		int result = 0;
		for (Node node : graph) {
			int recursiveResult = longestPathFromNode(node);
			if (recursiveResult > result) {
				result = recursiveResult;
			}
		}
		return result;
	}
	private static int longestPathFromNode(Node node) {
		//System.out.println("\t\t\t\tLPFN: Node " + node.id + " - entry");
		int result = 0;
		for (Node successor : node.successors()) {
			int recursiveResult = longestPathFromNode(successor);
			if (recursiveResult > result) {
				result = recursiveResult;
			}
		}
		//System.out.println("\t\t\t\tLPFN: Path to node " + node + " has length " + (result + node.getDelay()));
		return result + node.getDelay();
	}
	
	private static int shiftSum(Graph graph) {
		int result = 0;
		for (Node node : graph) {
			for (Node successor : node.allSuccessors().keySet()) {
				result += node.allSuccessors().get(successor);
			}
		}
		return result;
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
				System.err.println("Warning: Move wasn't executed");
				return false;
			}
			
			for (Node predecessor : node.allPredecessors().keySet()) {
				Integer newWeight = node.allPredecessors().get(predecessor).intValue() + iterationShift;
				//System.out.println("\t\tSetting new weight " + newWeight + " for edge between " + predecessor + " and " + node + " (old weight: " + node.allPredecessors().get(predecessor).intValue() + ")");
				node.prepend(predecessor, newWeight);
				predecessor.append(node, newWeight);
			}
			for (Node successor : node.allSuccessors().keySet()) {
				Integer newWeight = node.allSuccessors().get(successor).intValue() - iterationShift;
				//System.out.println("\t\tSetting new weight " + newWeight + " for edge between " + node + " and " + successor + " (old weight: " + node.allSuccessors().get(successor).intValue() + ")");
				node.append(successor, newWeight);
				successor.prepend(node, newWeight);
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
		
		public String toString() {
			return "RetimingMove: Node " + node.id + ", shift " + iterationShift;
		}
		
	}
	
}