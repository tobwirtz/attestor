package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleProgramState;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.JimpleUtil;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.ConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.SettableValue;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import de.rwth.i2.attestor.util.SingleElementUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * IdentityStmt models statements like x = {@literal @}this or x = {@literal @}param_1
 * @author Hannah Arndt
 *
 */
public class IdentityStmt extends Statement {

	private static final Logger logger = LogManager.getLogger( "IdentityStmt" );

	/**
	 * the program counter of the successor statement
	 */
	private final int nextPC;
	/**
	 * the value to which something will be assigned
	 */
	private final SettableValue lhs;
	/**
	 * the string representation of the argument that will be assigned,
	 * {@literal @}this, {@literal @}parameter_n
	 */
	private final String rhs;

	public IdentityStmt( int nextPC, SettableValue lhs, String rhs ){
		super();
		this.nextPC = nextPC;
		this.lhs = lhs;
		this.rhs = rhs.split( " " ) [0];
	}

	/**
	 * gets the value for the intermediate specified in {@link #rhs} from the heap 
	 * and assigns {@link #lhs} to it. Upon this the intermediate is deleted from the heap
	 * (i.e. this statement can only be called once per intermediate)
	 */
	@Override
	public Set<ProgramState> computeSuccessors( ProgramState programState )
			throws NotSufficientlyMaterializedException{

		JimpleProgramState jimpleProgramState = JimpleUtil.deepCopy( (JimpleProgramState) programState );

		ConcreteValue concreteRHS = jimpleProgramState.removeIntermediate( rhs );
		if( concreteRHS.isUndefined() ){
			logger.debug( rhs + " is not attached to the heap. (Continued by ignoring." );
		}else{
			if( !( lhs.getType().equals( concreteRHS.type() ) ) ){
				String msg = "The type of the resulting ConcreteValue for rhs does not match ";
				msg += " with the type of the lhs";
				msg += "\n expected: " + lhs.getType() + " got: " + concreteRHS.type();
				logger.debug( msg );
			}
		}
		try {
			lhs.setValue( jimpleProgramState, concreteRHS );
		} catch (NullPointerDereferenceException e) {
			logger.error(e.getErrorMessage(this));
		}
		
		return JimpleUtil.createSingletonAndUpdatePC(jimpleProgramState, nextPC);
	}

	@Override
	public boolean needsMaterialization( ProgramState programState ){
		return lhs.needsMaterialization( (JimpleProgramState) programState );
	}

	public String toString(){
		return lhs + " = " + rhs + ";";
	}

	@Override
	public boolean hasUniqueSuccessor() {
		
		return true;
	}
	
	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return lhs.getPotentialViolationPoints();
	}
	
	@Override
	public Set<Integer> getSuccessorPCs() {
		
		return SingleElementUtil.createSet(nextPC);
	}

}
