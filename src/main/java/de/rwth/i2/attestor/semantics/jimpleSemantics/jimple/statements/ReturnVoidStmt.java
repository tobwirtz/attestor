package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationBuilder;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SymbolicExecutionObserver;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.semantics.util.VariableScopes;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import gnu.trove.iterator.TIntIterator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ReturnVoidStmt models the statement return;
 * 
 * @author Hannah Arndt
 *
 */
public class ReturnVoidStmt extends Statement {

	/**
	 * removes all locals of the current scope from the heap,
	 * and returns the resulting heap with exit location (-1)
	 */
	@Override
	public Set<ProgramState> computeSuccessors(ProgramState programState, SymbolicExecutionObserver observer)
			throws NotSufficientlyMaterializedException{

		observer.update(this, programState);

		programState = programState.clone();

		// -1 since this statement has no successor location
		int nextPC = -1;
		programState.setProgramCounter(nextPC);

		removeLocals( programState );
		return Collections.singleton(programState);
	}

	@Override
	public boolean needsMaterialization( ProgramState executable ){
		return false;
	}

	public String toString(){
		return "return;";
	}

    @Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return ViolationPoints.getEmptyViolationPoints();
	}
	
	@Override
	public Set<Integer> getSuccessorPCs() {
		
		return new HashSet<>();
	}

	/**
	 * Removes local variables from the current block.
	 * @param programState The programState whose local variables should be removed.
	 */
	private void removeLocals( ProgramState programState ){
		int scope = programState.getScopeDepth();
		HeapConfiguration heap = programState.getHeap();
		HeapConfigurationBuilder builder = heap.builder();
		
		TIntIterator iter = heap.variableEdges().iterator();
		
		while(iter.hasNext()) {
			int var = iter.next();
			String name = heap.nameOf(var);
			if(VariableScopes.hasScope(name, scope)) {
				builder.removeVariableEdge(var);
			}
		}
		builder.build();
	}

}
