package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationBuilder;
import de.rwth.i2.attestor.graph.heap.internal.InternalHeapConfiguration;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.programState.defaultState.ExceptionProgramState;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.Statement;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InstanceInvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Local;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.SettableValue;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.semantics.util.Constants;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.types.Type;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;

/**
 * @author Tobias
 */

public class ValidationComponent extends SceneObject {


    ValidationComponent(Scene scene){
        super(scene);
    }

    boolean validate(int maxListLength, int inversePercentageForVariable, String stmtToBeValidated){

        boolean result = false;

        // build the lists
        List<List<Object>> lists = buildLists(maxListLength);

        for(List<Object> l : lists){

            result = false;

            // add variables
            Map<Object, List<String>> elementsAndVariableNames = addVariablesRandomlyToList(l, inversePercentageForVariable);
            // dllToHC
            ProgramState inputState = scene().createProgramState(dllToHC(l, elementsAndVariableNames));


            Collection<ProgramState> successors;
            List<Object> libraryList = new LinkedList<>(l);
            ArrayList<Value> arguments = new ArrayList<>();
            InvokeHelper instanceHelper = new InstanceInvokeHelper(this, new Local(inputState.getVariableTarget("head").type(), "head"), arguments);
            Statement stmt;
            SettableValue lhs;
            String exception = "";
            Random rand = new Random();
            int maxIndex = (int) Math.round((libraryList.size()+1)*1.15);

            switch(stmtToBeValidated){

                case "AddStmt":
                    // create statement
                    stmt = new AddStmt(this, instanceHelper, new Local(inputState.getVariableTarget("head").type(), "head"), 1);

                    // compute successors / execute transfer function
                    successors = stmt.computeSuccessors(inputState);

                    // execute method on original list
                    libraryList.add("Test");
                    break;

                case "AddAtIndexStmt":
                    // create statement
                    stmt = new AddAtIndexStmt(this, instanceHelper, new Local(inputState.getVariableTarget("head").type(), "head"), 1); //new AddStmt(this, instanceHelper, new Local(inputState.getVariableTarget("head").type(), "head"), 1);

                    // compute successors / execute transfer function
                    successors = stmt.computeSuccessors(inputState);

                    // execute method on original list
                    int index = rand.nextInt(maxIndex);
                    try {
                        libraryList.add(index, "Test");
                    }catch (IndexOutOfBoundsException e){
                        exception = "IndexOutOfBoundsException";
                    }

                    break;

                case "ClearStmt":
                    // create statement
                    stmt = new ClearStmt(this, instanceHelper, new Local(inputState.getVariableTarget("head").type(), "head"), 1);

                    // compute successors / execute transfer function
                    successors = stmt.computeSuccessors(inputState);

                    // execute method on original list
                    libraryList.clear();
                    break;

                case "GetIndexStmt":

                    // create statement
                    //lhs is settable test-value, rhs is (Value) Base/head
                    lhs = new Local(scene().getType("java.lang.Object"),"GetIndexStmtTestVariable");
                    stmt = new GetIndexStmt(this, lhs, new Local(inputState.getVariableTarget("head").type(), "head"), 1, new HashSet<>());

                    successors = stmt.computeSuccessors(inputState);


                    // execute method on original list
                    try {
                        int randomIndex = rand.nextInt(maxIndex);
                        if (elementsAndVariableNames.containsKey(libraryList.get(randomIndex))) {
                            elementsAndVariableNames.get(libraryList.get(randomIndex)).add("GetIndexStmtTestVariable");
                        } else {
                            List<String> newVars = new LinkedList<>();
                            newVars.add("GetIndexStmtTestVariable");
                            elementsAndVariableNames.put(libraryList.get(randomIndex), newVars);
                        }

                    }catch (IndexOutOfBoundsException e){
                        exception = "IndexOutOfBoundsException";
                    }


                    break;

                case "RemoveIndexStmt":

                    // create statement
                    stmt = new RemoveIndexStmt(this, instanceHelper, new Local(inputState.getVariableTarget("head").type(), "head"), 1);

                    // compute successors / execute transfer function
                    successors = stmt.computeSuccessors(inputState);

                    // execute method on original list
                    try {
                        libraryList.remove(rand.nextInt(maxIndex));
                    } catch (IndexOutOfBoundsException e){
                        exception = "IndexOutOfBoundsException";
                    }

                    break;

                case "IteratorStmts":

                    // create statement
                    //lhs is settable value (var set to iteratornode), rhs is (Value) Base/head of List
                    lhs = new Local(scene().getType("java.lang.Object"),"IteratorObjectVariable");
                    Local var = new Local(scene().getType("java.lang.Object"),"IteratorTestVariable");
                    Statement iteratorStmt = new IteratorAsObjectStmt(this, lhs, new Local(inputState.getVariableTarget("head").type(), "head"), 1, new HashSet<>());
                    Statement nextStmt = new IteratorNextAsObjectStmt(this, var, lhs, 1, new HashSet<>());

                    successors = iteratorStmt.computeSuccessors(inputState);
                    Iterator it = libraryList.iterator();

                    int randomIndex = rand.nextInt(Math.min(maxIndex, 10))+1;

                    // apply .next method for a random amount of times
                    Object currentElement = "false";
                    for(int i = 0; i < randomIndex; i++){
                        successors = computeSuccessorsForAllStates(successors, nextStmt);

                        try{
                            currentElement = it.next();
                        } catch (Exception e) {
                            exception = "NoSuchElementException";
                            break;
                        }
                    }

                    // add iterator to variables
                    if(!exception.equals("NoSuchElementException") && currentElement != null){
                        if(elementsAndVariableNames.containsKey(currentElement)){
                            elementsAndVariableNames.get(currentElement).add("IteratorTestVariable");
                        }else{
                            List<String> newVars = new LinkedList<>();
                            newVars.add("IteratorTestVariable");
                            elementsAndVariableNames.put(currentElement, newVars);
                        }
                    }


                    break;

                default: return false;
            }


            if(!sanityCheckForLists(successors, libraryList.size()+1)){
                // +1 to max size needed because due to abstraction, attestor can make the list longer than it actually is
                // (e.g. when applying the remove stmt)
                System.out.println("Sanitycheck failed with listlength "+ libraryList.size());
                return false;
            }

            // check if there is a successor that matches the library list
            HeapConfiguration libraryResultHeap = dllToHC(libraryList, elementsAndVariableNames);
            for(ProgramState succ : successors){

                if(succ instanceof ExceptionProgramState && ((ExceptionProgramState) succ).exceptionMessage.equals(exception)){
                    result = true;
                }

                // abstraction step for successors
                HeapConfiguration succHC = this.scene().strategies().getAggressiveCanonicalizationStrategy().canonicalize(succ.getHeap());

                if(libraryResultHeap.equals(succHC) && exception.equals("")){
                    result = true;
                }
            }
            if(!result){
                return false;
            }
            if(successors.size() > 1 && !correctenesCheck(inputState.getHeap().clone(), successors, stmtToBeValidated)){
                System.out.println("Correctnes Check failed");
                return false;
            }
        }


        return result;
    }



