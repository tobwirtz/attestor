package de.rwth.i2.attestor.grammar.materialization;

import static org.junit.Assert.assertEquals;

import de.rwth.i2.attestor.tasks.GeneralNonterminal;
import de.rwth.i2.attestor.tasks.GeneralSelectorLabel;
import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.graph.Nonterminal;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.InternalHeapConfiguration;
import de.rwth.i2.attestor.types.Type;
import de.rwth.i2.attestor.types.TypeFactory;
import gnu.trove.list.array.TIntArrayList;

public class GraphMaterializerTest {
	
	public static int RANK = 2;
	public static int NODE_FOR_TO_REPLACE = 1;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testOnDefaultNonterminal() throws Exception {
		GraphMaterializer materializer = new GraphMaterializer();
		
		Nonterminal toReplace = createDefaultNonterminal();
		HeapConfiguration inputGraph = createOneNonterminalInput( toReplace );
		HeapConfiguration rule = createSimpleRule();
		HeapConfiguration expectedMaterializedGraph = createExpectedMaterializedGraph();
		
		int toReplaceIndex = findIndexOf( toReplace, inputGraph );
		
		HeapConfiguration actual = 
				materializer.getMaterializedCloneWith( inputGraph, toReplaceIndex, rule );
		
		assertEquals("Input Graph has changed", createOneNonterminalInput(toReplace), inputGraph);
		assertEquals("not materialized as expected", expectedMaterializedGraph, actual );
	}

	private Nonterminal createDefaultNonterminal() {
		String uniqueLabel = "GraphMaterializerTest";
		boolean [] reductionTentacles = new boolean[]{true,false};
		
		return GeneralNonterminal.getNonterminal(uniqueLabel, RANK, reductionTentacles);
	}
	
	private HeapConfiguration createOneNonterminalInput(Nonterminal toReplace) {
		HeapConfiguration hc = new InternalHeapConfiguration();

		Type type = TypeFactory.getInstance().getType("type");

		TIntArrayList nodes = new TIntArrayList();
		return hc.builder().addNodes(type, 2, nodes)
				.addNonterminalEdge( toReplace )
				.addTentacle(nodes.get( NODE_FOR_TO_REPLACE ) )
				.addTentacle(nodes.get(0) )
				.build()
				.build();
	}
	
	private HeapConfiguration createSimpleRule() {
		HeapConfiguration hc = new InternalHeapConfiguration();

		Type type = TypeFactory.getInstance().getType("type");
		SelectorLabel label = GeneralSelectorLabel.getSelectorLabel("label");

		TIntArrayList nodes = new TIntArrayList();
		return hc.builder().addNodes(type, 3, nodes)
				.setExternal(0)
				.setExternal(1)
				.addSelector(nodes.get(0), label, nodes.get(1) )
				.addSelector( nodes.get(1), label, nodes.get(2) )
				.build();
	}
	
	private HeapConfiguration createExpectedMaterializedGraph() {
		HeapConfiguration hc = new InternalHeapConfiguration();

		Type type = TypeFactory.getInstance().getType("type");
		SelectorLabel label = GeneralSelectorLabel.getSelectorLabel("label");

		TIntArrayList nodes = new TIntArrayList();
		return hc.builder().addNodes(type, 3, nodes)
				.addSelector(nodes.get(2), label, nodes.get(1) )
				.addSelector( nodes.get(1), label, nodes.get(0) )
				.build();
	}
	
	private int findIndexOf( Nonterminal toReplace, HeapConfiguration inputGraph ) throws Exception {
		TIntArrayList nts = inputGraph.attachedNonterminalEdgesOf(NODE_FOR_TO_REPLACE);
		for( int i = 0; i < nts.size(); i++ ){
			if( inputGraph.labelOf(nts.get(i) ).equals(toReplace) ){
				return nts.get( i );
			}
		}
		throw new Exception("Nt not present");
	}


}
