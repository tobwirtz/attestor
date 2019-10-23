package de.rwth.i2.attestor.programState.defaultState;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

public class ExceptionProgramState extends DefaultProgramState {

    public String exceptionMessage;

    /**
     * Initializes a program state.
     *
     * @param heap The underlying heap configuration.
     */
    public ExceptionProgramState(HeapConfiguration heap, String exceptionMessage) {
        super(heap);
        this.exceptionMessage = exceptionMessage;
        super.addAP(exceptionMessage);
    }
}
