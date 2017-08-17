package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values;

import de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.ViolationPoints;
import de.rwth.i2.attestor.types.Type;
import de.rwth.i2.attestor.util.NotSufficientlyMaterializedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Locals represent local variables
 * @author Hannah Arndt
 *
 */
public class Local implements SettableValue {

	private static final Logger logger = LogManager.getLogger( "Local" );

	/**
	 * the expected type
	 */
	private final Type type;
	/**
	 * the name of the local variable
	 */
	private final String name;

	public Local( Type type, String name ){
		this.type = type;
		this.name = name;

	}

	public String getName(){
		return this.name;
	}

	public Type getType(){
		return this.type;
	}

	@Override
	public ConcreteValue evaluateOn( JimpleProgramState programState ) throws NotSufficientlyMaterializedException{

		ConcreteValue res = programState.getVariableTarget( this.getName() );
		
		if( !( this.type.equals( res.type() ) ) ){
			
			String msg = "The type of the resulting ConcreteValue ";
			msg += this.getName();
			msg += " does not match.";
			msg += "\n expected: " + this.type + " got: " + res.type();
			logger.debug( msg );
		}

		return res;
	}

	/**
	 * sets the variable in programState to concreteTarget
	 */
	@Override
	public void setValue(JimpleProgramState programState, ConcreteValue concreteTarget )
			throws NotSufficientlyMaterializedException{

		programState.setVariable( this.getName(), concreteTarget );
	}

	@Override
	public boolean needsMaterialization( JimpleProgramState programState ){
		return false;
	}


	public String toString(){
		return name;
	}

	@Override
	public ViolationPoints getPotentialViolationPoints() {
		
		return ViolationPoints.getEmptyViolationPoints();
	}

}
