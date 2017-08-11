package de.rwth.i2.attestor.strategies.indexedGrammarStrategies.index;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.strategies.indexedGrammarStrategies.IndexedNonterminal;

import java.util.List;

public interface IndexMaterializationStrategy {

	IndexedNonterminal materializeStack( IndexedNonterminal nt, IndexSymbol s );
	List<IndexSymbol> getRuleCreatingSymbolFor(IndexSymbol s1, IndexSymbol s2);
	void materializeStacks(HeapConfiguration heapConfiguration, IndexSymbol originalIndexSymbol,
			IndexSymbol desiredIndexSymbol);
	boolean canCreateSymbolFor(IndexSymbol originalIndexSymbol, IndexSymbol desiredIndexSymbol);

}