package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;


import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleExecutable;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.JimpleUtil;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.TemporaryVariablesUtil;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.*;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.util.*;

/**
 * AssignStmts model assignments of locals or fields to values e.g. x.y = z
 * 
 * @author hannah
 *
 */
public class AssignStmt extends Statement {

	private static final Logger logger = LogManager.getLogger( "AssignStmt" );
	/**
	 * the element to which something will be assigned (e.g. variable or field)
	 */
	private final SettableValue lhs;
	/**
	 * The expression that will be assigned
	 */
	private final Value rhs;
	/**
	 * the program counter of the successor state
	 */
	private final int nextPC;
	
	private final ViolationPoints potentialViolationPoints;
	
	private final Set<String> liveVariableNames;

	public AssignStmt( SettableValue lhs , Value rhs , int nextPC, Set<String> liveVariableNames ){
		super();
		this.rhs = rhs;
		this.lhs = lhs;
		this.nextPC = nextPC;
		this.liveVariableNames = liveVariableNames;
		
		potentialViolationPoints = new ViolationPoints();
		potentialViolationPoints.addAll(lhs.getPotentialViolationPoints());
		potentialViolationPoints.addAll(rhs.getPotentialViolationPoints());
		
		
	}

	/**
	 * evaluates the rhs and assigns it to the left hand side. In case the rhs
	 * evaluates to undefined, the variable will be removed from the heap (It
	 * will not point to its old value). In this case the logger will also issue
	 * a warning. <br>
	 * If the types of the lhs and the rhs do not match, there will be a
	 * warning, but the assignment will still be realized.<br>
	 * 
	 * If the variable in rhs is not live in this statement, it will be removed from the heap
	 * to enable abstraction at this point.
	 * 
	 * @throws NotSufficientlyMaterializedException if rhs or lhs cannot be evaluated on the given heap
	 */
	@Override
	public Set<ProgramState> computeSuccessors( ProgramState state ) throws NotSufficientlyMaterializedException {
		
		JimpleExecutable executable = (JimpleExecutable) state;		
		executable = JimpleUtil.deepCopy(executable);
		
		ConcreteValue concreteRHS;
		try {
			concreteRHS = rhs.evaluateOn( executable );
		} catch (NullPointerDereferenceException e) {
			logger.error( e.getErrorMessage(this) );
			concreteRHS = executable.getUndefined();
		}

		if( concreteRHS.isUndefined() ){
			logger.warn( "The value of rhs is undefined. Ignoring Assign." );
		}else{
			if( DebugMode.ENABLED && !( lhs.getType().equals( concreteRHS.type() ) ) ){
				String msg = "The type of the resulting ConcreteValue for rhs does not match ";
				msg += " with the type of the lhs";
				msg += "\n expected: " + lhs.getType() + " got: " + concreteRHS.type();
				logger.warn( msg );
			}
		}
		
		try {
			lhs.evaluateOn( executable );
			lhs.setValue( executable, concreteRHS );
		} catch (NullPointerDereferenceException e) {
			logger.error(e.getErrorMessage(this));
		}
		
		TemporaryVariablesUtil.checkAndRemoveTemp(rhs.toString(), executable, liveVariableNames);
		
		JimpleExecutable result = JimpleUtil.deepCopy(executable);
		result.setProgramCounter(nextPC);
		
		return SingleElementUtil.createSet( result );
	}

	@Override
	public boolean needsMaterialization( ProgramState state ){
		
		JimpleExecutable executable = (JimpleExecutable) state;
		
		return rhs.needsMaterialization( executable ) || lhs.needsMaterialization( executable );
	}


	public String toString(){
		return lhs.toString() + " = " + rhs.toString() + ";";
	}

	@Override
	public boolean hasUniqueSuccessor() {
		
		return true;
	}
	
	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return potentialViolationPoints;
	}

	@Override
	public Set<Integer> getSuccessorPCs() {
		
		return SingleElementUtil.createSet(nextPC);
	}
	
}