    private Collection<ProgramState> computeSuccessorsForAllStates(Collection<ProgramState> inputStates, Statement stmt){
        Collection<ProgramState> successors = new LinkedHashSet<>();

        for(ProgramState state : inputStates){
            if(state instanceof ExceptionProgramState){
                successors.add(state);
            }else {
                successors.addAll(stmt.computeSuccessors(state));
            }
        }


        return successors;
    }



    private HeapConfiguration dllToHC(List list, Map<Object, List<String>> variablesAndElements){
        Set<Object> elementsHavingVariables = variablesAndElements.keySet();
        HeapConfiguration result = new InternalHeapConfiguration();
        TIntArrayList nodes = new TIntArrayList();

        SelectorLabel next = scene().getSelectorLabel("next");
        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");
        //SelectorLabel prev = scene().getSelectorLabel("prev");
        Type headType = scene().getType("java.util.LinkedListPointer");
        Type type = scene().getType("java.util.LinkedList");

        int index = 0;
        HeapConfigurationBuilder builder = result.builder();

        builder = builder.addNodes(scene().getType("int"), 2, nodes)
                .addVariableEdge(Constants.ONE, nodes.get(1))
                .addVariableEdge(Constants.TRUE, nodes.get(1))
                .addVariableEdge(Constants.FALSE, nodes.get(0))
                .addVariableEdge(Constants.ZERO, nodes.get(0));
        index = index+2;

        // add head node (not an element node)
        builder = builder.addNodes(headType, 1, nodes);
        builder = builder.addVariableEdge("head", nodes.get(index));
        index++;

        // add elements
        for(Object element : list){

            builder = builder.addNodes(type, 1, nodes);

            if(index == 3){
                builder = builder.addSelector(nodes.get(index-1), getFirst, nodes.get(index));
            }else {
                builder = builder.addSelector(nodes.get(index - 1), next, nodes.get(index));
            }


            if(elementsHavingVariables.contains(element)){
                // add variable to nodes.get(index)
                for(String varName : variablesAndElements.get(element)) {
                    if(varName.equals("IteratorTestVariable")){
                        // create iterator node and make the curr selector point to the element node
                        TIntArrayList newNodes = new TIntArrayList();
                        builder = builder.addNodes(scene().getType("java.util.Iterator"), 1, newNodes)
                                .addSelector(newNodes.get(0), scene().getSelectorLabel("curr"), nodes.get(index))
                                .addVariableEdge("IteratorObjectVariable", newNodes.get(0));
                    }
                    builder = builder.addVariableEdge(varName, nodes.get(index));
                }

            }

            index++;
        }

        // add null node and let last node point with next to null node
        builder = builder.addNodes(type, 1, nodes);
        builder = builder.addVariableEdge(Constants.NULL, nodes.get(index));
        if(index == 3){
            builder = builder.addSelector(nodes.get(index-1), getFirst, nodes.get(index));
        }else {
            builder = builder.addSelector(nodes.get(index-1), next, nodes.get(index));
        }


        result = builder.build();

        // abstraction needed
        result = this.scene().strategies().getAggressiveCanonicalizationStrategy().canonicalize(result);

        return result;
    }


