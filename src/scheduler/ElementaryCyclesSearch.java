package scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class ElementaryCyclesSearch {
	private List<List<Node>> cycles = null;
	
	private int[][] adjList = null;;
	
	private boolean[] blocked = null;
	
	private Vector<Integer>[] B = null;
	
	private Vector<Integer> stack = null;
	private HashMap<Node, Integer> nodeToInteger;
	private HashMap<Integer, Node> integerToNode;

	public ElementaryCyclesSearch(Graph g) {
		
		nodeToInteger = new HashMap<Node, Integer>();
		integerToNode = new HashMap<Integer, Node>();
		int i = 0;
		for (Node nd : g) {
			nodeToInteger.put(nd, i);
			integerToNode.put(i, nd);
			i++;
		}
		this.adjList = AdjacencyList.getAdjacencyList(nodeToInteger);
	}
	
	@SuppressWarnings("unchecked")
	public List<List<Node>> getElementaryCycles() {
		this.cycles = new Vector<List<Node>>();
		this.blocked = new boolean[this.adjList.length];
		this.B = new Vector[this.adjList.length];
		this.stack = new Vector<Integer>();
		SCC sccs = new SCC(this.adjList);
		int s = 0;
		
		while(true) {
			SCCResult sccResult = sccs.getAdjacencyList(s);
			if (sccResult != null && sccResult.getAdjList() != null) {
				Vector<Integer>[] scc = sccResult.getAdjList();
				s = sccResult.getLowestNodeId();
				for (int j = 0; j < scc.length; j++) {
					if ((scc[j] != null) && (scc[j].size() > 0)) {
						this.blocked[j] = false;
						this.B[j] = new Vector<Integer>();
					}
				}
				
				this.findCycles(s, s, scc);
				s++;
			} else {
				break;
			}
		}
		
		return this.cycles;
	}

	private boolean findCycles(int v, int s, Vector<Integer>[] adjList) {
		boolean f = false;
		this.stack.add(new Integer(v));
		this.blocked[v] = true;
		
		for (int i = 0; i < adjList[v].size(); i++) {
			int w = ((Integer) adjList[v].get(i)).intValue();
			//found cycle
			if (w == s) {
				Vector<Node> cycle = new Vector<Node>();
				for (int j = 0; j < this.stack.size(); j++) {
					int index = this.stack.get(j).intValue();
					cycle.add(this.integerToNode.get(index));
				}
				this.cycles.add(cycle);
				f=true;
			} else if(!this.blocked[w]) {
				if (this.findCycles(w, s, adjList)) {
					f = true;
				}
			}
		}
		
		if(f) {
			this.unblock(v);
		} else {
			for (int i = 0; i < adjList[v].size(); i++) {
				int w = ((Integer) adjList[v].get(i)).intValue();
				if (!this.B[w].contains(new Integer(v))) {
					this.B[w].add(new Integer(v));
				}
			}
		}
		this.stack.remove(new Integer(v));
		return f;
	}

	private void unblock(int node) {
		this.blocked[node] = false;
		Vector<Integer> Bnode = this.B[node];
		while (Bnode.size() > 0) {
			Integer w = Bnode.get(0);
			Bnode.remove(0);
			if (this.blocked[w.intValue()]) {
				this.unblock(w.intValue());
			}
		}
	}
}
