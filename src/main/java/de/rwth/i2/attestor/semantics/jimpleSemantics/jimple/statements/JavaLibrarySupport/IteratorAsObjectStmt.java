package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;


import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.*;
import de.rwth.i2.attestor.semantics.util.DeadVariableEliminator;
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
public class IteratorAsObjectStmt extends Statement {
    private static final Logger logger = LogManager.getLogger("IteratorAsObjectAssignInvoke");

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

    public IteratorAsObjectStmt(SceneObject sceneObject, SettableValue lhs, Value listBase, int nextPC, Set<String> liveVariableNames) {

        super(sceneObject);
        this.rhs = listBase;
        this.lhs = lhs;
        this.nextPC = nextPC;
        this.liveVariableNames = liveVariableNames;

        potentialViolationPoints = new ViolationPoints();
        potentialViolationPoints.addAll(lhs.getPotentialViolationPoints());
        potentialViolationPoints.addAll(listBase.getPotentialViolationPoints());
        potentialViolationPoints.add(listBase.toString(), "next");


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


        int baseNode;
        try{
            baseNode = ((GeneralConcreteValue) rhs.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            baseNode = -1;
        }

        // 1. get heap and add iterator Object node with "curr" selector to first element node of list

        TIntArrayList newNodes = new TIntArrayList();
        HeapConfiguration hc = programState.getHeap();
        hc.builder().addNodes(scene().getType("java.util.Iterator"), 1, newNodes)
                .addSelector(newNodes.get(0), scene().getSelectorLabel("curr"), baseNode)
                .build();

        int iteratorObjectNode = newNodes.get(0);

        programState = programState.shallowCopyWithUpdateHeap(hc);

        System.out.println("IteratorAsObject BEFORE: " + programState.getHeap());


        // 2. get concrete value of the Iterator-Object node
        GeneralConcreteValue concreteRHS = new GeneralConcreteValue(scene().getType("java.util.Iterator"), iteratorObjectNode);


        // 3. do same as in assign stmt
        try {
            lhs.evaluateOn(programState); // enforce materialization if necessary
            lhs.setValue(programState, concreteRHS);
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
        }

        System.out.println("IteratorAsObject AFTER: " + programState.getHeap());

        
        ProgramState result = programState.clone();
        result.setProgramCounter(nextPC);

        return SingleElementUtil.createSet(result);
    }


    public String toString() {

        return rhs.toString() + ".iterator()" + ";";
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
