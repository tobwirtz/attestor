package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeCleanup;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;
import gnu.trove.list.array.TIntArrayList;

import java.util.Collection;
import java.util.Set;

/**
 * @author Tobias
 */

public class AppendOneElementDummyStmt extends Statement implements InvokeCleanup {

    /**
     * handles arguments, and if applicable the this-reference.
     */
    private final InvokeHelper invokePrepare;

    /**
     * the program counter of the successor state
     */
    private final int nextPC;


    public AppendOneElementDummyStmt(SceneObject sceneObject, InvokeHelper invokePrepare, int nextPC){

        super(sceneObject);
        this.invokePrepare = invokePrepare;
        this.nextPC = nextPC;
    }

    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState) {


        ProgramState preparedState = programState.clone();
        //invokePrepare.prepareHeap(preparedState);

        HeapConfiguration heapConfig = preparedState.getHeap();


        // collect nodes, that have a nonterminal edge in the "next" direction
        TIntArrayList hasNonterminalEdge = new TIntArrayList();
        int[] nTEdges = heapConfig.nonterminalEdges().toArray();
        for (int i : nTEdges
             ) {
            hasNonterminalEdge.add(heapConfig.attachedNodesOf(i).get(0));
        }

        // get to the end of the list
        int node = heapConfig.variableTargetOf(invokePrepare.argumentString());
        TIntArrayList newNodes = new TIntArrayList();
        SelectorLabel next = scene().getSelectorLabel("next");


        System.out.println("Before:" + heapConfig);
        System.out.println("Set:" + hasNonterminalEdge.toString());


        while(heapConfig.selectorTargetOf(node, next) != heapConfig.variableTargetOf("null")  /* !(hasNonterminalEdge.contains(node) && heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(node).get(0)).get(1) == heapConfig.variableTargetOf("null"))*/){

            // case when nonterminal has edge to null node
            if((hasNonterminalEdge.contains(node) && heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(node).get(0)).get(1) == heapConfig.variableTargetOf("null"))){
                invokePrepare.cleanHeap(preparedState);
                preparedState.setProgramCounter(nextPC);
                return SingleElementUtil.createSet(preparedState);
            }

            System.out.println("Node before going to next:" + node);
            if(hasNonterminalEdge.contains(node)){
                node = heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(node).get(0)).get(1);
            }else if(heapConfig.selectorLabelsOf(node).contains(next)){
                node = heapConfig.selectorTargetOf(node, next);
            }else{
                System.out.println("Input auf der die Methode operieren soll scheint keine Liste zu sein");
                return null;
            }
        }

        // add node at the end of the list (Type is important for canonicalization)
        heapConfig.builder().addNodes(scene().getType("SLList"), 1, newNodes);
        heapConfig.builder().removeSelector(node, next);
        heapConfig.builder().addSelector(node, next, newNodes.get(0));
        heapConfig.builder().addSelector(newNodes.get(0), next, heapConfig.variableTargetOf("null"));
        heapConfig.builder().build();

        //int[] nodes = heapConfig.nodes().toArray();
        //String test = heapConfig.nodeTypeOf(nodes[0]).toString();
        System.out.println("After:" + heapConfig);
        System.out.println("argument: " + invokePrepare.argumentString() + " Node: " + node);

        ProgramState result = preparedState.shallowCopyWithUpdateHeap(heapConfig);
        invokePrepare.cleanHeap(result);
        result.setProgramCounter(nextPC);

        return SingleElementUtil.createSet(result);
    }

    public String toString() {

        return "<SLList: void appendOneElement(SLList)>;";
                //invokePrepare.baseValueString() + method.toString() + "(" + invokePrepare.argumentString() + ");";
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

    @Override
    public ProgramState getCleanedResultState(ProgramState state) {

        invokePrepare.cleanHeap(state);
        return state;
    }
}
