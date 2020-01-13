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
import org.jboss.util.Heap;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Tobias
 */


public class RemoveIndexStmt extends Statement implements InvokeCleanup {

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

    public RemoveIndexStmt(SceneObject sceneObject, InvokeHelper invokePrepare, Value baseValue, int nextPC){
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
    public Set<ProgramState> computeSuccessors(ProgramState programState) {

        Set<ProgramState> result = new LinkedHashSet<>();

        ProgramState preparedState = programState.shallowCopyUpdatePC(nextPC);
        //invokePrepare.prepareHeap(preparedState);


        HeapConfiguration heapConfig = preparedState.getHeap();


        /* collect nodes, that have a nonterminal edge in the "next" or "prev" direction
        TIntArrayList hasNonterminalEdgeNextDirection = new TIntArrayList();
        TIntArrayList hasNonterminalEdgePrevDirection = new TIntArrayList();
        int[] nTEdges = heapConfig.nonterminalEdges().toArray();
        for (int i : nTEdges
        ) {
            hasNonterminalEdgeNextDirection.add(heapConfig.attachedNodesOf(i).get(0));
            hasNonterminalEdgePrevDirection.add(heapConfig.attachedNodesOf(i).get(1));
        }
         */

        // Case when index is out of bounds
        result.add(new ExceptionProgramState(programState.getHeap().clone(), "IndexOutOfBoundsException"));

        if(!heapConfig.nonterminalEdges().isEmpty()){
            // handles case, when the node to be removed is abstracted
            result.add(preparedState.clone());
        }


        // set node to base value
        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }

        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");
        SelectorLabel next = scene().getSelectorLabel("next");


        System.out.println("Before (RemoveStmt):" + heapConfig);


        // go through List and create new ProgramState for every Node that can be removed

        int prevNode = node;
        TIntArrayList visitedNodes = new TIntArrayList();
        node = MethodsToOperateOnLists.getNextConcreteNodeInList(heapConfig, visitedNodes, node, next, getFirst);

        while(node != heapConfig.variableTargetOf("null")){


            if(heapConfig.selectorLabelsOf(node).contains(next)){
                // case when node has a concrete successor
                HeapConfiguration copy = heapConfig.clone();
                if(copy.selectorLabelsOf(prevNode).contains(getFirst)){
                    copy = removeNodeWithConcreteSuccessor(copy, node, prevNode, next, getFirst);
                }else{
                    copy = removeNodeWithConcreteSuccessor(copy, node, prevNode, next, next);
                }

                result.add(preparedState.shallowCopyWithUpdateHeap(copy));

            }else{

                Set<HeapConfiguration> materializedHeaps = MethodsToOperateOnLists.materializeFollowingNtEdgeManually(heapConfig, node, next, scene().getType("java.util.LinkedList"));
                HeapConfiguration copyWithFirstRule = (HeapConfiguration) materializedHeaps.toArray()[0];
                HeapConfiguration copyWithSecondRule = (HeapConfiguration) materializedHeaps.toArray()[1];
                /*
                // the next node is ntEdge
                int ntEdgeToBeMaterialized = MethodsToOperateOnLists.getAttachedNtEdgeInNextDirection(node, heapConfig);

                HeapConfiguration copyWithFirstRule = heapConfig.clone();
                HeapConfiguration copyWithSecondRule = heapConfig.clone();

                // materialize with first rule
                int nextConcreteNode = heapConfig.attachedNodesOf(ntEdgeToBeMaterialized).get(1);
                copyWithFirstRule.builder().removeNonterminalEdge(ntEdgeToBeMaterialized)
                        .addSelector(node, next, nextConcreteNode).build();

                // materialize with second rule
                TIntArrayList newNode = new TIntArrayList();
                copyWithSecondRule.builder().addNodes(scene().getType("java.util.LinkedList"), 1, newNode)
                        .removeSelector(node, next)
                        .addSelector(node, next, newNode.get(0))
                        .build();

                // replace first tentacle of ntEdge with the new node
                MethodsToOperateOnLists.replaceNtEdgeWithUpdatedTentacles(copyWithSecondRule, ntEdgeToBeMaterialized, newNode.get(0), copyWithSecondRule.attachedNodesOf(ntEdgeToBeMaterialized).get(1));


                 */

                // remove nodes
                if(heapConfig.selectorLabelsOf(prevNode).contains(getFirst)){
                    copyWithFirstRule = removeNodeWithConcreteSuccessor(copyWithFirstRule, node, prevNode, next, getFirst);
                    copyWithSecondRule = removeNodeWithConcreteSuccessor(copyWithSecondRule, node, prevNode, next, getFirst);
                }else {
                    copyWithFirstRule = removeNodeWithConcreteSuccessor(copyWithFirstRule, node, prevNode, next, next);
                    copyWithSecondRule = removeNodeWithConcreteSuccessor(copyWithSecondRule, node, prevNode, next, next);
                }

                result.add(preparedState.shallowCopyWithUpdateHeap(copyWithFirstRule));
                result.add(preparedState.shallowCopyWithUpdateHeap(copyWithSecondRule));
            }

            // get to next concrete node in the list
            prevNode = node;
            node = MethodsToOperateOnLists.getNextConcreteNodeInList(heapConfig, visitedNodes, node, next, getFirst);

        }

/*
        for(ProgramState p : result){
            invokePrepare.cleanHeap(p);
        }
*/
        return result;
    }


    /**
     * Removes node from the list and connects the prevoius node with the successor node
     * node needs to have a materialized successor node
     * @param heapConfig the HeapConfiguration on which the node is supposed to be removed
     * @param node node to be removed
     * @param prevnode node pointing with prevnodeSel to the node to be removed
     * @param next selectorlabel next
     * @return the heapconfiguration with the node removed from it
     */
    private HeapConfiguration removeNodeWithConcreteSuccessor(HeapConfiguration heapConfig, int node, int prevnode, SelectorLabel next, SelectorLabel prevnodeSel){
        HeapConfiguration copy = heapConfig.clone();
        if(heapConfig.selectorLabelsOf(node).contains(next)
                && heapConfig.selectorLabelsOf(prevnode).contains(prevnodeSel)
                && heapConfig.selectorTargetOf(prevnode, prevnodeSel) == node){
            // case when the previous and the next list-nodes are materialized
            copy.builder().removeSelector(prevnode, prevnodeSel)
                    .addSelector(prevnode, prevnodeSel, copy.selectorTargetOf(node, next))
                    .removeNode(node)
                    .build();
            return copy;
            // TODO is it necessary to remove variables/let them point to null?
        }else if(heapConfig.selectorLabelsOf(node).contains(next) && heapConfig.attachedNonterminalEdgesOf(node).size() == 1){
            // case when the next node is materialized and previous node was ntEdge
            int ntEdge = heapConfig.attachedNonterminalEdgesOf(node).get(0);

            // replace second tentacle of ntEdge with the successor of node
            MethodsToOperateOnLists.replaceNtEdgeWithUpdatedTentacles(copy, ntEdge, copy.attachedNodesOf(ntEdge).get(0), heapConfig.selectorTargetOf(node, next));
            //copy.attachedNodesOf(ntEdge).replace(1, heapConfig.selectorTargetOf(node, next));
            copy.builder()
                    .removeNode(node)
                    .build();
            return copy;
        }
        return null;
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
