package de.rwth.i2.attestor.grammar.materialization.moduleTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.rwth.i2.attestor.tasks.GeneralNonterminal;
import de.rwth.i2.attestor.tasks.GeneralSelectorLabel;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import de.rwth.i2.attestor.grammar.Grammar;
import de.rwth.i2.attestor.grammar.materialization.DefaultGrammarResponseApplier;
import de.rwth.i2.attestor.grammar.materialization.DefaultMaterializationRuleManager;
import de.rwth.i2.attestor.grammar.materialization.GeneralMaterializationStrategy;
import de.rwth.i2.attestor.grammar.materialization.GrammarResponseApplier;
import de.rwth.i2.attestor.grammar.materialization.GraphMaterializer;
import de.rwth.i2.attestor.grammar.materialization.MaterializationRuleManager;
import de.rwth.i2.attestor.grammar.materialization.ViolationPointResolver;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.ExampleHcImplFactory;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.tasks.defaultTask.DefaultState;
import sun.java2d.loops.FillRect.General;

public class GeneralMaterializationStrategyTest_Materialize_Default {

	private static GeneralMaterializationStrategy materializer;
	
	@BeforeClass
	public static void setUp() throws Exception {
		GeneralNonterminal listLabel = GeneralNonterminal
				.getNonterminal( "List", 2, new boolean[] { false, true } );
		
		Grammar grammar = Grammar.builder()
								.addRule( listLabel , ExampleHcImplFactory.getListRule1() )
								.addRule( listLabel , ExampleHcImplFactory.getListRule2() )
								.build();
		
		ViolationPointResolver violationPointResolver = new ViolationPointResolver(grammar);
		MaterializationRuleManager ruleManager = 
				new DefaultMaterializationRuleManager(violationPointResolver);
		GraphMaterializer graphMaterializer = new GraphMaterializer();
		GrammarResponseApplier ruleApplier = new DefaultGrammarResponseApplier(graphMaterializer);
		
		materializer = new GeneralMaterializationStrategy( ruleManager, ruleApplier );
		
	}

	@Test
	public void testMaterialize_Default() {
		
		HeapConfiguration testInput = ExampleHcImplFactory.getMaterializationTest();
		DefaultState inputConf = new DefaultState(testInput);
		
		ViolationPoints vio = new ViolationPoints("x", "next");
		
		List<ProgramState> res = materializer.materialize(inputConf, vio);
		
		assertEquals("input graph should not change", ExampleHcImplFactory.getMaterializationTest(), testInput );
		assertEquals( 2, res.size() );
		
		for(int i=0; i < 2; i++) {
			
			HeapConfiguration hc = res.get(i).getHeap();
			int x = hc.variableWith("0-x");
			int t = hc.targetOf(x);
			
			
			assertTrue(hc.selectorLabelsOf(t).contains(GeneralSelectorLabel.getSelectorLabel("next")));
		}
		
		List<HeapConfiguration> resHCs = new ArrayList<>();
		resHCs.add( res.get(0).getHeap() );
		resHCs.add( res.get(1).getHeap() );
		
		assertTrue("first expected materialization", resHCs.contains( ExampleHcImplFactory.getMaterializationRes1() ) );
		assertTrue("second expected materialization", resHCs.contains( ExampleHcImplFactory.getMaterializationRes2() ) );
	}

}