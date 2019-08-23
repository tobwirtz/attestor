package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.Nonterminal;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationBuilder;
import de.rwth.i2.attestor.graph.heap.NonterminalEdgeBuilder;
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
        invokePrepare.prepareHeap(preparedState);

        // in Case the index is out of bounds
        result.add(preparedState);

        HeapConfiguration heapConfig = preparedState.getHeap();


        // TODO For schleife: für jeden materialisierten Element-Knoten 1 Program state, in dem dieser entfernt wird. Falls Knoten eine ntEdge hat, dann diese vorher manuell konkretisieren

        // collect nodes, that have a nonterminal edge in the "next" direction
        TIntArrayList hasNonterminalEdgeNextDirection = new TIntArrayList();
        TIntArrayList hasNonterminalEdgePrevDirection = new TIntArrayList();
        int[] nTEdges = heapConfig.nonterminalEdges().toArray();
        for (int i : nTEdges
        ) {
            hasNonterminalEdgeNextDirection.add(heapConfig.attachedNodesOf(i).get(0));
            hasNonterminalEdgePrevDirection.add(heapConfig.attachedNodesOf(i).get(1));
        }


        // go through List and create new ProgramState for every Node that can be removed
        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }

        //TIntArrayList newNodes = new TIntArrayList();
        SelectorLabel next = scene().getSelectorLabel("next");


        System.out.println("Before (RemoveStmt):" + heapConfig);

        int prevNode = node;
        TIntArrayList visitedNodes = new TIntArrayList();
        node = getNextConcreteNodeInList(heapConfig, visitedNodes, node, next);

        while(node != heapConfig.variableTargetOf("null") && heapConfig.selectorTargetOf(node, next) != heapConfig.variableTargetOf("null")){
            // TODO beautify, avoid doubled code (use remove method from below in the first two ifs)

            if(heapConfig.selectorLabelsOf(node).contains(next) && heapConfig.selectorLabelsOf(prevNode).contains(next) && heapConfig.selectorTargetOf(prevNode, next) == node){
                // case when the previous and the next list-nodes are materialized
                HeapConfiguration copy = heapConfig.clone();
                copy.builder().removeSelector(prevNode, next)
                        .addSelector(prevNode, next, copy.selectorTargetOf(node, next))
                        .removeNode(node)
                        .build();
                result.add(preparedState.shallowCopyWithUpdateHeap(copy));
                // TODO is it necessary to remove variables/let them point to null?
            }else if(heapConfig.selectorLabelsOf(node).contains(next) && heapConfig.attachedNonterminalEdgesOf(node).size() == 1){
                // case when the next node is materialized and previous node was ntEdge
                int ntEdge = heapConfig.attachedNonterminalEdgesOf(node).get(0);

                HeapConfiguration copy = heapConfig.clone();

                // replace second tentacle of ntEdge with the successor of node
                replaceNtEdgeWithUpdatedTentacles(copy, ntEdge, copy.attachedNodesOf(ntEdge).get(0), heapConfig.selectorTargetOf(node, next));
                //copy.attachedNodesOf(ntEdge).replace(1, heapConfig.selectorTargetOf(node, next));
                copy.builder().removeNode(node);
                result.add(preparedState.shallowCopyWithUpdateHeap(copy));
            }else{
                // the next node is ntEdge
                int ntEdgeToBeMaterialized = heapConfig.attachedNonterminalEdgesOf(node).get(heapConfig.attachedNonterminalEdgesOf(node).size()-1);

                HeapConfiguration copyWithFirstRule = heapConfig.clone();
                HeapConfiguration copyWithSecondRule = heapConfig.clone();

                // materialize with first rule
                int nextConcreteNode = heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(node).get(0)).get(1);
                copyWithFirstRule.builder().removeNonterminalEdge(ntEdgeToBeMaterialized)
                        .addSelector(node, next, nextConcreteNode).build();

                // materialize with second rule
                TIntArrayList newNode = new TIntArrayList();
                copyWithSecondRule.builder().addNodes(scene().getType("java.util.LinkedList"), 1, newNode)
                        .removeSelector(node, next)
                        .addSelector(node, next, newNode.get(0))
                        .build();

                // replace first tentacle of ntEdge with the new node
                replaceNtEdgeWithUpdatedTentacles(copyWithSecondRule, ntEdgeToBeMaterialized, newNode.get(0), copyWithSecondRule.attachedNodesOf(ntEdgeToBeMaterialized).get(1));
                //copyWithSecondRule.attachedNodesOf(ntEdgeToBeMaterialized).replace(0, newNode.get(0));

                // remove nodes
                copyWithFirstRule = removeNodeWithConcreteSuccessor(copyWithFirstRule, node, prevNode, next);
                copyWithSecondRule = removeNodeWithConcreteSuccessor(copyWithSecondRule, node, prevNode, next);

                result.add(preparedState.shallowCopyWithUpdateHeap(copyWithFirstRule));
                result.add(preparedState.shallowCopyWithUpdateHeap(copyWithSecondRule));
            }
            // get to next concrete node in the list

            node = getNextConcreteNodeInList(heapConfig, visitedNodes, node, next);
        }

        // TODO handle removing the last node before null-node



        for(ProgramState p : result){
            invokePrepare.cleanHeap(p);
        }

        return result;
    }


    /**
     * Removes node from the list and connects the prevoius node with the successor node
     * node needs to have a materialized successor node
     * @param heapConfig
     * @param node node to be removed
     * @param prevnode node pointing with next selector to the node to be removed
     * @param next selectorlabel next
     * @return the heapconfiguration with the node removed from it
     */
    private HeapConfiguration removeNodeWithConcreteSuccessor(HeapConfiguration heapConfig, int node, int prevnode, SelectorLabel next){
        HeapConfiguration copy = heapConfig.clone();
        if(heapConfig.selectorLabelsOf(node).contains(next) && heapConfig.selectorLabelsOf(prevnode).contains(next) && heapConfig.selectorTargetOf(prevnode, next) == node){
            // case when the previous and the next list-nodes are materialized
            copy.builder().removeSelector(prevnode, next)
                    .addSelector(prevnode, next, copy.selectorTargetOf(node, next))
                    .removeNode(node)
                    .build();
            return copy;
            // TODO is it necessary to remove variables/let them point to null?
        }else if(heapConfig.selectorLabelsOf(node).contains(next) && heapConfig.attachedNonterminalEdgesOf(node).size() == 1){
            // case when the next node is materialized and previous node was ntEdge
            int ntEdge = heapConfig.attachedNonterminalEdgesOf(node).get(0);

            // replace second tentacle of ntEdge with the successor of node
            replaceNtEdgeWithUpdatedTentacles(copy, ntEdge, copy.attachedNodesOf(ntEdge).get(0), heapConfig.selectorTargetOf(node, next));
            //copy.attachedNodesOf(ntEdge).replace(1, heapConfig.selectorTargetOf(node, next));
            copy.builder().removeNode(node);
            return copy;
        }
        return null;
    }

    private int getNextConcreteNodeInList(HeapConfiguration hc, TIntArrayList visitedNodes, int currentNode, SelectorLabel next){
        visitedNodes.add(currentNode);
        if(hc.selectorLabelsOf(currentNode).contains(next)){
            currentNode = hc.selectorTargetOf(currentNode, next);
        }else{
            TIntArrayList ntEdges = hc.attachedNonterminalEdgesOf(currentNode);
            if(ntEdges.size() > 2){
                System.out.println("Input does not seem to be a List:" + hc);
            }
            for(int i : ntEdges.toArray()){
                TIntArrayList tentacles = hc.attachedNodesOf(i);
                for(int j : tentacles.toArray()){
                    if(!visitedNodes.contains(j)){
                        currentNode = j;
                        break;
                    }
                }
                if(!visitedNodes.contains(currentNode)){
                    break;
                }
            }
        }
        return currentNode;
    }


    private void replaceNtEdgeWithUpdatedTentacles(HeapConfiguration hc, int ntEdge, int t1, int t2){

        TIntArrayList newAttachedNodes = new TIntArrayList();
        newAttachedNodes.add(t1);
        newAttachedNodes.add(t2);

        hc.builder().addNonterminalEdge(hc.labelOf(ntEdge), newAttachedNodes)
                .removeNonterminalEdge(ntEdge)
                .build();
        return;
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
