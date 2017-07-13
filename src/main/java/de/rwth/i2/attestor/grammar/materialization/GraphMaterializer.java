package de.rwth.i2.attestor.grammar.materialization;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public class GraphMaterializer {


	public HeapConfiguration getMaterializedCloneWith( HeapConfiguration inputGraph, 
													   int toReplaceIndex,
													   HeapConfiguration rule) {
		
		final HeapConfiguration cloneOfInput = inputGraph.clone();
		
		final HeapConfiguration materializedGraph = 
				cloneOfInput.builder().replaceNonterminalEdge(toReplaceIndex, rule).build();
		return materializedGraph;
	}

}