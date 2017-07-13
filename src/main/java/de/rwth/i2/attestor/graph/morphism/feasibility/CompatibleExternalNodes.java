package de.rwth.i2.attestor.graph.morphism.feasibility;

import de.rwth.i2.attestor.graph.morphism.CandidatePair;
import de.rwth.i2.attestor.graph.morphism.FeasibilityFunction;
import de.rwth.i2.attestor.graph.morphism.Graph;
import de.rwth.i2.attestor.graph.morphism.VF2State;

/**
 * Checks whether the nodes in the provided candidate pair either correspond to the same position
 * in the respective sequence of external nodes or are both not external at all.
 *
 * @author Christoph
 */
public class CompatibleExternalNodes implements FeasibilityFunction {

	@Override
	public boolean eval(VF2State state, CandidatePair candidate) {
		
		Graph patternGraph = state.getPattern().getGraph();
		Graph targetGraph = state.getTarget().getGraph();
		int p = candidate.p;
		int t = candidate.t;
		
		if(patternGraph.isExternal(p) || targetGraph.isExternal(t)) {
			if(patternGraph.getExternalIndex(p) != targetGraph.getExternalIndex(t)) {
				return false;
			}
		}
		
		return true;
	}

}
