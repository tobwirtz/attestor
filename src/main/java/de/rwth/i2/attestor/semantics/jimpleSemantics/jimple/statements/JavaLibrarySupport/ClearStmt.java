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
import gnu.trove.procedure.TIntProcedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Set;

public class ClearStmt extends Statement implements InvokeCleanup {

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

    public ClearStmt(SceneObject sceneObject, InvokeHelper invokePrepare, Value baseValue, int nextPC){
        super(sceneObject);
        this.invokePrepare = invokePrepare;
        this.baseValue = baseValue;
        this.nextPC = nextPC;
    }

    /**
     * Executes a single step of the abstract program semantics on the given program state.
     * Since the abstract program semantics may be non-deterministic (for example if a conditional statement cannot
     * be evaluated), this results in a set of successor program states in general.
     *
     * @param programState The state on which the abstract program semantics shall be executed.
     * @return All states resulting from executing the program semantics on programState.
     */
    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState) {

        ProgramState preparedState = programState.clone();
        //invokePrepare.prepareHeap(preparedState);

        final HeapConfiguration heapConfig = preparedState.getHeap();



        // collect nodes, that have a nonterminal edge in the "next" direction
        TIntArrayList hasNonterminalEdge = new TIntArrayList();
        int[] nTEdges = heapConfig.nonterminalEdges().toArray();
        for (int i : nTEdges
             ) {
            hasNonterminalEdge.add(heapConfig.attachedNodesOf(i).get(0));
        }

        // rearrange first next pointer to point to null-node
        int node;
        try{
            node = ((GeneralConcreteValue) baseValue.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }

        System.out.println("Before:" + heapConfig);
        System.out.println("Set:" + hasNonterminalEdge.toString());

        SelectorLabel next = scene().getSelectorLabel("next");

        int nodeToBeRemoved = heapConfig.selectorTargetOf(node, next);
        heapConfig.builder()
                .removeSelector(node, next)
                .addSelector(node, next, heapConfig.variableTargetOf("null"))
                .build();


        // collect nodes of the old list

        TIntArrayList nodesToBeRemoved = new TIntArrayList();

        while(nodeToBeRemoved != heapConfig.variableTargetOf("null")){
            System.out.println("Node before going to next:" + nodeToBeRemoved);
            nodesToBeRemoved.add(nodeToBeRemoved);
            if(hasNonterminalEdge.contains(nodeToBeRemoved)){
                nodeToBeRemoved = heapConfig.attachedNodesOf(heapConfig.attachedNonterminalEdgesOf(nodeToBeRemoved).get(0)).get(1);
            }else if(heapConfig.selectorLabelsOf(nodeToBeRemoved).contains(next)){
                nodeToBeRemoved = heapConfig.selectorTargetOf(nodeToBeRemoved, next);
            }else{
                System.out.println("Input of ClearStmt method does not seem to be a List");
                return null;
            }
        }

        // delete old nodes
        nodesToBeRemoved.forEach(new TIntProcedure() {
                                     @Override
                                     public boolean execute(int i) {
                                         heapConfig.builder()
                                                 .removeNode(i)
                                                 .build();
                                         return true;
                                     }
                                 }
        );

        System.out.println("After:" + heapConfig);

        ProgramState result = preparedState.shallowCopyWithUpdateHeap(heapConfig);
        //invokePrepare.cleanHeap(result);
        result.setProgramCounter(nextPC);

        return SingleElementUtil.createSet(result);
    }

    /**
     * @return All potential violation points that may prevent execution of this statement.
     */
    @Override
    public ViolationPoints getPotentialViolationPoints() {
        return invokePrepare.getPotentialViolationPoints();
    }

    /**
     * @return The set of all program locations that are direct successors of this program statement in
     * the underlying control flow graph.
     */
    @Override
    public Set<Integer> getSuccessorPCs() {
        return SingleElementUtil.createSet(nextPC);
    }

    /**
     * @return true, if the statement always requires canonicalization
     */
    @Override
    public boolean needsCanonicalization() {
        return false;
    }

    @Override
    public ProgramState getCleanedResultState(ProgramState state) {
        invokePrepare.cleanHeap(state);
        return state;
    }
}
