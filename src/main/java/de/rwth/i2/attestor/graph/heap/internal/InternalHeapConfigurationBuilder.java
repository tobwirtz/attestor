package de.rwth.i2.attestor.graph.heap.internal;

import de.rwth.i2.attestor.graph.Nonterminal;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.digraph.LabeledDigraph;
import de.rwth.i2.attestor.graph.heap.*;
import de.rwth.i2.attestor.types.Type;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;

/**
 * All the messy details of a {@link HeapConfigurationBuilder} for {@link InternalHeapConfiguration}s. 
 * All identifiers of nodes, nonterminal edges, and variable edges used by InternalHeapConfigurationBuilder
 * are public identifiers.
 * 
 * @author Christoph
 *
 */
public class InternalHeapConfigurationBuilder implements HeapConfigurationBuilder {

	/**
	 * The HeapConfiguration that is manipulated by this InternalHeapConfigurationBuilder.
	 */
	protected InternalHeapConfiguration heapConf;
	
	/**
	 * Creates a new InternalHeapConfigurationBuilder for the provided InternalHeapConfiguration.
	 * Note that an InternalHeapConfigurationBuilder is assumed to be unique for each InternalHeapConfiguration
	 * and should thus only be created in InternalHeapConfiguration.builder().
	 *
	 * @param heapConf The InternalHeapConfiguration that should be changed by this builder.
	 */
    InternalHeapConfigurationBuilder(InternalHeapConfiguration heapConf) {
		
		if(heapConf == null) {
			
			throw new NullPointerException();
		}
		
		this.heapConf = heapConf;
		
	}

	@Override
	public HeapConfiguration build() {
		
		cleanupGraphAndIDs();
		
		// invalidate this builder
		heapConf.builder = null;
		HeapConfiguration result = heapConf;
		heapConf = null;
		
		return result;
	}
	
	/**
	 * Restores a compact graph representation while keeping all public IDs
	 * of elements that have not been deleted unchanged. 
	 */
	private void cleanupGraphAndIDs() {
     	// Swap all deleted elements to the end and remove them from the
		// graph to get a tight representation. 
		// The obtained map stores all performed swaps
		TIntIntMap swaps = heapConf.graph.pack();
		
		// Update the mapping from private to public IDs such that swapped
		// private IDs still refer to the same public ID as before.
		heapConf.publicToPrivateIDs.transformValues(value -> {

            if(swaps.containsKey(value)) {
                return swaps.get(value);
            } else {
                return value;
            }
        });
	}

	@Override
	public HeapConfigurationBuilder addNodes(Type type, int count, TIntArrayList buffer) {
		
		if(type == null || buffer == null) {
			throw new NullPointerException();
		}
		
		if(count < 0) {
			throw new IllegalArgumentException("Provided count must be positive.");
		}
		
		for(int i=0; i < count; i++) {
			
			int publicId = addPrivatePublicIdPair();
			heapConf.graph.addNode(type, 10, 10);
			buffer.add( publicId );
			++heapConf.countNodes;
		}
		
		return this;
	}
	
	/**
	 * Computes a new public and private ID and adds it to the HeapConfiguration.
	 * @return The computed public id.
	 */
	private int addPrivatePublicIdPair() {
		
		int privateId = getNextPrivateId();
		int publicId = getNextPublicId();
		heapConf.publicToPrivateIDs.put(publicId,privateId);
		return publicId;
	}
	
	/**
	 * @return The next private ID available in the graph.
	 */
	protected int getNextPrivateId() {
		
		return heapConf.graph.size();
	}
	
	/**
	 * @return The next public ID available for the underlying HeapConfiguration
	 */
	private int getNextPublicId() {
		
		int result = 0;
		while(heapConf.publicToPrivateIDs.containsKey(result)) {
			++result;
		}
		
		return result;
	}

