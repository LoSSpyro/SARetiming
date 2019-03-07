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
	// maximum change for the index shift of a node with either no predecessors or successors (so can be infinitely shifted)
	public static final int LOOSE_NODE_SHIFT_MAX = 5;
	
	private static int lastLongestPath, lastShiftSum, lastShiftMax;
	
	private final Graph initGraph;
	private Graph bestGraph;
	private Graph saGraph;
	private float initTemp;
	private float stopTemp;
	private boolean allowShiftsGr1;
	private int innerLoopIterations;
	private boolean foundLooseNodes;
	
	public SARetiming(Graph graph) {
		initGraph = graph;
		innerLoopIterations = (int) Math.round(10 * Math.pow(initGraph.size(), 4./3.));
		allowShiftsGr1 = true;
		foundLooseNodes = false;
		stopTemp = DEFAULT_STOP_TEMP;
	}
	public void setAllowShiftsGr1(boolean allowShiftsGr1) {
		this.allowShiftsGr1 = allowShiftsGr1;
	}
	public void setStopTemp(float stopTemp) {
		this.stopTemp = stopTemp;
	}
	
	public SARetimingResultPackage run(int print) {
		foundLooseNodes = false;
		float oldCost = getGraphCost(initGraph, print);
		float minCost = oldCost;
		Graph bestGraph = initGraph;
		float worstCost = oldCost;
		int worstII = longestZeroWeightedPath(initGraph), worstSum = shiftSum(initGraph), worstMax = shiftMax(initGraph);
		if (print >= 1) {
			System.out.println("Initial Achieved II = " + longestZeroWeightedPath(initGraph) + ". Initial shift sum = " + shiftSum(initGraph));
		}
		
		long startTime = System.currentTimeMillis();
		findInitTemp(print);
		Graph graph = initGraph.clone();
		float temp = initTemp;
		
		while (temp > stopTemp) {
			int acceptedChanges = 0;
			if (print >= 1) {
				System.out.println("\tDoing " + innerLoopIterations + " loops with temperature " + temp);
			}
			
			for (int cntInner = 0; cntInner < innerLoopIterations; cntInner++) {
				RetimingMove move = generateRandomMove(graph, print);
				move.execute();
				float newCost = getGraphCost(graph, print);
				float deltaCost = newCost - oldCost; // < 0: improvement
				double r = Math.random();
				double acceptProb = Math.exp(-1000*deltaCost / temp);
				if (r < acceptProb) {
					// accept candidate
					oldCost = newCost;
					acceptedChanges++;
					if (print >= 2) {
						System.out.println("\t\t\tAccepted move with probability " + (Math.round(100*acceptProb)) + "% (deltaC = " + deltaCost + ")");
					}
					
					if (newCost < minCost) {
						minCost = newCost; 
						bestGraph = graph.clone();
					}
					if (lastLongestPath > worstII) {
						worstII = lastLongestPath;
					}
					if (lastShiftSum > worstSum) {
						worstSum = lastShiftSum;
					}
					if (shiftMax(graph) > worstMax) {
						worstMax = lastShiftMax;
					}
					if (newCost > worstCost) {
						worstCost = newCost;
					}
				} else if (print >= 2) {
					move.reverse();
					System.out.println("\t\t\tRejected move. r = " + r + " !< accProb = " + acceptProb);
				}
			}
			float alpha = (float) acceptedChanges / (float) innerLoopIterations;
			temp = updateTemp(temp, alpha, print);
			if (print >= 1) {
				System.out.println("\t\tCurrent Achieved II = " + longestZeroWeightedPath(graph) + ". ShiftSum = " + shiftSum(graph));
			}
		}
		long wallclock = System.currentTimeMillis() - startTime;
		
		this.bestGraph = bestGraph;
		this.saGraph = graph;
		
		if (print >= 1) {
			System.out.println("\n\nFinished with temperature:\t" + temp + " < " + stopTemp);
			System.out.println("Initial temperature:\t\t" + initTemp + "\n");
		}
		
		
		return new SARetimingResultPackage(bestGraph, foundLooseNodes, LOOSE_NODE_SHIFT_MAX,
				wallclock, initTemp, stopTemp,
				longestZeroWeightedPath(initGraph), worstII, longestZeroWeightedPath(graph), longestZeroWeightedPath(bestGraph),
				shiftSum(initGraph), worstSum, shiftSum(graph), shiftSum(bestGraph),
				shiftMax(initGraph), worstMax, shiftMax(graph), shiftMax(bestGraph),
				getGraphCost(initGraph), worstCost, getGraphCost(graph), getGraphCost(bestGraph));
	}
	
	private void findInitTemp(int print) {
		float initTemp;
		Graph graph = initGraph.clone();
		
		int tries = 0;
		while (true) {
			int n = initGraph.size();
			float[] costs = new float[n];
			float average = 0f;
			for (int i = 0; i < n; i++) {
				RetimingMove move = generateRandomMove(graph, print);
				move.execute();
				float cost = getGraphCost(graph, print);
				move.reverse();
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
				if (print >= 1) {
					System.out.println("Initial temperature determined as " + initTemp);
				}
				break;
			}
			if (print >= 1) {
				System.err.print("Calculated initital temperature of " + initTemp + ".");
			}
			if (tries++ < 5) {
				initTemp = 5f;
				if (print >= 1) {
					System.err.println("Using default initial temperature " + initTemp);
				}
				break;
			}
			if (print >= 1) {
				System.err.println("Trying again...");
			}
		}
		
		this.initTemp = initTemp;
	}
	
	private float updateTemp(float temp, float alpha, int print) {
		float y = 0.8f;
		if (alpha > 0.96f) {
			y = 0.5f;
		} else if (alpha > 0.8f) {
			y = 0.9f;
		} else if (alpha > 0.15f) {
			y = 0.95f;
		}
		
		if (print >= 1) {
			System.out.println("\t\tChange acceptance rate alpha is " + alpha + ". T *= " + y);
		}
		return temp * y;
	}
	
	
	private RetimingMove generateRandomMove(Graph graph, int print) {
		List<RetimingMove> possibleMoves = getPossibleMoves(graph);
		int moveIndex = new Random().nextInt(possibleMoves.size());
		RetimingMove move = possibleMoves.get(moveIndex);
		
		if (print >= 2) {
			System.out.println("\t\tGenerated random " + move);
		}
		
		return move;
	}
	
	private List<RetimingMove> getPossibleMoves(Graph graph) {
		List<RetimingMove> result = new ArrayList<RetimingMove>();
		
		for (Node node : graph) {
			int minIn = Integer.MAX_VALUE;
			int maxIn = Integer.MIN_VALUE;
			for (Integer weight : node.allPredecessors().values()) {
				if (weight < minIn) {
					minIn = weight;
				}
				if (weight > maxIn) {
					maxIn = weight;
				}
			}
			int minOut = Integer.MAX_VALUE;
			int maxOut = Integer.MIN_VALUE;
			for (Integer weight : node.allSuccessors().values()) {
				if (weight < minOut) {
					minOut = weight;
				}
				if (weight > maxOut) {
					maxOut = weight;
				}
			}

			if (minIn == Integer.MAX_VALUE || maxIn == Integer.MIN_VALUE) {
				// no incoming edges
				foundLooseNodes = true;
				if (allowShiftsGr1) {
					minIn = LOOSE_NODE_SHIFT_MAX;
				} else {
					minIn = minOut;
					maxIn = -minOut;
				}
			}
			if (minOut == Integer.MAX_VALUE || maxOut == Integer.MIN_VALUE) {
				// no outgoing edges
				foundLooseNodes = true;
				if (allowShiftsGr1) {
					minOut = LOOSE_NODE_SHIFT_MAX;
				} else {
					minOut = minIn;
					maxOut = -minIn;
				}
			}
			int oldMaxShift = maxIn > maxOut ? maxIn : maxOut;
			
			for (int iterShift = -minIn; iterShift <= minOut; iterShift++) {
				int newMaxIn = maxIn + iterShift;
				int newMaxOut = maxOut - iterShift;
				int newMaxShift = newMaxIn > newMaxOut ? newMaxIn : newMaxOut;
				
				if (iterShift == 0) {
					continue;
				}
				if (!allowShiftsGr1 && (newMaxShift > oldMaxShift && newMaxShift > 1)) {
					// reject moves that worsen the maxShift AND result in a maxShift > 1
					// moves that improve the maxShift (of the node in question) OR have a maxShift of 1 are accepted
					// moves that don't change the maxShift are accepted as well
					continue;
				}
				result.add(new RetimingMove(node, new Integer(iterShift)));
			}
		}
		
		return result;
	}
	
	
	public static float getGraphCost(Graph graph) {
		return getGraphCost(graph, 0);
	}
	public static float getGraphCost(Graph graph, int print) {
		float achievedII = longestZeroWeightedPath(graph);
		float shiftSum = shiftSum(graph);
		float weightedShiftSum = (float) (1 - Math.exp(-shiftSum/1000000f));
		if (weightedShiftSum >= 1f) {
			System.err.println("Warning: shift sum too big for scaling factor");
			throw new RuntimeException();
		}
		float cost = achievedII + weightedShiftSum;
		if (print >= 2) {
			//System.out.println("\t\t\tweightedShiftSum = " + weightedShiftSum);
			System.out.println("\t\t\tCost = " + cost);
		}
		return cost;
	}
	
	public static int longestZeroWeightedPath(Graph graph) {
		int result = 0;
		Map<Node, Integer> visited = new HashMap<Node, Integer>((int) (graph.size() * 1.5f));
		for (Node node : graph) {
			int lengthFromNode = longestPathFromNode(node, visited); 
			if (lengthFromNode > result) {
				result = lengthFromNode;
			}
		}
		lastLongestPath = result;
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
		lastShiftSum = result;
		return result;
	}
	public static int shiftMax(Graph graph) {
		int result = 0;
		for (Node node : graph) {
			for (Node successor : node.allSuccessors().keySet()) {
				result = Math.max(result, node.allSuccessors().get(successor));
			}
		}
		lastShiftMax = result;
		return result;
	}
	
	public void evaluate(String filename, float calcTime) {
		try {
			int initGraphII, bestGraphII, saGraphII;
			
			initGraphII = longestZeroWeightedPath(initGraph);
			bestGraphII = longestZeroWeightedPath(bestGraph);
			saGraphII = longestZeroWeightedPath(saGraph);
			
			String[] arguments = filename.split("/");
			String file = arguments[arguments.length-1];
			
			BufferedWriter result_file = new BufferedWriter(new FileWriter("results/values.txt", true));
			result_file.write(file + "\t" + initGraphII + "\t" + bestGraphII + "\t" + saGraphII + "\t" + calcTime);
			
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
				System.err.println("Warning: Move not valid or already executed");
				return false;
			}
			
			for (Node predecessor : node.allPredecessors().keySet()) {
				Integer newWeight = node.allPredecessors().get(predecessor).intValue() + iterationShift;
				node.prepend(predecessor, newWeight);
				predecessor.append(node, newWeight);
			}
			for (Node successor : node.allSuccessors().keySet()) {
				Integer newWeight = node.allSuccessors().get(successor).intValue() - iterationShift;
				node.append(successor, newWeight);
				successor.prepend(node, newWeight);
			}
			
			wasExecuted = true;
			return true;
		}
		
		public boolean reverse() {
			if (!wasExecuted) {
				System.err.println("Warning: Move has not yet been executed, couldn't be reversed");
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
	
	public class SARetimingResultPackage {
		
		public final Graph graph;
		public final int graphSize;
		public final boolean foundLooseNodes;
		public final int looseNodeShiftMax;
		
		public long wallclock;
		public final float initTemp;
		public final float stopTemp;
		
		public final int initII, worstII, saII, bestII;
		public final int initShiftSum, worstShiftSum, saShiftSum, bestShiftSum;
		public final int initShiftMax, worstShiftMax, saShiftMax, bestShiftMax;
		public final float initCost, worstCost, saCost, bestCost;
		
		public SARetimingResultPackage (Graph graph, boolean foundLooseNodes, int looseNodeShiftMax,
				long wallclock, float initTemp, float stopTemp,
				int initII, int worstII, int saII, int bestII,
				int initShiftSum, int worstShiftSum, int saShiftSum, int bestShiftSum,
				int initShiftMax, int worstShiftMax, int saShiftMax, int bestShiftMax,
				float initCost, float worstCost, float saCost, float bestCost) {
			this.graph = graph;
			this.graphSize = graph.size();
			this.foundLooseNodes = foundLooseNodes;
			this.looseNodeShiftMax = looseNodeShiftMax;
			
			this.wallclock = wallclock;
			this.initTemp = initTemp;
			this.stopTemp = stopTemp;
			
			this.initII = initII;
			this.worstII = worstII;
			this.saII = saII;
			this.bestII = bestII;
			this.initShiftSum = initShiftSum;
			this.worstShiftSum = worstShiftSum;
			this.saShiftSum = saShiftSum;
			this.bestShiftSum = bestShiftSum;
			this.initShiftMax = initShiftMax;
			this.worstShiftMax = worstShiftMax;
			this.saShiftMax = saShiftMax;
			this.bestShiftMax = bestShiftMax;
			this.initCost = initCost;
			this.worstCost = worstCost;
			this.saCost = saCost;
			this.bestCost = bestCost;
		}
		
		public void printDiagnose() {
			System.out.println("\n\tAchvdII\tShftSum\tShftMax\tCost");
			System.out.println("Initial\t" + initII + "\t" + initShiftSum + "\t" + initShiftMax + "\t" + initCost);
			System.out.println("Worst\t" + worstII + "\t" + worstShiftSum + "\t" + worstShiftMax + "\t" + worstCost);
			System.out.println("SAFinal\t" + saII + "\t" + saShiftSum + "\t" + saShiftMax + "\t" + saCost);
			System.out.println("Best\t" + bestII + "\t" + bestShiftSum + "\t" + bestShiftMax + "\t" + bestCost);
			System.out.println("\nWorst values do not necessarily come from the same graph.\n");
			
			if (graph.size() < 200) {
				System.out.println("MinII:\t" + minII.getMinII(graph, false));
			}
			if (bestII < saII) {
				System.err.println("Caution: the best found solution wasn't delivered by SA!");
			}
			if (foundLooseNodes) {
				System.err.println("Warning: Found loose node (no predecessors and/or successors). Used max shift " + LOOSE_NODE_SHIFT_MAX);
			}
			System.out.println("\nCalculation time: " + ((float) wallclock / 1000f) + "s");
			System.out.println("\n\n\n");
		}
		
	}
	
}