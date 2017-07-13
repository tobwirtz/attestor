package de.rwth.i2.attestor.graph.morphism;

/**
 * A FeasibilityFunction is a condition that is evaluated by a {@link VF2Algorithm} in order to determine
 * whether extending a {@link VF2State} by a {@link CandidatePair} might still correspond to a valid
 * graph morphism.
 *
 * @author Christoph
 *
 */
public interface FeasibilityFunction {

	/**
	 * Evaluates whether extending the given state by a candidate pair results in a partial mapping between
	 * pattern and target Graph might still lead to a valid Morphism.
	 * @param state The current VF2State determining the partial Morphism computed so far.
	 * @param candidate The new CandidatePair determining the pattern-target pair that should be added to the morphism.
	 * @return True if and only if adding the candidate pair to the current state of the morphism might still lead to a 
	 *         valid Morphism.
	 */
	boolean eval(VF2State state, CandidatePair candidate);
	
}
