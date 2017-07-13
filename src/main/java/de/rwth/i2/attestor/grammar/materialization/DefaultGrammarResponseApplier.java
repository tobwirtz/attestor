package de.rwth.i2.attestor.grammar.materialization;

import java.util.ArrayList;
import java.util.Collection;

import de.rwth.i2.attestor.grammar.materialization.communication.DefaultGrammarResponse;
import de.rwth.i2.attestor.grammar.materialization.communication.GrammarResponse;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public class DefaultGrammarResponseApplier implements GrammarResponseApplier {
	
	GraphMaterializer graphMaterializer;

	public DefaultGrammarResponseApplier(GraphMaterializer graphMaterializer ) {
		 this.graphMaterializer = graphMaterializer;
	}

	/* (non-Javadoc)
	 * @see de.rwth.i2.attestor.grammar.materialization.GrammarResponseApplier#applyGrammarResponseTo(de.rwth.i2.attestor.graph.heap.HeapConfiguration, int, de.rwth.i2.attestor.grammar.materialization.communication.GrammarResponse)
	 */
	@Override
	public Collection<HeapConfiguration> applyGrammarResponseTo( HeapConfiguration inputGraph, 
										int edgeId, 
										GrammarResponse grammarResponse) 
										throws WrongResponseTypeException {
		
		if( grammarResponse instanceof DefaultGrammarResponse ){	
			DefaultGrammarResponse defaultGrammarResponse = (DefaultGrammarResponse) grammarResponse;
			
			return applyRulesInGrammarResponseTo(inputGraph, edgeId, defaultGrammarResponse);
			
		}else{
			throw new WrongResponseTypeException("can only handle DefaultGrammarResponse" );
		}
		
	}

	/**
	 * uses the graphMaterializer to apply each rule in the grammarResponse to
	 * the inputGraph
	 * 
	 * @param inputGraph the graph which will be materialized
	 * @param edgeId the id of the nonterminal edge which will be materialized
	 * @param grammarResponse a DefaultGrammarResponse holding all the rules 
	 * which will be applied
	 * @return a collection holding all the materialization results.
	 */
	private Collection<HeapConfiguration> applyRulesInGrammarResponseTo(
			HeapConfiguration inputGraph, int edgeId,
			DefaultGrammarResponse defaultGrammarResponse) {
		
		Collection<HeapConfiguration> materializedGraphs = new ArrayList<>();
		
		for( HeapConfiguration rhsToApply : defaultGrammarResponse.getApplicableRules() ){
			
			HeapConfiguration materializedGraph = 
					graphMaterializer.getMaterializedCloneWith( inputGraph, edgeId, rhsToApply );
			materializedGraphs.add( materializedGraph );
		}
		
		return materializedGraphs;
	}

}