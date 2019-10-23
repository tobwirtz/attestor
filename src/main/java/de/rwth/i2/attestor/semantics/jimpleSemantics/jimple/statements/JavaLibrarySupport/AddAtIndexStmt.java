package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.programState.defaultState.ExceptionProgramState;
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Tobias
 */


public class AddAtIndexStmt extends Statement implements InvokeCleanup {

    private static final Logger logger = LogManager.getLogger("LinkedListRemoveIndexStmt");

    /**
     * handles arguments, and if applicable the this-reference.
     */
    private final InvokeHelper invokePrepare;

    private final Value baseValue;

    /**
     * the program counter of the successor state
     */
    private final int nextPC;

    public AddAtIndexStmt(SceneObject sceneObject, InvokeHelper invokePrepare, Value baseValue, int nextPC){
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


    /**
     * iterates over the list and nondeterministically returns one ProgramState for each index where the
     * element to be added gets inserted as the next element
     * @param programState The state on which the abstract program semantics shall be executed.
     * @return All ProgramStates that could result in the add operation without knowing the index
     */
    @Override
    public Set<ProgramState> computeSuccessors(ProgramState programState) {

        Set<ProgramState> result = new LinkedHashSet<>();

        ProgramState preparedState = programState.shallowCopyUpdatePC(nextPC);
        //invokePrepare.prepareHeap(preparedState);

        // in Case the index refers to a node, that is not materialized yet
        // in this case the abstraction after the statement will yield the same ProgramState as before
        if(preparedState.getHeap().nonterminalEdges().size() > 0) {
            result.add(preparedState);
        }

        // case when Index is out of Bounds
        result.add(new ExceptionProgramState(preparedState.getHeap().clone(), "IndexOutOfBoundsException"));

        HeapConfiguration heapConfig = preparedState.getHeap();


        // go through List and create new ProgramState for every position the index could point to
        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }

        SelectorLabel next = scene().getSelectorLabel("next");
        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");

        System.out.println("Before (AddAtIndexStmt):" + heapConfig);

        TIntArrayList visitedNodes = new TIntArrayList();

        // add node at index 0 and add resulting state to result
        HeapConfiguration newHC = heapConfig.clone();
        MethodsToOperateOnLists.insertElementIntoListAtNextPosition(newHC, node, getFirst, next, scene().getType("java.util.LinkedList"));
        result.add(preparedState.shallowCopyWithUpdateHeap(newHC));

        node = MethodsToOperateOnLists.getNextConcreteNodeInList(heapConfig, visitedNodes, node, next, getFirst);

        // add nodes at all other positions
        while(node != heapConfig.variableTargetOf("null")){

            // add node and add resulting state to result
            newHC = heapConfig.clone();
            MethodsToOperateOnLists.insertElementIntoListAtNextPosition(newHC, node, next, next, scene().getType("java.util.LinkedList"));
            result.add(preparedState.shallowCopyWithUpdateHeap(newHC));

            // continue iterating through original list
            node = MethodsToOperateOnLists.getNextConcreteNodeInList(heapConfig, visitedNodes, node, next, getFirst);
        }

/*
        for(ProgramState p : result){
            invokePrepare.cleanHeap(p);
        }
*/
        return result;
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
        return true;
    }
}
