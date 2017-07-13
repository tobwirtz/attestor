package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements;


import de.rwth.i2.attestor.stateSpaceGeneration.Semantics;

/**
 * Statements are {@link de.rwth.i2.attestor.stateSpaceGeneration.Semantics Sementics}
 * with {@link de.rwth.i2.attestor.semantics.jimpleSemantics.JimpleExecutable JimpleExecutable} 
 * as heaps.
 * @author Hannah Arndt
 *
 */
public abstract class Statement implements Semantics {

	/**
	 * True if and only if canonicalization should be performed
	 * immediately after executing this statement.
	 */
	private boolean isCanonicalizationPermitted = true;

    /**
     *
     * @return True if and only if canonicalization should be performed
     *         immediately after executing this statement.
     */
	public boolean permitsCanonicalization() {
		
		return isCanonicalizationPermitted;
	}

    /**
     * @param permitted True if and only if canonicalization should be performed
     *                  immediately after executing this statement.
     */
	public void setPermitCanonicalization(boolean permitted) {
		
		this.isCanonicalizationPermitted = permitted;
	}
}