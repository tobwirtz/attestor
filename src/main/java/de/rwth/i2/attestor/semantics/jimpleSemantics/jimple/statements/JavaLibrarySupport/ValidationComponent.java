package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationBuilder;
import de.rwth.i2.attestor.graph.heap.internal.InternalHeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.types.Type;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

/**
 * @author Tobias
 */

public class ValidationComponent extends SceneObject {


    ValidationComponent(SceneObject otherObject){
        super(otherObject);
    }

    public HeapConfiguration dllToHC(List list, Map<Object, String> variablesAndElements){
        Set<Object> elementsHavingVariables = variablesAndElements.keySet();
        HeapConfiguration result = new InternalHeapConfiguration();
        TIntArrayList nodes = new TIntArrayList();

        SelectorLabel next = scene().getSelectorLabel("next");
        //SelectorLabel prev = scene().getSelectorLabel("prev");
        Type type = scene().getType("node");

        int index = 0;
        HeapConfigurationBuilder builder = result.builder();

        for(Object element : list){

            builder = builder.addNodes(type, 1, nodes);

            if(index > 0){
                builder = builder.addSelector(index-1, next, index);
                //result.builder().addSelector(index, prev, index-1);
            }

            if(elementsHavingVariables.contains(element)){
                // add variable to nodes.get(index)
                builder = builder.addVariableEdge(variablesAndElements.get(element), index);

            }

            index++;
        }

        result = builder.build();



        return result;
    }

    public List buildLists(){
        List<List> result = new LinkedList<>();

        // empty List
        List<Object> li = new LinkedList<>();
        result.add(li);

        // Lists without variables
        for(int i = 1; i <= 50; i++){
            li = new LinkedList<>(li);
            li.add("Dummy Object");
            result.add(li);
        }

        return result;
    }

    public Map<Object, String> addVariablesRandomlyToList(List<Object> list){

        Map<Object, String> elementsAndVariablenames = new HashMap<>();
        int variableCount = 1;
        Random rand = new Random();
        for(int i = 0; i < list.size(); i++){
            Object listElement = list.get(i);
            int getsVariable = rand.nextInt(4);

            if(getsVariable == 1){
                // save in Map
                elementsAndVariablenames.put(listElement, "Var " + variableCount);
                variableCount++;
            }

        }
        return elementsAndVariablenames;
    }




    public boolean listIsomorphycheck (HeapConfiguration heap1, int head1, HeapConfiguration heap2, int head2){

        SelectorLabel next = scene().getSelectorLabel("next");
        TIntArrayList matchedInFirst = new TIntArrayList();
        TIntArrayList matchedInSecond = new TIntArrayList();

        while(head1 != heap1.variableTargetOf("null") && head2 != heap2.variableTargetOf("null")){

            if(heap1.attachedVariablesOf(head1).size() != heap2.attachedVariablesOf(head2).size()
            || heap1.attachedNonterminalEdgesOf(head1).size() != heap2.attachedNonterminalEdgesOf(head2).size()
            || heap1.successorNodesOf(head1).size() != heap2.successorNodesOf(head2).size()
            || heap1.predecessorNodesOf(head1).size() != heap2.predecessorNodesOf(head2).size()){
                return false;
            }

            matchedInFirst.add(head1);
            matchedInSecond.add(head2);

            if(heap1.selectorLabelsOf(head1).contains(next) && heap2.selectorLabelsOf(head2).contains(next)){
                head1 = heap1.selectorTargetOf(head1, next);
                head2 = heap2.selectorTargetOf(head2, next);
            }else if(heap1.attachedNonterminalEdgesOf(head1).size() > 0 && heap2.attachedNonterminalEdgesOf(head2).size() > 0){

                TIntArrayList ntEdges1 = heap1.attachedNonterminalEdgesOf(head1);
                TIntArrayList ntEdges2 = heap2.attachedNonterminalEdgesOf(head2);

                for(int i = 0; i < ntEdges1.size(); i++){

                    int edge1 = ntEdges1.get(i);
                    int edge2 = ntEdges2.get(i);

                    if(!matchedInFirst.contains(edge1) && !matchedInSecond.contains(edge2)){
                        head1 = edge1;
                        head2 = edge2;
                        break;
                    }
                }
            }


        }
        return true;
    }

}