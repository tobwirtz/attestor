package de.rwth.i2.attestor.main.phases.transformers;

import de.rwth.i2.attestor.graph.heap.HeapConfiguration;

import java.util.List;

public interface InputTransformer {

    List<HeapConfiguration> getInputs();
}
