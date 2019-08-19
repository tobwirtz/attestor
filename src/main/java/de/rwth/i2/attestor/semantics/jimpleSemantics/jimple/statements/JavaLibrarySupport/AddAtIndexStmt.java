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
        invokePrepare.prepareHeap(preparedState);

        // in Case the index refers to a node, that is not materialized yet
        // in this case the abstraction after the statement will yield the same ProgramState as before
        result.add(preparedState);

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

        System.out.println("Before (AddAtIndexStmt):" + heapConfig);

        TIntArrayList visitedNodes = new TIntArrayList();
        while(node != heapConfig.variableTargetOf("null")){

            // add node and add resulting state to result
            HeapConfiguration newHC = heapConfig.clone();
            newHC = insertElementIntoListAtNextPosition(newHC, node, next);
            result.add(preparedState.shallowCopyWithUpdateHeap(newHC));

            // continue iterating through original list
            node = getNextConcreteNodeInList(heapConfig, visitedNodes, node, next);
        }


        for(ProgramState p : result){
            invokePrepare.cleanHeap(p);
        }

        return result;
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



    private HeapConfiguration insertElementIntoListAtNextPosition(HeapConfiguration hc, int node, SelectorLabel next){

        TIntArrayList newNodes = new TIntArrayList();

        if(hc.selectorLabelsOf(node).contains(next)){

            int followingNode = hc.selectorTargetOf(node,next);

            hc.builder().addNodes(scene().getType("java.util.LinkedList"), 1, newNodes)
                    .removeSelector(node, next)
                    .addSelector(node, next, newNodes.get(0))
                    .addSelector(newNodes.get(0), next, followingNode)
                    .build();

        }else{
            // TODO as soon as you know how you can modify ntEdges
        }
        return hc;
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
