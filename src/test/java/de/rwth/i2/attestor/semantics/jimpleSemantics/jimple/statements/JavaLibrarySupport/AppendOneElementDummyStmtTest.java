package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.internal.ExampleHcImplFactory;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InstanceInvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.InvokeHelper;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Local;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.values.Value;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.types.Type;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class AppendOneElementDummyStmtTest {

    private AppendOneElementDummyStmt stmt;
    private HeapConfiguration inputGraph;
    private ProgramState inputState;

    private SceneObject sceneObject;
    private ExampleHcImplFactory hcFactory;

    @Before
    public void setUp() throws Exception {
        sceneObject = new MockupSceneObject();
        hcFactory = new ExampleHcImplFactory(sceneObject);

        Type type = sceneObject.scene().getType("SLList");
        Local var = new Local(type, "x");
        Local argument = new Local(type, "y");


        inputState = sceneObject.scene().createProgramState(hcFactory.getListAndConstants());
        inputState.prepareHeap();
        inputGraph = inputState.getHeap();

        ArrayList<Value> arguments = new ArrayList<>();
        arguments.add(argument);

        InvokeHelper invokePrepare = new InstanceInvokeHelper(sceneObject, var, arguments);

        stmt = new AppendOneElementDummyStmt(sceneObject, invokePrepare, 1);
    }

    @Test
    public void computeSuccessors() {
        Collection<ProgramState> res = stmt.computeSuccessors(inputState);
        assertEquals(1, res.size());
        ProgramState resState = res.iterator().next();
        assertNotSame("ensure clone on state level", resState, inputState);
        assertNotSame("ensure clone on graph level", inputGraph, resState.getHeap());
        assertSame("ensure inputGraph still in inputState", inputGraph, inputState.getHeap());
        ProgramState tmp = sceneObject.scene().createProgramState(hcFactory.getListAndConstants());
        tmp.prepareHeap();
        HeapConfiguration expectedGraph = tmp.getHeap();
        assertEquals("ensure inputGraph didn't change", expectedGraph, inputGraph);
        // for clean heap, the setUp needs to be changed, so the heap nodes are of type SLList
        assertEquals("ensure heap is clean again", inputGraph, resState.getHeap());
    }
}