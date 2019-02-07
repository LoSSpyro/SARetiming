package scheduler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SARetiming {
	
	public static final float DEFAULT_STOP_TEMP = .5f;
	// maximum change for the index shift of a node with wither no predecessors or successors (so can be infinitely shifted)
	public static final int LOOSE_NODE_SHIFT_MAX = 5;
	
	private final Graph initGraph; 
	private Graph graph;
	private float initTemp;
	private float stopTemp;
	private float temp;
	private int innerLoopIterations;
	private boolean foundLooseNodes;
	
	private Graph bestGraph;
	private Graph finalGraph;
	
	public SARetiming(Graph graph) {
		initGraph = graph;
		this.graph = graph;
		initTemp = findInitTemp(false);
		stopTemp = DEFAULT_STOP_TEMP;
		innerLoopIterations = (int) Math.round(10 * Math.pow(initGraph.size(), 4./3.));
		foundLooseNodes = false;
	}
	
	public Graph run(float initTemp, float stopTemp, int innerNum, boolean print) {
		setSAParams(initTemp, stopTemp, innerNum);
		return run(print);
	}
	public Graph run(boolean print) {
		graph = initGraph;
		float oldCost = getGraphCost(graph, print);
		temp = initTemp;
		foundLooseNodes = false;
		float initCost = oldCost;
		float minCost = oldCost;
		Graph bestGraph = initGraph;
		System.out.println("Initial cost is " + initCost);
		
		while (temp > stopTemp) {
			int acceptedChanges = 0;
			System.out.println("\tDoing " + innerLoopIterations + " loops with temperature " + temp);
			
			for (int cntInner = 0; cntInner < innerLoopIterations; cntInner++) {
				//System.out.println("\t\t" + cntInner);
				Graph candidate = doRandomMove(print);
				float newCost = getGraphCost(candidate, print);
				if (newCost < minCost) {
					minCost = newCost;
					bestGraph = candidate;
				}
				float deltaCost = newCost - oldCost; // < 0: improvement
				float r = (float) Math.random();
				if (r < Math.exp(-1000*deltaCost / temp)) {
					// accept candidate
					graph = candidate;
					oldCost = newCost;
					acceptedChanges++;
					if (print) {
						System.out.println("\t\t\tAccepted move with probability " + (Math.round(100*Math.exp(-deltaCost / temp))) + "% (deltaC = " + deltaCost + ")");
					}
				} else {
					if (print) {
						System.out.println("\t\t\tRejected move");
					}
				}
			}
			float alpha = (float) acceptedChanges / (float) innerLoopIterations;
			updateTemp(alpha, print);
			System.out.println("\tCurrent cost = " + oldCost);
		}
		System.out.println("\n\nFinished with temperature " + temp + " < " + stopTemp);
		System.out.println("Initial temperature was " + initTemp);
		System.out.println("Initial cost was " + initCost);
		System.out.println("MinII is " + minII.getMinII(graph, false));
		System.out.println("Minimal found cost is " + minCost);
		System.out.println("Final SA cost is " + oldCost);
		if (Math.floor(minCost) < Math.floor(oldCost)) {
			System.err.println("Caution: the best found solution wasn't delivered by SA!");
		}
		System.out.println("Shift sum for best solution: " + shiftSum(bestGraph));
		System.out.println("Shift sum for SA solution: " + shiftSum(graph));
		System.out.println("Shift max for best solution: " + shiftMax(bestGraph));
		System.out.println("Shift max for SA solution: " + shiftMax(graph));
		if (foundLooseNodes) {
			System.err.println("Warning: Found loose node (no predecessors and/or successors). Used max shift " + LOOSE_NODE_SHIFT_MAX);
		}
		System.out.println("\n\n\n");
		
		finalGraph = graph;
		this.bestGraph = bestGraph;
		return graph;
	}
	
	private float findInitTemp(boolean print) {
		graph = initGraph;
		for (int tries = 0; tries < 5; tries++) {
			int n = initGraph.size();
			float[] costs = new float[n];
			float average = 0f;
			for (int i = 0; i < n; i++) {
				graph = doRandomMove(print);
				float cost = getGraphCost(graph, print);
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
			
			if (initTemp > 2) {
				break;
			}
			System.err.println("Calculated initital temperature of " + initTemp + ". Trying again...");
		}
		
		System.out.println("Initial temperature determined as " + initTemp);
		return initTemp;
	}
	
	private void updateTemp(float alpha, boolean print) {
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
	
	
	private Graph doRandomMove(boolean print) {
		Graph result = graph.clone();
		
		List<RetimingMove> possibleMoves = getPossibleMoves(result);
		int moveIndex = new Random().nextInt(possibleMoves.size());
		RetimingMove move = possibleMoves.get(moveIndex);
		
		if (print) {
			System.out.println("\t\tExecuting random " + move);
		}
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
	
	
	public static float getGraphCost(Graph graph, boolean print) {
		// TODO
		float achievedII = longestZeroWeightedPath(graph);
		float shiftSum = shiftSum(graph);
		float weightedShiftSum = (float) (1 - Math.exp(-shiftSum/1000f));
		if (weightedShiftSum >= 1f) {
			System.err.println("Warning: shift sum to big for scaling factor");
			System.exit(1);
		}
		float cost = achievedII + weightedShiftSum;
		if (print) {
			//System.out.println("\t\t\tweightedShiftSum = " + weightedShiftSum);
			System.out.println("\t\t\tCost = " + cost);
		}
		return cost;
	}
	
	public static int longestZeroWeightedPath(Graph graph) {
		int result = 0;
		Map<Node, Integer> visited = new HashMap<Node, Integer>();
		for (Node node : graph) {
			int lengthFromNode = longestPathFromNode(node, visited); 
			if (lengthFromNode > result) {
				result = lengthFromNode;
			}
		}
		return result;
	}
	private static int longestPathFromNode(Node node, Map<Node, Integer> visited) {
		if (visited.containsKey(node)) {
			return visited.get(node);
		}
		int result = 0;
		for (Node successor : node.successors()) {
			int lengthFromNode = longestPathFromNode(successor, visited);
			if (lengthFromNode > result) {
				result = lengthFromNode;
			}
		}
		visited.put(node, result);
		return result + node.getDelay();
	}
	
	public static int shiftSum(Graph graph) {
		int result = 0;
		for (Node node : graph) {
			for (Node successor : node.allSuccessors().keySet()) {
				result += node.allSuccessors().get(successor);
			}
		}
		return result;
	}
	public static int shiftMax(Graph graph) {
		int result = 0;
		for (Node node : graph) {
			for (Node successor : node.allSuccessors().keySet()) {
				result = Math.max(result, node.allSuccessors().get(successor));
			}
		}
		return result;
	}
	
	public void evaluate(String filename, float calcTime) {
		try {
			int initGraphII, bestGraphII, finalGraphII;
			
			initGraphII = longestZeroWeightedPath(initGraph);
			bestGraphII = longestZeroWeightedPath(bestGraph);
			finalGraphII = longestZeroWeightedPath(finalGraph);
			
			String[] arguments = filename.split("/");
			String file = arguments[arguments.length-1];
			
			BufferedWriter result_file = new BufferedWriter(new FileWriter("results/values.txt", true));
			result_file.write(file + "\t" + initGraphII + "\t" + bestGraphII + "\t" + finalGraphII + "\t" + calcTime);
			
			result_file.write("\n");
			result_file.flush();
			result_file.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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