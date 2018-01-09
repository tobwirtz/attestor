package de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies;

import de.rwth.i2.attestor.grammar.languageInclusion.LanguageInclusionStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.PostProcessingStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateCanonicalizationStrategyWrapper;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FinalStateSubsumptionPostProcessingStrategy implements PostProcessingStrategy {

    private StateCanonicalizationStrategyWrapper canonicalizationStrategy;
    private LanguageInclusionStrategy languageInclusionStrategy;
    private int minAbstractionDistance;

    public FinalStateSubsumptionPostProcessingStrategy(StateCanonicalizationStrategyWrapper canonicalizationStrategy,
                                                       LanguageInclusionStrategy languageInclusionStrategy,
                                                       int minAbstractionDistance) {

        this.canonicalizationStrategy = canonicalizationStrategy;
        this.languageInclusionStrategy = languageInclusionStrategy;
        this.minAbstractionDistance = minAbstractionDistance;
    }

    @Override
    public void process(StateSpace stateSpace) {

        if (minAbstractionDistance == 0) {
            return;
        }

        if (stateSpace.getFinalStateIds().size() == 1) {
            return;
        }

        Set<ProgramState> finalStates = stateSpace.getFinalStates();

        Set<ProgramState> fullyAbstractStates = new LinkedHashSet<>();
        Map<Integer, Integer> idMap = new LinkedHashMap<>();

        for (ProgramState state : finalStates) {
            ProgramState absState = canonicalizationStrategy.canonicalize(state);
            absState.setStateSpaceId(state.getStateSpaceId());
            ProgramState oldState = addIfAbsent(absState, fullyAbstractStates);

            if (oldState != null) {
                idMap.put(state.getStateSpaceId(), oldState.getStateSpaceId());
            } else {
                idMap.put(state.getStateSpaceId(), absState.getStateSpaceId());
            }
        }

        if (fullyAbstractStates.size() < finalStates.size()) {
            stateSpace.updateFinalStates(fullyAbstractStates, idMap);
        }
    }

    private ProgramState addIfAbsent(ProgramState absState, Set<ProgramState> abstractedStates) {

        for (ProgramState state : abstractedStates) {
            if(absState.getProgramCounter() == state.getProgramCounter()
                    && languageInclusionStrategy.includes(absState.getHeap(), state.getHeap())) {
                return state;
            }
        }

        abstractedStates.add(absState);
        return null;
    }
}