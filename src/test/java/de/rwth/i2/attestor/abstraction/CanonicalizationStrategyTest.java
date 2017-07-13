package de.rwth.i2.attestor.abstraction;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import de.rwth.i2.attestor.tasks.GeneralNonterminal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import de.rwth.i2.attestor.grammar.Grammar;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.ExampleHcImplFactory;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Skip;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.tasks.defaultTask.DefaultCanonicalizationStrategy;
import de.rwth.i2.attestor.tasks.defaultTask.DefaultState;
import gnu.trove.list.array.TIntArrayList;


public class CanonicalizationStrategyTest {
	
	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger( "CanonicalizationStrategyTest" );
	
	private DefaultCanonicalizationStrategy canonicalizationStrategy;
	
	@Before
	public void setUp() throws Exception {
		
		GeneralNonterminal listLabel = GeneralNonterminal.getNonterminal( "List", 2, new boolean[] { false, true } );
		
		Grammar grammar = Grammar.builder()
				.addRule( listLabel , ExampleHcImplFactory.getListRule1() )
				.addRule( listLabel , ExampleHcImplFactory.getListRule2() )
				.addRule( listLabel , ExampleHcImplFactory.getListRule3() )
				.build();
		
		canonicalizationStrategy = new DefaultCanonicalizationStrategy(grammar, true);
		canonicalizationStrategy.setIgnoreUniqueSuccessorStatements(false);
	}

	@Test
	public void testSmall() {

		HeapConfiguration test = ExampleHcImplFactory.getCanonizationTest1();
		
		DefaultState testExec = new DefaultState(test);
		Set<ProgramState> resStates = canonicalizationStrategy.canonicalize(new Skip(0), testExec);
		
		assertEquals("Input heap should not change", ExampleHcImplFactory.getCanonizationTest1(), test );
		
		assertEquals("There is only one embedding.", resStates.size(), 1);
		
		for(ProgramState state : resStates) {
			
			assertEquals("result not as expected", ExampleHcImplFactory.getCanonizationRes1(), state.getHeap() );
		}
		
		
	}
	
	@Test
	public void testBig() {

		HeapConfiguration test = ExampleHcImplFactory.getCanonizationTest2();
		
	
		DefaultState testExec = new DefaultState(test);
		Set<ProgramState> resStates = canonicalizationStrategy.canonicalize(new Skip(0), testExec);
		
		assertEquals("Input heap should not change", ExampleHcImplFactory.getCanonizationTest2(), test );
		
		assertEquals("There is only one embedding.", resStates.size(), 1);
		
		for(ProgramState state : resStates) {
				
			assertEquals("result not as expected", ExampleHcImplFactory.getCanonizationRes1(), state.getHeap() );
		}
	}
	
	@Test
	public void testWithVariable() {

		HeapConfiguration test = ExampleHcImplFactory.getCanonizationTest3();
		
		DefaultState testExec = new DefaultState(test);
		Set<ProgramState> resStates = canonicalizationStrategy.canonicalize(null, testExec);
		
		assertEquals("Input heap should not change", ExampleHcImplFactory.getCanonizationTest3(), test );
		assertEquals("There is only one embedding.", resStates.size(), 1);
		
		for(ProgramState state : resStates) {
		
			assertEquals("result not as expected", ExampleHcImplFactory.getCanonizationRes3(), state.getHeap() );
		}
	}
	
	@Test
	public void testLongSllFullAbstraction() {
		
		HeapConfiguration test = ExampleHcImplFactory.getLongConcreteSLL();
		DefaultState testExec = new DefaultState(test);
		Set<ProgramState> resStates = canonicalizationStrategy.canonicalize(new Skip(0), testExec);
		
		HeapConfiguration expected = ExampleHcImplFactory.getSLLHandle();
		
		assertEquals(1, resStates.size());
		
		for(ProgramState state : resStates) {
			assertEquals(expected, state.getHeap());
		}
	}
	
	@Test
	public void testLongSllFullAbstractionWithVariables() {
		
		HeapConfiguration test = ExampleHcImplFactory.getLongConcreteSLL().clone();
		
		TIntArrayList nodes = test.nodes();
		
		test.builder()
			.addVariableEdge("x", nodes.get(0))
			.addVariableEdge("y", nodes.get(9))
			.build();
		
		
		DefaultState testExec = new DefaultState(test);
		Set<ProgramState> resStates = canonicalizationStrategy.canonicalize(new Skip(0), testExec);
		
		HeapConfiguration expected = ExampleHcImplFactory.getSLLHandle();
		TIntArrayList expectedNodes = expected.nodes();
		
		expected.builder()
			.addVariableEdge("x", expectedNodes.get(0))
			.addVariableEdge("y", expectedNodes.get(1))
			.build();
		
		assertEquals(1, resStates.size());
		
		for(ProgramState state : resStates) {
			assertEquals(expected, state.getHeap());
		}
	}
}