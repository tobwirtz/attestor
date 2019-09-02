package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.graph.SelectorLabel;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationBuilder;
import de.rwth.i2.attestor.graph.heap.internal.InternalHeapConfiguration;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.types.Type;
import gnu.trove.list.array.TIntArrayList;

import java.util.List;

/**
 * @author Tobias
 */

public class ValidationComponent extends SceneObject {


    ValidationComponent(SceneObject otherObject){
        super(otherObject);
    }

    public HeapConfiguration dllToHC(List list){
        HeapConfiguration result = new InternalHeapConfiguration();
        TIntArrayList nodes = new TIntArrayList();

        SelectorLabel next = scene().getSelectorLabel("next");
        //SelectorLabel prev = scene().getSelectorLabel("prev");
        Type type = scene().getType("node");

        int index = 0;

        for(Object element : list){

            result.builder().addNodes(type, 1, nodes);

            if(index > 0){
                result.builder().addSelector(index-1, next, index);
                //result.builder().addSelector(index, prev, index-1);
            }

            index++;
        }

        result.builder().build();



        return result;
    }

    
}