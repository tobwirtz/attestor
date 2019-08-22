package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeCleanup;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.GeneralConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;
import gnu.trove.list.array.TIntArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Set;

/**
 * @author Tobias
 */

public class LinkedListInitStmt extends Statement implements InvokeCleanup {

    private static final Logger logger = LogManager.getLogger("LinkedListInitStmt");

    /**
     * handles arguments, and if applicable the this-reference.
     */
    private final InvokeHelper invokePrepare;

    private final Value baseValue;

    /**
     * the program counter of the successor state
     */
    private final int nextPC;

    public LinkedListInitStmt(SceneObject sceneObject, InvokeHelper invokePrepare, Value baseValue, int nextPC){
        super(sceneObject);
        this.invokePrepare = invokePrepare;
        this.baseValue = baseValue;
        this.nextPC = nextPC;
    }

    @Override
    public ProgramState getCleanedResultState(ProgramState state) {
        invokePrepare.cleanHeap(state);
        return state;
    }

    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState) {

        ProgramState preparedState = programState.clone();
        invokePrepare.prepareHeap(preparedState);

        HeapConfiguration heapConfig = preparedState.getHeap();

        System.out.println("Before init:" + heapConfig);

        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }

        SelectorLabel next = scene().getSelectorLabel("next");
/*
        heapConfig.builder().addSelector(node, next, heapConfig.variableTargetOf("null"));
        heapConfig.builder().build();

 */
/*

        System.out.println("After init:" + heapConfig);

*/
        ProgramState result = preparedState.shallowCopyWithUpdateHeap(heapConfig);
        invokePrepare.cleanHeap(result);
        result.setProgramCounter(nextPC);

        return SingleElementUtil.createSet(result);
    }

    @Override
    public ViolationPoints getPotentialViolationPoints() {
        return invokePrepare.getPotentialViolationPoints();
    }

    @Override
    public Set<Integer> getSuccessorPCs() {
        return SingleElementUtil.createSet(nextPC);
    }

    @Override
    public boolean needsCanonicalization() {
        return false;
    }
}
