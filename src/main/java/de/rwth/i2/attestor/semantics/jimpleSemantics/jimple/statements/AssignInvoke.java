package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleExecutable;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.JimpleUtil;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.AbstractMethod;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.*;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.util.*;

/**
 * AssignInvoke models statements of the form x = foo(); or x = bar(3, name);
 * @author Hannah Arndt
 *
 */
public class AssignInvoke extends Statement {

	private static final Logger logger = LogManager.getLogger( "AssignInvoke" );

	/**
	 * the value to which the result will be assigned
	 */
	private final SettableValue lhs;
	/**
	 * the abstract translation of the method that is called
	 */
	private final AbstractMethod method;
	/**
	 * handles arguments, and if applicable the this-reference.
	 * Also manages the variable scope.
	 */
	private final InvokeHelper invokePrepare;
	/**
	 * the program counter of the successor statement
	 */
	private final int nextPC;

	public AssignInvoke( SettableValue lhs, AbstractMethod method, InvokeHelper invokePrepare,
			int nextPC ){
		super();
		this.lhs = lhs;
		this.method = method;
		this.invokePrepare = invokePrepare;
		this.nextPC = nextPC;
	}

	/**
	 * gets the fixpoint of the abstract method for the given input.
	 * For each of the resulting heaps, retrieves the return argument and 
	 * creates a new heap where it is assigned correctly.
	 * If a result is lacking a return, it is ignored.<br>
	 * 
	 * If any variable appearing in the arguments is not live at this point,
	 * it will be removed from the heap to enable abstraction. Furthermore,
	 * if lhs is a variable it will be removed before invoking the function,
	 * as it is clearly not live at this point.
	 */
	@Override
	public Set<ProgramState> computeSuccessors( ProgramState state )
			throws NotSufficientlyMaterializedException{
		
		JimpleExecutable executable = (JimpleExecutable) state;
		executable = JimpleUtil.deepCopy(executable);
		
		invokePrepare.prepareHeap( executable );
		
		if( lhs instanceof Local ){
			executable.leaveScope();
			executable.removeVariable( ((Local)lhs).getName() );
			executable.enterScope();
		}
		Set<ProgramState> methodResult = method.getResult( executable.getHeap(), executable.getScopeDepth() );

		Set<ProgramState> assignResult = new HashSet<>();
		for( ProgramState resState : methodResult ) {

			JimpleExecutable resExec = (JimpleExecutable) resState;
			ConcreteValue concreteRHS = resExec.removeIntermediate( "@return" );

			invokePrepare.cleanHeap( resExec );

			if( concreteRHS.isUndefined() ){
				if( DebugMode.ENABLED ){
					logger.warn( "rhs evaluated to undefined (no return attached to heap)" );
				}
			}else{
				if( DebugMode.ENABLED && !( lhs.getType().equals( concreteRHS.type() ) ) ){
					String msg = "The type of the resulting ConcreteValue for rhs does not match ";
					msg += " with the type of the lhs";
					msg += "\n expected: " + lhs.getType() + " got: " + concreteRHS.type();
					logger.warn( msg );
				}
			}
			try {
				lhs.setValue( resExec, concreteRHS );
			} catch (NullPointerDereferenceException e) {
				logger.error(e.getErrorMessage(this));
			}
			
			JimpleExecutable freshExecutable = JimpleUtil.deepCopy(resExec);
			freshExecutable.setProgramCounter(nextPC);
			
			assignResult.add( freshExecutable );
		}
				
		return assignResult;
	}

	@Override
	public boolean needsMaterialization( ProgramState executable ){
		return invokePrepare.needsMaterialization( (JimpleExecutable) executable );
	}

	public String toString(){
		String res = lhs.toString() + " = " + method.toString() + "(";
		res += invokePrepare.argumentString();
		res += ");";
		return res;
	}

	@Override
	public boolean hasUniqueSuccessor() {

		return false;
	}
	
	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return invokePrepare.getPotentialViolationPoints();
	}

	@Override
	public Set<Integer> getSuccessorPCs() {
		
		return SingleElementUtil.createSet(nextPC);
	}
	
}