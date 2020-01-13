package de.rwth.i2.attestor.phases.symbolicExecution.utilStrategies;

import de.rwth.i2.attestor.programState.defaultState.ExceptionProgramState;
import de.rwth.i2.attestor.semantics.TerminalStatement;
import de.rwth.i2.attestor.stateSpaceGeneration.FinalStateStrategy;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.SemanticsCommand;

import java.util.Collection;

public class TerminalStatementFinalStateStrategy implements FinalStateStrategy {

    @Override
    public boolean isFinalState(ProgramState state, Collection<ProgramState> successorStates,
                                SemanticsCommand semanticsCommand) {

        // IMPORTANT: The return below suggests that an occurring Exception is not caught! If try/catch blocks are to be used, this might need fixing.
        return (successorStates.isEmpty() && isTerminalStatement(semanticsCommand)) || state instanceof ExceptionProgramState;
    }

    private boolean isTerminalStatement(SemanticsCommand semanticsCommand) {

        return semanticsCommand.getClass() == TerminalStatement.class;
    }
}
