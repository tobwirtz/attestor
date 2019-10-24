package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.types.Type;
import gnu.trove.list.array.TIntArrayList;

import java.util.LinkedHashSet;
import java.util.Set;

class MethodsToOperateOnLists {


    static int getNextConcreteNodeInList(HeapConfiguration hc, TIntArrayList visitedNodes, int currentNode, SelectorLabel next, SelectorLabel getFirst){
        visitedNodes.add(currentNode);
        if(hc.selectorLabelsOf(currentNode).contains(next)){
            currentNode = hc.selectorTargetOf(currentNode, next);
        }else if(hc.selectorLabelsOf(currentNode).contains(getFirst)){
            currentNode = hc.selectorTargetOf(currentNode, getFirst);
        }else{
            TIntArrayList ntEdges = hc.attachedNonterminalEdgesOf(currentNode);
            if(ntEdges.size() > 2){
                System.out.println("Input does not seem to be a List:" + hc);
                return -1;
            }

            int followingNtEdge = getAttachedNtEdgeInNextDirection(currentNode, hc);
            currentNode = hc.attachedNodesOf(followingNtEdge).get(1);
            /*
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
             */
        }
        if(currentNode == -1){
            System.out.println("Input does not seem to be a List: " + hc);
        }
        return currentNode;
    }

    static void replaceNtEdgeWithUpdatedTentacles(HeapConfiguration hc, int ntEdge, int t1, int t2){

        TIntArrayList newAttachedNodes = new TIntArrayList();
        newAttachedNodes.add(t1);
        newAttachedNodes.add(t2);

        hc.builder().addNonterminalEdge(hc.labelOf(ntEdge), newAttachedNodes)
                .removeNonterminalEdge(ntEdge)
                .build();
    }

    static void insertElementIntoListAtNextPosition(HeapConfiguration hc, int node, SelectorLabel currentSel, SelectorLabel followingSel, Type nodeType){

        TIntArrayList newNodes = new TIntArrayList();

        if(hc.selectorLabelsOf(node).contains(currentSel)){

            int followingNode = hc.selectorTargetOf(node,currentSel);

            hc.builder().addNodes(nodeType, 1, newNodes)
                    .removeSelector(node, currentSel)
                    .addSelector(node, currentSel, newNodes.get(0))
                    .addSelector(newNodes.get(0), followingSel, followingNode)
                    .build();

        }else if(node != hc.variableTargetOf("null")) {
            
            int followingNtEdge = getAttachedNtEdgeInNextDirection(node, hc);
            TIntArrayList tentacles = hc.attachedNodesOf(followingNtEdge);

            int nodeAfterNtEdge;

            if(tentacles.get(0) == node){
                nodeAfterNtEdge = tentacles.get(1);
            }else{
                nodeAfterNtEdge = tentacles.get(0);
            }

            hc.builder().addNodes(nodeType, 1, newNodes)
                    .addSelector(node, currentSel, newNodes.get(0))
                    .build();

            replaceNtEdgeWithUpdatedTentacles(hc, followingNtEdge, newNodes.get(0), nodeAfterNtEdge);
        }


    }

    static Set<HeapConfiguration> materializeFollowingNtEdgeManually(HeapConfiguration heapConfig, int node, SelectorLabel next, Type nodeType){

        Set<HeapConfiguration> result = new LinkedHashSet<>();

        // the next node needs to be an ntEdge
        int ntEdgeToBeMaterialized = MethodsToOperateOnLists.getAttachedNtEdgeInNextDirection(node, heapConfig);

        HeapConfiguration copyWithFirstRule = heapConfig.clone();
        HeapConfiguration copyWithSecondRule = heapConfig.clone();

        // materialize with first rule
        int nextConcreteNode = heapConfig.attachedNodesOf(ntEdgeToBeMaterialized).get(1);
        copyWithFirstRule.builder().removeNonterminalEdge(ntEdgeToBeMaterialized)
                .addSelector(node, next, nextConcreteNode).build();

        // materialize with second rule
        TIntArrayList newNode = new TIntArrayList();
        copyWithSecondRule.builder().addNodes(nodeType, 1, newNode)
                .removeSelector(node, next)
                .addSelector(node, next, newNode.get(0))
                .build();

        // replace first tentacle of ntEdge with the new node
        replaceNtEdgeWithUpdatedTentacles(copyWithSecondRule, ntEdgeToBeMaterialized, newNode.get(0), copyWithSecondRule.attachedNodesOf(ntEdgeToBeMaterialized).get(1));

        result.add(copyWithFirstRule);
        result.add(copyWithSecondRule);

        return result;
    }

    static int getAttachedNtEdgeInNextDirection(int node, HeapConfiguration hc){
        TIntArrayList attachedNtEdges = hc.attachedNonterminalEdgesOf(node);
        int res = -1;
        for(int i = 0; i < attachedNtEdges.size(); i++){
            if(hc.attachedNodesOf(attachedNtEdges.get(i)).get(0) == node){
                res = attachedNtEdges.get(i);
            }
        }
        return res;
    }

}
