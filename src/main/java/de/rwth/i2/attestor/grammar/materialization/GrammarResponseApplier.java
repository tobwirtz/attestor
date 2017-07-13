package de.rwth.i2.attestor.grammar.materialization;

import java.util.Collection;

import de.rwth.i2.attestor.grammar.materialization.communication.GrammarResponse;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public interface GrammarResponseApplier {

	/**
	 * materializes the given edge in the inputGraph with each rule in the
	 * grammarResponse yielding a collection of materialized graphs.
	 * 
	 * @param inputGraph the graph which will be materialized
	 * @param edgeId the id of the nonterminal edge which will be materialized
	 * @param grammarResponse a DefaultGrammarResponse holding all the rules 
	 * which will be applied
	 * @return a collection holding all the materialization results.
	 * @throws WrongResponseTypeException if the grammarResponse is not a DefaultGrammarResponse 
	 */
	Collection<HeapConfiguration> applyGrammarResponseTo(HeapConfiguration inputGraph, int edgeId,
			GrammarResponse grammarResponse) throws WrongResponseTypeException;

}