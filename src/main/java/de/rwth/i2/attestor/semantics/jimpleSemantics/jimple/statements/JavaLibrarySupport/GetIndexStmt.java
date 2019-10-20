package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;


import de.rwth.i2.attestor.grammar.materialization.util.ViolationPoints;
import de.rwth.i2.attestor.graph.BasicNonterminal;
import de.rwth.i2.attestor.graph.Nonterminal;
import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.GeneralConcreteValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.NullPointerDereferenceException;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.SettableValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.stateSpaceGeneration.Program;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.SingleElementUtil;
import gnu.trove.list.array.TIntArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Tobias
 */
public class GetIndexStmt extends Statement {
    private static final Logger logger = LogManager.getLogger("GetIndexAssignInvoke");

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
    // TODO remove unused variable liveVariableNames

    public GetIndexStmt(SceneObject sceneObject, SettableValue lhs, Value listBase, int nextPC, Set<String> liveVariableNames) {

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
        Set<ProgramState> result = new HashSet<>();

        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");
        SelectorLabel next = scene().getSelectorLabel("next");

        int node;
        try{
            node = ((GeneralConcreteValue) rhs.evaluateOn(programState)).getNode();
        } catch (NullPointerDereferenceException e) {
            logger.error(e.getErrorMessage(this));
            node = -1;
        }

        // iterate over all elements of the original list
        HeapConfiguration hc = programState.getHeap();
        TIntArrayList visitedNodes = new TIntArrayList();
        node = MethodsToOperateOnLists.getNextConcreteNodeInList(hc, visitedNodes, node, next, getFirst);

        do {
            ProgramState ps = programState.clone();

            // For each materialized node, add ProgramState to result with variable pointing to node
            GeneralConcreteValue concreteRHS = new GeneralConcreteValue(scene().getType("java.util.LinkedList"), node);

            try {
                lhs.evaluateOn(ps); // enforce materialization if necessary
                lhs.setValue(ps, concreteRHS);
            } catch (NullPointerDereferenceException e) {
                logger.error(e.getErrorMessage(this));
            }

            ProgramState res = ps.clone();
            res.setProgramCounter(nextPC);
            result.add(res);


            // check if next node is materialized
            if(!hc.selectorLabelsOf(node).contains(next)){

                // next node is not materialized -> apply rules

                // 1. add node between current node and ntEdge
                HeapConfiguration hc1 = hc.clone();
                TIntArrayList newNodes = new TIntArrayList();
                int nextConcreteNode = MethodsToOperateOnLists.getNextConcreteNodeInList(hc1, visitedNodes, node, next, getFirst);
                hc1.builder()
                        .addNodes(scene().getType("java.util.LinkedList"), 1, newNodes)
                        .addSelector(node, next, newNodes.get(0))
                        .build();

                // ntEdges are ordered after time of creation -> usage of method
                //int ntEdge = hc1.attachedNonterminalEdgesOf(node).get(hc1.attachedNonterminalEdgesOf(node).size()-1);
                int ntEdge = MethodsToOperateOnLists.getAttachedNtEdgeInNextDirection(node, hc1);
                Nonterminal ntEdgeLabel = hc1.labelOf(ntEdge);
                MethodsToOperateOnLists.replaceNtEdgeWithUpdatedTentacles(hc1, ntEdge, newNodes.get(0), nextConcreteNode);

                ProgramState tmp = ps.shallowCopyWithUpdateHeap(hc1.clone());
                concreteRHS = new GeneralConcreteValue(scene().getType("java.util.LinkedList"), newNodes.get(0));

                try {
                    lhs.evaluateOn(tmp); // enforce materialization if necessary
                    lhs.setValue(tmp, concreteRHS);
                } catch (NullPointerDereferenceException e) {
                    logger.error(e.getErrorMessage(this));
                }

                result.add(tmp.shallowCopyUpdatePC(nextPC));


                // 2. add ntEdge and concrete node in front of ntEdge
                // (since we start with hc1 here, we only have to add an ntEdge between node and newNodes.get(0)
                HeapConfiguration hc2 = hc1.clone();
                TIntArrayList newAttachedNodes = new TIntArrayList();
                newAttachedNodes.add(node);
                newAttachedNodes.add(newNodes.get(0));
                hc2.builder()
                        .removeSelector(node, next)
                        .addNonterminalEdge(ntEdgeLabel, newAttachedNodes)
                        .build();

                tmp = ps.shallowCopyWithUpdateHeap(hc2);
                //concreteRHS = new GeneralConcreteValue(scene().getType("java.util.LinkedList"), newNodes.get(0));

                try {
                    lhs.evaluateOn(tmp); // enforce materialization if necessary
                    lhs.setValue(tmp, concreteRHS);
                } catch (NullPointerDereferenceException e) {
                    logger.error(e.getErrorMessage(this));
                }

                result.add(tmp.shallowCopyUpdatePC(nextPC));

            }

            // set node to next concrete node and continue loop
            node = MethodsToOperateOnLists.getNextConcreteNodeInList(hc, visitedNodes, node, next, getFirst);


        }while(node != hc.variableTargetOf("null"));


        return result;
    }


    public String toString() {

        return rhs.toString() + ".get()" + ";";
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