	@Override
	public HeapConfigurationBuilder removeIsolatedNode(int node) {
		
		int privateId = getPrivateId(node);
		
		if(!isNode(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a node.");
		}
		
		if(heapConf.graph.successorSizeOf(privateId) > 0
				|| heapConf.graph.predecessorSizeOf(privateId) > 0) {
			throw new IllegalArgumentException("Provided node is not isolated.");
		}
		
		removeElement(node, privateId);
		--heapConf.countNodes;
		
		return this;
	}
	
	/**
	 * Removes an existing private ID from the underlying HeapConfiguration.
	 * @param publicId The public ID of the removed element.
	 * @param privateId The private ID of the removed element.
	 */
	private void removeElement(int publicId, int privateId) {
		
		heapConf.graph.removeNodeAt(privateId);
		heapConf.publicToPrivateIDs.remove(publicId);
	}
	
	/**
	 * Provides the private ID corresponding to a given public ID
	 * @param publicId the public ID 
	 * @return the private ID corresponding to publicId if it exists.
	 */
	protected int getPrivateId(int publicId) {
		
		return heapConf.getPrivateId(publicId);
	}

	
	@Override
	public HeapConfigurationBuilder addSelector(int from, SelectorLabel sel, int to) {
		
		int pFrom = getPrivateId(from);
		int pTo = getPrivateId(to);
		
		if(!isNode(pFrom)) {
			throw new IllegalArgumentException("ID 'from' does not refer to a valid node.");
		}
		
		if(sel == null) {
			throw new NullPointerException();
		}
		
		if(!isNode(pTo)) {
			throw new IllegalArgumentException("ID 'to' does not refer to a valid node.");
		}
		
		if(heapConf.graph.containsEdgeLabel(pFrom, sel)) {
			throw new IllegalArgumentException("Provided selector already exists.");
		}
		
		heapConf.graph.addEdge(pFrom, sel, pTo);		
		
		return this;
		 
	}
	
	/**
	 * @param privateId A privateId belonging to the underlyin InternalHeapConfiguration.
	 * @return True if and only if the provided private ID corresponds to a node.
	 */
	protected boolean isNode(int privateId) {
		
		return heapConf.isNode(privateId);
	}

	@Override
	public HeapConfigurationBuilder removeSelector(int node, SelectorLabel sel) {
		
		int privateId = getPrivateId(node);
		
		if(!isNode(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a node: " + node);
		}
		
		if(sel == null) {
			throw new NullPointerException();
		}
		
		heapConf.graph.removeEdgeLabelAt(privateId, sel);
		
		return this;
	}

	@Override
	public HeapConfigurationBuilder replaceSelector(int node, SelectorLabel oldSel, SelectorLabel newSel) {
		
		int privateId = getPrivateId(node);
		
		if(!isNode(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a node.");
		}
		
		if(oldSel == null || newSel == null) {
			throw new NullPointerException();
		}
		
		heapConf.graph.replaceEdgeLabel(privateId, oldSel, newSel);
		
		return this;
	}

	@Override
	public HeapConfigurationBuilder setExternal(int node) {
		
		int privateId = getPrivateId(node);
		
		if(!isNode(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a node: " + node);
		}
		
		if(heapConf.graph.isExternal(privateId)) {
			throw new IllegalArgumentException("Provided node is alread external.");
		}
		
		heapConf.graph.setExternal(privateId);
		
		return this;
	}

	@Override
	public HeapConfigurationBuilder unsetExternal(int node) {
		
		int privateId = getPrivateId(node);
		
		if(!isNode(privateId)) {
			throw new IllegalArgumentException("Provided ID is not a node: " + node);
		}
		
		if(!heapConf.graph.isExternal(privateId)) {
			throw new IllegalArgumentException("Provided node is not external.");
		}
		
		heapConf.graph.unsetExternal(privateId);
		
		return this;
	}

	@Override
	public HeapConfigurationBuilder addVariableEdge(String name, int target) {
		
		int tId = getPrivateId(target);
		
		if(name == null) {
			throw new NullPointerException();
		}
		
		if(!isNode(tId)) {
			throw new IllegalArgumentException("Provided target does not correspond to a node.");
		}
		
		if(heapConf.variableWith(name) != HeapConfiguration.INVALID_ELEMENT) {
			throw new IllegalArgumentException("Variable already exists");
		}
		
		int publicId = addPrivatePublicIdPair();
		int privateId = getPrivateId(publicId);
		
		// variable edges are attached to exactly one node and have no 
		// incoming edges in the underlying graph		
		heapConf.graph.addNode(Variable.get(name), 1, 0);
		heapConf.graph.addEdge(privateId, 1, tId);
		++heapConf.countVariableEdges;
		
		return this;
	}

	@Override
	public HeapConfigurationBuilder removeVariableEdge(int varEdge) {
		
		int privateId = getPrivateId(varEdge);
		
		if(!isVariable(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a variable edge.");
		}
		
		if(heapConf.graph.removeNodeAt(privateId)) {
			--heapConf.countVariableEdges;
		}
		
		return this;
	}

	/**
	 * Checks whether a provided private ID belonging to the underlying InternalHeapConfiguration
	 * corresponds to a variable.
	 * @param privateId A private ID belonging to an element of the underlying InternalHeapConfiguration.
	 * @return True if and only if privateId corresponds to a variable.
	 */
	private boolean isVariable(int privateId) {
		
		return heapConf.graph.nodeLabelOf(privateId) instanceof Variable;
	}

	@Override
	public HeapConfigurationBuilder addNonterminalEdge(Nonterminal label, TIntArrayList attachedNodes) {
		
		if(label == null || attachedNodes == null) {
			throw new NullPointerException();
		}
		
		if(label.getRank() != attachedNodes.size()) {
			throw new IllegalArgumentException("The rank of the provided label and the size of the list of attached nodes do not coincide.");
		}
		
		int publicId = addPrivatePublicIdPair();
		int privateId = getPrivateId(publicId);
		
		heapConf.graph.addNode(label, attachedNodes.size(), 0);
		for(int i=0; i < attachedNodes.size(); i++) {
			int to = getPrivateId( attachedNodes.get(i) );
			if(!isNode(to)) {
				throw new IllegalArgumentException("ID of one attached node does not actually correspond to a node.");
			}
			heapConf.graph.addEdge(privateId, i, to);
		}
		++heapConf.countNonterminalEdges;
		
		return this;
	}
	
	@Override
	public NonterminalEdgeBuilder addNonterminalEdge(Nonterminal nt) {
		return new InternalNonterminalEdgeBuilder( nt, this );
	}

	@Override
	public HeapConfigurationBuilder removeNonterminalEdge(int ntEdge) {
		
		int privateId = getPrivateId(ntEdge);
		
		if(!isNonterminalEdge(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a nonterminal edge.");
		}
		
		if(heapConf.graph.removeNodeAt(privateId)) {
			--heapConf.countNonterminalEdges;
		}
		
		return this;
	}

	/**
	 * Checks whether a provided private ID belonging to the underlying InternalHeapConfiguration is
	 * a nonterminal hyperedge.
	 * @param privateId A private ID that belongs to an element of the underlying InternalHeapConfiguration.
	 * @return True if and only if private ID corresponds to a nonterminal hyperedge.
	 */
	private boolean isNonterminalEdge(int privateId) {
		
		return heapConf.isNonterminalEdge(privateId);
	}

	@Override
	public HeapConfigurationBuilder replaceNonterminal(int ntEdge, Nonterminal newNt) {
		
		int privateId = getPrivateId(ntEdge);
		
		if(newNt == null) {
			throw new NullPointerException();
		}
		
		if(!isNonterminalEdge(privateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a nonterminal edge.");
		}
		
		int rank = ((Nonterminal) heapConf.graph.nodeLabelOf(privateId)).getRank();		
		if( rank != newNt.getRank() ) {
			throw new IllegalArgumentException("The rank of the provided nonterminal is " +
					"different from the original rank: " + rank + " vs. " + newNt.getRank());
		}
		
		heapConf.graph.replaceNodeLabel(privateId, newNt);
		
		return this;
	}

	@Override
	public HeapConfigurationBuilder replaceNonterminalEdge(int ntEdge, HeapConfiguration replacement) {
		
		if(replacement == null) {
			throw new NullPointerException();
		}
		
		if(!(replacement instanceof InternalHeapConfiguration)) {
			throw new IllegalArgumentException("Provided replacement is not an InternalHeapConfiguration.");
		}

		InternalHeapConfiguration repl = (InternalHeapConfiguration) replacement;
		int ntPrivateId = getPrivateId(ntEdge);
		
		if(!isNonterminalEdge(ntPrivateId)) {
			throw new IllegalArgumentException("Provided ID does not correspond to a nonterminal edge.");
		}
		
		// store originally attached nodes, because these are merged with the external nodes of repl.
		TIntArrayList tentacles = heapConf.graph.successorsOf(ntPrivateId);
		
		if(tentacles.size() != replacement.countExternalNodes()) {
			throw new IllegalArgumentException("The rank of the nonterminal edge to be replaced " +
					"does not match the rank of the replacement.");
		}
		
		removeNonterminalEdge(ntEdge);
		
		addReplacementGraph(repl, tentacles);
		
		return this;
	}

	/**
	 * Adds the provided InternalHeapConfiguration to the one underlying this builder.
	 * Its external nodes will be merged with the provided list of nodes.
	 * @param replacement The InternalHeapConfiguration that should be added to the underlying
	 *                    InternalHeapConfiguration.
	 * @param tentacles A list of nodes that determines how replacement and the underlying HeapConfiguration are
	 *                  glued together. Thus, we require that replacement.countExternalNodes() equals tentacles.size().
	 */
	private void addReplacementGraph(InternalHeapConfiguration replacement, TIntArrayList tentacles) {
		int replSize = replacement.graph.size();
		TIntArrayList newElements = computeNewElements(replacement, tentacles);
		
		// In the second pass we add all selectors for nodes as well as nonterminal hyperedges
		// and their tentacles.
		for(int i=0; i < replSize; i++) {
			
			if(replacement.isNode(i))  {
				
				addNodeFromReplacement(replacement, newElements, i);
				
			} else if(replacement.isNonterminalEdge(i)) {
				
				addNtEdgeFromReplacement(replacement, newElements, i);
			}
		}
	}
	
	/**
	 * Creates a map that contains the new private IDs assigned to elements of a provided InternalHeapConfiguration
	 * that is added to the underlying InternalHeapConfiguration.
	 * For all elements except external nodes new private IDs are created.
	 * For each external node, the map contains the private ID of a node in the underlying InternalHeapConfiguration
	 * that is merged with it.
	 *
	 * @param replacement The InternalHeapConfiguration that should be added to the underlying HeapConfiguration.
	 * @param tentacles A list of nodes in the underlying HeapConfiguration that determines the nodes that are
	 *                  merged with the external nodes of replacement.
	 * @return A list that maps each element of replacement to its private ID in the underlying HeapConfiguration.
	 */
	private TIntArrayList computeNewElements(InternalHeapConfiguration replacement, TIntArrayList tentacles) {
		
		int replSize = replacement.graph.size();
		TIntArrayList newElements = new TIntArrayList(replSize);
		
		for(int i=0; i < replSize; i++) {
			
			if(replacement.isNode(i)) {
				
				int extPos = replacement.graph.externalPosOf(i);
				if(extPos != LabeledDigraph.INVALID) {
					newElements.add( tentacles.get( extPos ) );
				} else {
					
					int privateId = getNextPrivateId();
					addPrivatePublicIdPair();
					heapConf.graph.addNode(replacement.graph.nodeLabelOf(i), 10, 10);
					++heapConf.countNodes;
					newElements.add(privateId);
				}
			} else {
				
				newElements.add( LabeledDigraph.INVALID );
			}
		}
		
		return newElements;
	}

    /**
     * Adds a single node from a HeapConfiguration that should be added to the underlying HeapConfiguration with
     * the provided private ID. Furthermore, all edges of the node are added to the underlying HeapConfiguration.
     * @param replacement The HeapConfiguration that should be added to the underlying HeapConfiguration.
     * @param newElements A list mapping all elements of replacement to their new private IDs in the underlying
     *                    InternalHeapConfiguration.
     * @param nodeIdToAdd The private ID of the node that should be added.
     */
	private void addNodeFromReplacement(InternalHeapConfiguration replacement,
                                        TIntArrayList newElements, int nodeIdToAdd) {
		int privateId = newElements.get(nodeIdToAdd);
		TIntArrayList succ = replacement.graph.successorsOf(nodeIdToAdd);
		for(int j=0; j < succ.size(); j++) {
			Object label = replacement.graph.edgeLabelAt(nodeIdToAdd, j);
			int to = newElements.get( succ.get(j) );
			heapConf.graph.addEdge(privateId, label, to);
		}
	}

    /**
     * Adds a nonterminal edge from a HeapConfiguration that should be added to the underlying HeapConfiguration
     * with the provided privateID. Furthermore, all tentacles to nodes are set.
     * @param replacement The HeapConfiguration that should be added to the underlying HeapConfiguration.
     * @param newElements A list mapping all elements of replacement to their new private IDs in the underlying
     *                    InternalHeapConfiguration.
     * @param ntIdToAdd The private ID of the nonterminal edge that should be added.
     */
	private void addNtEdgeFromReplacement(InternalHeapConfiguration replacement, TIntArrayList newElements, int ntIdToAdd) {
		int freshPrivateId = getNextPrivateId();
		addPrivatePublicIdPair();
		TIntArrayList succ = replacement.graph.successorsOf(ntIdToAdd);
		heapConf.graph.addNode(replacement.graph.nodeLabelOf(ntIdToAdd), succ.size(), 0);
		++heapConf.countNonterminalEdges;
		for(int j=0; j < succ.size(); j++) {
			int to = newElements.get(succ.get(j));
			heapConf.graph.addEdge(freshPrivateId, j, to);
		}
	}

	@Override
	public HeapConfigurationBuilder replaceMatching(Matching matching, Nonterminal nonterminal) {
		
		if(matching == null || nonterminal == null) {
			throw new NullPointerException();
		}
		
		InternalHeapConfiguration pattern = (InternalHeapConfiguration) matching.pattern();
		
		if(pattern.countExternalNodes() != nonterminal.getRank()) {
			throw new IllegalArgumentException("The number of external nodes in pattern must " +
					"match the rank of the provided nonterminal.");
		}
		
		InternalMatching internalMatching = (InternalMatching) matching;
		
		// First remove all selector edges and tentacles that also occur in pattern
		removeSelectorAndTentacleEdges(internalMatching, pattern);
		
		removeNonExternalNodes(internalMatching, pattern);
		
		addMatchingNonterminalEdge(internalMatching, pattern, nonterminal);
		
		return this;
	}

    /**
     * Removes all selector and tentacle edges in the underlying HeapConfiguration that belong to the provided
     * HeapConfiguration according to the provided matching.
     * @param matching A mapping from the provided HeapConfiguration to elements of the underlying HeapConfiguration.
     * @param pattern A HeapConfiguration that is embedded in the underlying HeapConfiguration.
     */
	private void removeSelectorAndTentacleEdges(InternalMatching matching, InternalHeapConfiguration pattern) {
		
		for(int i=0; i< pattern.graph.size(); i++) {
			
			int match = matching.internalMatch(i);
			
			for(int j=0; j < pattern.graph.successorSizeOf(i); j++) {
				
				Object l = pattern.graph.edgeLabelAt(i, j);
				if(l instanceof SelectorLabel) {
					
					heapConf.graph.removeEdgeLabelAt(match, l);
				}
			}
		}
	}

    /**
     * Removes all nodes in the underlying HeapConfiguration that belong to the provided HeapConfiguration
     * according to the provided matching.
     * @param matching A mapping from the provided HeapConfiguration to elements of the underlying HeapConfiguration.
     * @param pattern A HeapConfiguration that is embedded in the underlying HeapConfiguration.
     */
	private void removeNonExternalNodes(InternalMatching matching, InternalHeapConfiguration pattern) {
		
		for(int i=0; i< pattern.graph.size(); i++) {
			
			if(!pattern.graph.isExternal(i)) {				
				int match = matching.internalMatch(i);
				
				if(pattern.isNode(i)) {
					--heapConf.countNodes;
				} else if(pattern.isNonterminalEdge(i)) {
					--heapConf.countNonterminalEdges;
				}
				
				heapConf.graph.removeNodeAt(match);				
			}
		}
	}

    /**
     * Add a nonterminal hyperedge in the provided HeapConfiguration to the underlying HeapConfiguration
     * that is labeled with the provided Nonterminal. Its tentacles are determined by the provided matching.
     * @param matching A mapping from the provided HeapConfiguration to elements of the underlying HeapConfiguration.
     * @param pattern A HeapConfiguration that is embedded in the underlying HeapConfiguration.
     * @param nonterminal The label of the added hyperedge.
     */
	private void addMatchingNonterminalEdge(InternalMatching matching,
                                            InternalHeapConfiguration pattern, Nonterminal nonterminal) {

		int privateId = getNextPrivateId();
		addPrivatePublicIdPair();
		heapConf.graph.addNode(nonterminal, nonterminal.getRank(), 0);
		for(int i=0; i < nonterminal.getRank(); i++) {
			
			int extId = pattern.graph.externalNodeAt(i);
			if(extId == LabeledDigraph.INVALID) {
				throw new IllegalArgumentException("One of the patterns external nodes could not be matched");
			}
			
			int t = matching.internalMatch( extId );
			heapConf.graph.addEdge(privateId, i, t);
		}
		++heapConf.countNonterminalEdges;
	}



}