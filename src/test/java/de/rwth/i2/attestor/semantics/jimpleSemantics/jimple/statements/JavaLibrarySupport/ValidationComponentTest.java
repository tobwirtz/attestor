package de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.JavaLibrarySupport;

import de.rwth.i2.attestor.MockupSceneObject;
import de.rwth.i2.attestor.main.PhaseRegistry;
import de.rwth.i2.attestor.main.scene.DefaultScene;
import de.rwth.i2.attestor.main.scene.SceneObject;
import de.rwth.i2.attestor.phases.commandLineInterface.CommandLinePhase;
import de.rwth.i2.attestor.phases.parser.ParseContractsPhase;
import de.rwth.i2.attestor.phases.parser.ParseGrammarPhase;
import de.rwth.i2.attestor.phases.parser.ParseInputPhase;
import de.rwth.i2.attestor.phases.parser.ParseProgramPhase;
import de.rwth.i2.attestor.phases.preprocessing.AbstractionPreprocessingPhase;
import de.rwth.i2.attestor.phases.preprocessing.GrammarRefinementPhase;
import de.rwth.i2.attestor.phases.preprocessing.MarkingGenerationPhase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ValidationComponentTest {

    private SceneObject sceneObject;
    //private LinkedList<Object> inputList;
    private ValidationComponent validationComponent;
    private int maxListLength = 50;
    private int inversePercentageForVariable = 4;

    @Before
    public void setUp() throws Exception {

        sceneObject = new MockupSceneObject();

        String[] args = new String[2];
        args[0] = "-l";
        args[1] = "configuration/settings/reverseSLList.attestor";
        PhaseRegistry registry = new PhaseRegistry();
        DefaultScene scene = new DefaultScene();
        registry.addPhase(new CommandLinePhase(scene, args))
                .addPhase(new ParseProgramPhase(scene)) //
                .addPhase(new ParseGrammarPhase(scene))
                .addPhase(new ParseInputPhase(scene))//
                .addPhase(new ParseContractsPhase(scene))//
                .addPhase(new MarkingGenerationPhase(scene))//
                .addPhase(new GrammarRefinementPhase(scene))//
                .addPhase(new AbstractionPreprocessingPhase(scene))
                .execute();
        //ParseGrammarPhase parsePhase = new ParseGrammarPhase(scene);
        //parsePhase.executePhase();
        //AbstractionPreprocessingPhase abstractionPhase = new AbstractionPreprocessingPhase(scene);
        //abstractionPhase.executePhase();
        validationComponent = new ValidationComponent(scene);

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
    public void validateAdd() {
        //System.out.println(inputList.toString());
        //HeapConfiguration hc = translator.dllToHC(inputList);
        //System.out.println(hc.toString());
        //int maxListLength = 50;
        //int inversePercentageForVariable = 4;
        assertTrue("AddStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "AddStmt"));
        //assertTrue("AddAtIndexStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "AddAtIndexStmt"));
        //assertTrue("ClearStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "ClearStmt"));
        //assertTrue("GetIndexStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "GetIndexStmt"));
        //assertTrue("RemoveIndexStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "RemoveIndexStmt"));
        //assertTrue("IteratorStmts are successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "IteratorStmts"));

        // iterator methods
        // linked list init???
    }

    @Test
    public void validateAddAtIndex() {

        assertTrue("AddAtIndexStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "AddAtIndexStmt"));

    }

    @Test
    public void validateClear() {

        assertTrue("ClearStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "ClearStmt"));

    }

    @Test
    public void validateGet() {

        assertTrue("GetIndexStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "GetIndexStmt"));

    }

    @Test
    public void validateRemove() {

        assertTrue("RemoveIndexStmt is successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "RemoveIndexStmt"));

    }

    @Test
    public void validateIteratorStmts() {

        assertTrue("IteratorStmts are successfully validated", validationComponent.validate(maxListLength, inversePercentageForVariable, "IteratorStmts"));

    }
}