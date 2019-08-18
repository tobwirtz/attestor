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
        invokePrepare.prepareHeap(preparedState);

        HeapConfiguration heapConfig = preparedState.getHeap();



        // collect nodes, that have a nonterminal edge in the "next" direction
        TIntArrayList hasNonterminalEdgeNextDirection = new TIntArrayList();
        TIntArrayList hasNonterminalEdgePrevDirection = new TIntArrayList();
        int[] nTEdges = heapConfig.nonterminalEdges().toArray();
        for (int i : nTEdges
             ) {
            hasNonterminalEdgeNextDirection.add(heapConfig.attachedNodesOf(i).get(0));
            hasNonterminalEdgePrevDirection.add(heapConfig.attachedNodesOf(i).get(1));
        }

        // get to the end of the list
        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }
        TIntArrayList newNodes = new TIntArrayList();
        SelectorLabel next = scene().getSelectorLabel("next");


        System.out.println("Before (addStmt):" + heapConfig);
        System.out.println("Set:" + hasNonterminalEdgeNextDirection.toString());

        while(heapConfig.selectorTargetOf(node, next) != heapConfig.variableTargetOf("null") && node != heapConfig.variableTargetOf("null")){
            System.out.println("Node before going to next:" + node);
            if(hasNonterminalEdgeNextDirection.contains(node)){
                if(hasNonterminalEdgePrevDirection.contains(heapConfig.variableTargetOf("null"))){
                    break;
                }
                node = heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(node).get(0)).get(1);
            }else if(heapConfig.selectorLabelsOf(node).contains(next)){
                node = heapConfig.selectorTargetOf(node, next);
            }else{
                System.out.println("Input auf der die Methode operieren soll scheint keine Liste zu sein");
                return null;
            }
        }

        if(!hasNonterminalEdgeNextDirection.contains(node)){
            // add node at the end of the list
            heapConfig.builder().addNodes(scene().getType("java.util.LinkedList"), 1, newNodes);
            heapConfig.builder().removeSelector(node, next);
            heapConfig.builder().addSelector(node, next, newNodes.get(0));
            heapConfig.builder().addSelector(newNodes.get(0), next, heapConfig.variableTargetOf("null"));
            heapConfig.builder().build();

        /* // this code works for a grammar, in which the last node of the list does not have a next pointer and is != null-node
        while(heapConfig.selectorLabelsOf(node).contains(next) || hasNonterminalEdgeNextDirection.contains(node)){

            if(heapConfig.selectorLabelsOf(node).contains(next)){
                node = heapConfig.selectorTargetOf(node, next);
            }else{
                node = heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(node).get(0)).get(1);
            }

        }

        // add node at end of list (the last node does not have a next pointer (!= null)
        heapConfig.builder().addNodes(scene().getType("java.util.LinkedList"), 1, newNodes);
        heapConfig.builder().addSelector(node, next, newNodes.get(0));
        heapConfig.builder().build();
*/
        }


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
        return true;
    }
}