    private List<Object> concretizedHCtoDLL(HeapConfiguration hc, Map<Object, List<String>> variablesAndElements){

        List<Object> res = new LinkedList<>();
        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");
        SelectorLabel next = scene().getSelectorLabel("next");
        int index = 0;

        int node = hc.variableTargetOf("head");
        TIntArrayList visitedNodes = new TIntArrayList();
        // go to first Element node
        node = MethodsToOperateOnLists.getNextConcreteNodeInList(hc, visitedNodes, node, next, getFirst);

        // iterate through HC and add list elements
        while(node != hc.variableTargetOf("null")){
            res.add("Dummy Object" + index);

            // add variables
            if (hc.attachedVariablesOf(node).size() > 0){
                List<String> vars = new LinkedList<>();
                for(int varEdge : hc.attachedVariablesOf(node).toArray()){
                    vars.add(hc.nameOf(varEdge));
                }
                variablesAndElements.put(res.get(index), vars);
            }

            index++;
            node = MethodsToOperateOnLists.getNextConcreteNodeInList(hc, visitedNodes, node, next, getFirst);
        }

        return res;
    }




    private List<List<Object>> buildLists(int maxListLength){
        List<List<Object>> result = new LinkedList<>();

        // empty List
        List<Object> li = new LinkedList<>();
        result.add(li);

        // Lists without variables
        for(int i = 1; i <= maxListLength; i++){
            li = new LinkedList<>(li);
            // needs to be numbered for the dllToHC method
            li.add("Dummy Object" + i);
            result.add(li);
        }

        return result;
    }

