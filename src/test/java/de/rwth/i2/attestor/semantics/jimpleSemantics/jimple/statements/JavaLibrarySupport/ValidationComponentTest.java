package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.main.scene.SceneObject;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertTrue;

public class ValidationComponentTest {

    private SceneObject sceneObject;
    //private LinkedList<Object> inputList;
    private ValidationComponent validationComponent;

    @Before
    public void setUp() throws Exception {

        sceneObject = new MockupSceneObject();
        validationComponent = new ValidationComponent(sceneObject);

        /*
        //build list
        inputList = new LinkedList<>();
        inputList.add("Object 0");
        inputList.add("Object 1");
        inputList.add("Object 2");
        inputList.add("Object 3");
        inputList.add("Object 4");

         */
    }

    @Test
    public void dllToHC() {
        //System.out.println(inputList.toString());
        //HeapConfiguration hc = translator.dllToHC(inputList);
        //System.out.println(hc.toString());
        assertTrue("AddStmt is successfully validated", validationComponent.validate());
    }
}