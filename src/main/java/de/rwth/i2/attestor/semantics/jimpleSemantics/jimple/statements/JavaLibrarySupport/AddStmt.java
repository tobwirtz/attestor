package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InstanceInvokeHelper;
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

public class AddStmt extends Statement implements InvokeCleanup {

    private static final Logger logger = LogManager.getLogger("LinkedListAddStmt");

    /**
     * handles arguments, and if applicable the this-reference.
     */
    private final InvokeHelper invokePrepare;

    private final Value baseValue;

    /**
     * the program counter of the successor state
     */
    private final int nextPC;

    public AddStmt(SceneObject sceneObject, InvokeHelper invokePrepare, Value baseValue, int nextPC){
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
        //invokePrepare.prepareHeap(preparedState);

        HeapConfiguration heapConfig = preparedState.getHeap();


        // set node to head pointer and iterate to the end of the list
        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }


        TIntArrayList newNodes = new TIntArrayList();
        SelectorLabel next = scene().getSelectorLabel("next");
        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");


        System.out.println("Before (addStmt):" + heapConfig);

        TIntArrayList visitedNodes = new TIntArrayList();
        visitedNodes.add(node);


        // if list is empty, node stays headpointer, else node gets set to first element
        if(heapConfig.selectorTargetOf(node, getFirst) != heapConfig.variableTargetOf("null")){
            node = heapConfig.selectorTargetOf(node, getFirst);
        }


        while(MethodsToOperateOnLists.getNextConcreteNodeInList(heapConfig, visitedNodes, node, next, getFirst) != heapConfig.variableTargetOf("null")
                && !heapConfig.selectorLabelsOf(node).contains(getFirst)){

            node = MethodsToOperateOnLists.getNextConcreteNodeInList(heapConfig, visitedNodes, node, next, getFirst);

        }


        // In case between the last node and the null node is an ntEdge, the added node gets canonicalized by not adding it manually
        if(MethodsToOperateOnLists.getAttachedNtEdgeInNextDirection(node, heapConfig) == -1){

            SelectorLabel sel;
            if(heapConfig.selectorLabelsOf(node).contains(getFirst)) {
                sel = getFirst;
            }else{
                sel = next;
            }

            // add node at the end of the list
            heapConfig.builder()
                    .addNodes(scene().getType("java.util.LinkedList"), 1, newNodes)
                    .removeSelector(node, sel)
                    .addSelector(node, sel, newNodes.get(0))
                    .addSelector(newNodes.get(0), next, heapConfig.variableTargetOf("null"))
                    .build();

        }


        ProgramState result = preparedState.shallowCopyWithUpdateHeap(heapConfig);
        //invokePrepare.cleanHeap(result);
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
        return true;
    }
}
