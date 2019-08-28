package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.GeneralConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.SettableValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class IteratorNextAsObjectStmt extends Statement {

    private static final Logger logger = LogManager.getLogger("IteratorNextAssignInvoke");

    /**
     * the element to which something will be assigned (e.g. variable or field)
     */
    private final SettableValue lhs;
    /**
     * The expression that will be assigned
     */
    private final Value rhs;
    /**
     * the program counter of the successor state
     */
    private final int nextPC;

    private final ViolationPoints potentialViolationPoints;

    private final Set<String> liveVariableNames;

    public IteratorNextAsObjectStmt(SceneObject sceneObject, SettableValue lhs, Value iteratorBase, int nextPC, Set<String> liveVariableNames) {

        super(sceneObject);
        this.rhs = iteratorBase;
        this.lhs = lhs;
        this.nextPC = nextPC;
        this.liveVariableNames = liveVariableNames;

        potentialViolationPoints = new ViolationPoints();
        potentialViolationPoints.addAll(lhs.getPotentialViolationPoints());
        potentialViolationPoints.addAll(iteratorBase.getPotentialViolationPoints());
        potentialViolationPoints.add(iteratorBase.toString(), "next");


    }

    /**
     * evaluates the rhs and assigns it to the left hand side. In case the rhs
     * evaluates to undefined, the variable will be removed from the heap (It
     * will not point to its old value). <br>
     * If the types of the lhs and the rhs do not match, there will be a
     * warning, but the assignment will still be realized.<br>
     * <p>
     * If the variable in rhs is not live in this statement, it will be removed from the heap
     * to enable abstraction at this point.
     */
    @Override
    public Collection<ProgramState> computeSuccessors(ProgramState programState) {

        programState = programState.clone();


        SelectorLabel next = scene().getSelectorLabel("next");
        SelectorLabel curr = scene().getSelectorLabel("curr");

        int iteratorNode;
        try{
            iteratorNode = ((GeneralConcreteValue) rhs.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            iteratorNode = -1;
        }


        // set node to curr
        HeapConfiguration hc = programState.getHeap();
        int node = hc.selectorTargetOf(iteratorNode, curr);


        // get HeapConfigurations with concrete successors of curr
        Set<HeapConfiguration> HeapConfigs = new LinkedHashSet<>();

        if(hc.selectorLabelsOf(node).contains(next) || node == hc.variableTargetOf("null")){

            HeapConfigs.add(hc.clone());

        }else{

            // materialize
            HeapConfigs.addAll(MethodsToOperateOnLists.materializeFollowingNtEdgeManually(hc, node, next, scene().getType("java.util.LinkedList")));

        }

        // set curr to curr.next and add to result
        Set<ProgramState> result = new LinkedHashSet<>();
        for(HeapConfiguration heap : HeapConfigs){

            int newCurr = node;

            if(node != heap.variableTargetOf("null")){
                newCurr = heap.selectorTargetOf(node, next);
                heap.builder()
                        .removeSelector(iteratorNode, curr)
                        .addSelector(iteratorNode, curr, newCurr)
                        .build();
            }
            ProgramState p = programState.shallowCopyWithUpdateHeap(heap);

            // get concrete value of the curr node
            GeneralConcreteValue concreteRHS = new GeneralConcreteValue(scene().getType("java.util.LinkedList"), newCurr);


            // do same as in assign stmt
            try {
                lhs.evaluateOn(p); // enforce materialization if necessary
                lhs.setValue(p, concreteRHS);
            } catch (NullPointerDereferenceException e) {
                logger.error(e.getErrorMessage(this));
            }

            p = p.clone();

            p.setProgramCounter(nextPC);
            result.add(p);
        }

        // TODO Think about eliminating dead variables


        return result;
    }


    public String toString() {

        return rhs.toString() + ".next()" + ";";
    }

    @Override
    public ViolationPoints getPotentialViolationPoints() {

        return potentialViolationPoints;
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