    private Map<Object, List<String>> addVariablesRandomlyToList(List<Object> list, int inversePercentageForVariable){

        Map<Object, List<String>> elementsAndVariablenames = new HashMap<>();
        int variableCount = 1;
        Random rand = new Random();
        for (Object listElement : list) {
            int getsVariable = rand.nextInt(inversePercentageForVariable);

            if (getsVariable == 1) {
                // save in Map
                List<String> vars = new LinkedList<>();
                vars.add("Var " + variableCount);
                elementsAndVariablenames.put(listElement, vars);
                variableCount++;
            }

        }
        return elementsAndVariablenames;
    }


    private boolean sanityCheckForLists(Collection<ProgramState> states, int maxlength){

        for(ProgramState state : states){
            int concreteNodeCounter = 1;
            HeapConfiguration hc = state.getHeap();
            SelectorLabel getFirst = scene().getSelectorLabel("getFirst");
            SelectorLabel next = scene().getSelectorLabel("next");

            int node = hc.variableTargetOf("head");
            TIntArrayList visitedNodes = new TIntArrayList();
            visitedNodes.add(node);
            if(!hc.selectorLabelsOf(node).contains(getFirst)){
                return false;
            }
            node = hc.selectorTargetOf(node, getFirst);

            while(node != hc.variableTargetOf("null")){
/*
                if(node == -1 || concreteNodeCounter > maxlength){
                    return false;
                }

 */
                node = MethodsToOperateOnLists.getNextConcreteNodeInList(hc, visitedNodes, node, next, getFirst);
                concreteNodeCounter++;
            }
        }

        return true;
    }


