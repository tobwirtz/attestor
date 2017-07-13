package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.Set;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleExecutable;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.JimpleUtil;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.util.SingleElementUtil;

/**
 * Branching Skip has no effect on the heap and two successors. It models
 * statements with two successors which we do not actually translate
 * 
 * @author Hannah Arndt
 *
 */
public class BranchingSkip extends Statement {

	/**
	 * program counter for the first successor
	 */
	private final int leftSuccessor;
	/**
	 * program counter for the second successor
	 */
	private final int rightSuccessor;

	public BranchingSkip( int leftSuccessor, int rightSuccessor ){
		super();
		this.leftSuccessor = leftSuccessor;
		this.rightSuccessor = rightSuccessor;
	}

	@Override
	public boolean needsMaterialization( ProgramState heap ){
		return false;
	}


	public String toString(){
		return "Skip;";
	}

	/**
	 * copies the input heap to both successor states
	 */
	@Override
	public Set<ProgramState> computeSuccessors(ProgramState state) {

		JimpleExecutable executable = (JimpleExecutable) state;
		
		JimpleExecutable leftResult = JimpleUtil.shallowCopyExecutable(executable);
		leftResult.setProgramCounter(leftSuccessor);
		
		JimpleExecutable rightResult = JimpleUtil.shallowCopyExecutable(executable);
		rightResult.setProgramCounter(rightSuccessor);
		
		Set<ProgramState> res = SingleElementUtil.createSet(leftResult);
		res.add( rightResult );
		return res;
	}

	@Override
	public boolean hasUniqueSuccessor() {

		return false;
	}
	
	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return ViolationPoints.getEmptyViolationPoints();
	}

	@Override
	public Set<Integer> getSuccessorPCs() {
		
		Set<Integer> res = SingleElementUtil.createSet(leftSuccessor);
		res.add(rightSuccessor);
		return res;
	}
}
