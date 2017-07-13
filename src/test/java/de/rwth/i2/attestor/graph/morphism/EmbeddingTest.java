package de.rwth.i2.attestor.graph.morphism;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.ExampleHcImplFactory;
import de.rwth.i2.attestor.graph.morphism.checkers.VF2EmbeddingChecker;

public class EmbeddingTest {
	
	@Test 
	public void testIdenticalGraphs() {
		
		Graph g = (Graph) ExampleHcImplFactory.getListRule1();
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(g, g);
		assertTrue("Identical graphs are embeddings of each other", checker.hasMorphism());
	}
	
	@Test 
	public void testSimpleDLLEmbedding() {
		
		Graph p = (Graph) ExampleHcImplFactory.getTwoElementDLL();
		Graph t = (Graph) ExampleHcImplFactory.getThreeElementDLL();
	
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertTrue("Two element DLL is embedded in three element DLL", checker.hasMorphism());
	}
	
	@Test 
	public void testWithInternal() {

		Graph p = (Graph) ExampleHcImplFactory.getThreeElementDLL();
		Graph t = (Graph) ExampleHcImplFactory.getFiveElementDLL();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertTrue("Three element DLL is embedded in five element DLL, both with 2 external nodes", checker.hasMorphism());
	}
	
	@Test 
	public void testNegative() {
		

		Graph p = (Graph) ExampleHcImplFactory.getBrokenFourElementDLL();
		Graph t = (Graph) ExampleHcImplFactory.getFiveElementDLL();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertFalse("Three element DLL with one additional pointer not embedded in five element dll", checker.hasMorphism());
	}
	
	@Test
	public void testSLLRule2(){
	
		Graph p = (Graph) ExampleHcImplFactory.getListRule2();
		Graph t = (Graph) ExampleHcImplFactory.getListRule2Test();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertTrue( checker.hasMorphism() );
	}
	
	@Test
	public void testSLLRule2Fail(){
	
		Graph p = (Graph) ExampleHcImplFactory.getListRule2();
		Graph t = (Graph) ExampleHcImplFactory.getListRule2TestFail();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertFalse( checker.hasMorphism() );
	}
	
	@Test
	public void testSLLRule3(){
	
		Graph p = (Graph) ExampleHcImplFactory.getListRule3();
		Graph t = (Graph) ExampleHcImplFactory.getTestForListRule3();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertTrue( checker.hasMorphism() );
	}
	
	@Test
	public void testSLLRule3Fail(){
	
		Graph p = (Graph) ExampleHcImplFactory.getListRule3();
		Graph t = (Graph) ExampleHcImplFactory.getTestForListRule3Fail();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertFalse( checker.hasMorphism() );
	}
	
	@Test
	public void testAllExternal() {
		
		Graph p = (Graph) ExampleHcImplFactory.getTreeLeaf();
		Graph t = (Graph) ExampleHcImplFactory.get2TreeLeaf();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertTrue( checker.hasMorphism() );
		
	}
	
	@Test
	public void testDLLWithThirdPointer() {
		
		Graph p = (Graph) ExampleHcImplFactory.getDLL2Rule();
		Graph t = (Graph) ExampleHcImplFactory.getDLLTarget();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run(p, t);
		assertTrue( checker.hasMorphism() );

	}
	
	@Test
	public void testHeapsWithDifferentStacks(){
		HeapConfiguration oneStack = ExampleHcImplFactory.getInput_DifferentStacks_1();
		HeapConfiguration otherStack = ExampleHcImplFactory.getInput_DifferentStacks_2();
		
		VF2EmbeddingChecker checker = new VF2EmbeddingChecker();
		checker.run( (Graph) oneStack, (Graph) otherStack);
		assertTrue(checker.hasMorphism());
	}
	
	
	
	
}