    private boolean correctenesCheck(HeapConfiguration inputHeap, Collection<ProgramState> succs, String statement){

        // 1. materialize first time
        List<HeapConfiguration> workset = materializeEachNtEdgeOnce(inputHeap);

        while(!succs.isEmpty()) {

            for(HeapConfiguration hc : workset){
                if(hc.countNonterminalEdges() == 0){

                    // 2. translate to library list
                    Map<Object, List<String>> elementsAndVariableNames = new HashMap<>();
                    List<Object> libraryList = concretizedHCtoDLL(hc, elementsAndVariableNames);

                    // 3. apply library method and transform result to abstract HeapConfiguration
                    List<HeapConfiguration> results = new LinkedList<>();
                    List<String> exceptions = new LinkedList<>();
                    switch(statement){
                        case "AddAtIndexStmt":
                            for(int i = 0; i <= libraryList.size()+1; i++) {
                                try {
                                    List<Object> tmp = new LinkedList(libraryList);
                                    tmp.add(i, "Test");
                                    results.add(dllToHC(tmp, elementsAndVariableNames));
                                }catch (IndexOutOfBoundsException e){
                                    exceptions.add("IndexOutOfBoundsException");
                                }
                            }
                            break;

                        case "GetIndexStmt":
                            for(int i = 0; i <= libraryList.size(); i++) {
                                try {
                                    List<Object> tmp = new LinkedList<>(libraryList);
                                    Map<Object, List<String>> tmpEandVnames = new HashMap<>(elementsAndVariableNames);
                                    if (tmpEandVnames.containsKey(libraryList.get(i))) {
                                        List<String> tmpVarList = new LinkedList<>(tmpEandVnames.get(libraryList.get(i)));
                                        tmpVarList.add("GetIndexStmtTestVariable");
                                        tmpEandVnames.replace(libraryList.get(i), tmpVarList);
                                    } else {
                                        List<String> newVars = new LinkedList<>();
                                        newVars.add("GetIndexStmtTestVariable");
                                        tmpEandVnames.put(libraryList.get(i), newVars);
                                    }

                                    results.add(dllToHC(tmp, tmpEandVnames));
                                }catch (IndexOutOfBoundsException e){
                                    exceptions.add("IndexOutOfBoundsException");
                                }
                            }
                            break;

                        case "RemoveIndexStmt":
                            for(int i = 0; i <= libraryList.size(); i++) {
                                try {
                                    List<Object> tmp = new LinkedList<>(libraryList);
                                    tmp.remove(i);
                                    results.add(dllToHC(tmp, elementsAndVariableNames));
                                } catch (IndexOutOfBoundsException e){
                                    exceptions.add("IndexOutOfBoundsException");
                                }
                            }
                            break;

                        case "IteratorStmts":
                            // create iterator, iterate through list and add
                            Iterator<Object> iterator = libraryList.iterator();
                            while(iterator.hasNext()){

                                Object currentElement = iterator.next();

                                List<Object> tmp = new LinkedList<>(libraryList);
                                Map<Object, List<String>> tmpEandVnames = new HashMap<>(elementsAndVariableNames);
                                if (tmpEandVnames.containsKey(currentElement)) {
                                    List<String> tmpVarList = new LinkedList<>(tmpEandVnames.get(currentElement));
                                    tmpVarList.add("IteratorTestVariable");
                                    tmpEandVnames.replace(currentElement, tmpVarList);
                                } else {
                                    List<String> newVars = new LinkedList<>();
                                    newVars.add("IteratorTestVariable");
                                    tmpEandVnames.put(currentElement, newVars);
                                }
                                results.add(dllToHC(tmp, tmpEandVnames));

                            }
                            exceptions.add("NoSuchElementException");
                            break;

                        default: return false;
                    }

                    // 4. remove reached HCs from succs set
                    for(HeapConfiguration res : results){
                        List<ProgramState> toBeRemoved = new LinkedList<>();
                        for(ProgramState state : succs){
                            if(state instanceof ExceptionProgramState && exceptions.contains(((ExceptionProgramState) state).exceptionMessage)){
                                toBeRemoved.add(state);
                            }else if(!(state instanceof ExceptionProgramState)){
                                HeapConfiguration succHC = this.scene().strategies().getAggressiveCanonicalizationStrategy().canonicalize(state.getHeap());
                                if(succHC.equals(res)){
                                    toBeRemoved.add(state);
                                }
                            }
                        }
                        for(ProgramState state : toBeRemoved){
                            succs.remove(state);
                        }

                    }

                }
            }

            // remove all concrete states in workset (finished working with them)
            Set<HeapConfiguration> removableWorklistItems = new HashSet<>();
            for(HeapConfiguration hc : workset){
                if(hc.nonterminalEdges().size() == 0){
                    removableWorklistItems.add(hc);
                }
            }
            workset.removeAll(removableWorklistItems);

            // materialize further
            List<HeapConfiguration> tmp = new LinkedList<>();
            for(HeapConfiguration hc : workset){
                tmp.addAll(materializeEachNtEdgeOnce(hc));
            }

            workset = tmp;


        }


        return true;
    }


    private List<HeapConfiguration> materializeEachNtEdgeOnce(HeapConfiguration hc){

        List<HeapConfiguration> result = new LinkedList<>();
        HeapConfiguration list1 = hc.clone();
        HeapConfiguration list2 = hc.clone();

        SelectorLabel getFirst = scene().getSelectorLabel("getFirst");
        SelectorLabel next = scene().getSelectorLabel("next");
        Type type = scene().getType("java.util.LinkedList");

        int node = hc.variableTargetOf("head");
        TIntArrayList visitedNodes = new TIntArrayList();

        node = hc.selectorTargetOf(node, getFirst);

        while(hc.variableTargetOf("null") != node){

            if(hc.selectorLabelsOf(node).contains(next)){
                node = hc.selectorTargetOf(node, next);
            }else{

                list1 = MethodsToOperateOnLists.materializeFollowingNtEdgeWithFirstRule(list1, node, next, type);
                list2 = MethodsToOperateOnLists.materializeFollowingNtEdgeWithSecondRule(list2, node, next, type);

                node = MethodsToOperateOnLists.getNextConcreteNodeInList(hc, visitedNodes, node, next, getFirst);
            }
        }

        result.add(list1);
        result.add(list2);

        return result;
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