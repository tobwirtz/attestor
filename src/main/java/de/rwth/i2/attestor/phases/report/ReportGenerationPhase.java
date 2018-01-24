package de.rwth.i2.attestor.phases.report;

import java.io.*;
import java.util.*;

import de.rwth.i2.attestor.grammar.GrammarExporter;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationExporter;
import de.rwth.i2.attestor.io.CustomHcListExporter;
import de.rwth.i2.attestor.io.FileUtils;
import de.rwth.i2.attestor.io.jsonExport.cytoscapeFormat.*;
import de.rwth.i2.attestor.io.jsonExport.inputFormat.ContractToInputFormatExporter;
import de.rwth.i2.attestor.main.AbstractPhase;
import de.rwth.i2.attestor.main.scene.ElementNotPresentException;
import de.rwth.i2.attestor.main.scene.Scene;
import de.rwth.i2.attestor.phases.communication.OutputSettings;
import de.rwth.i2.attestor.phases.transformers.*;
import de.rwth.i2.attestor.procedures.Contract;
import de.rwth.i2.attestor.procedures.Method;
import de.rwth.i2.attestor.stateSpaceGeneration.*;
import de.rwth.i2.attestor.util.ZipUtils;

public class ReportGenerationPhase extends AbstractPhase {

    private StateSpace stateSpace;
    private Program program;
    private OutputSettings outputSettings;

    public ReportGenerationPhase(Scene scene) {

        super(scene);
    }

    @Override
    public String getName() {

        return "Report generation";
    }

    @Override
    public void executePhase() {

        outputSettings = getPhase(OutputSettingsTransformer.class).getOutputSettings();

        if (outputSettings.isNoExport()) {
            return;
        }

        stateSpace = getPhase(StateSpaceTransformer.class).getStateSpace();
        program = getPhase(ProgramTransformer.class).getProgram();

        try {
            if (outputSettings.isExportGrammar()) {
                exportGrammar();
            }

            if (outputSettings.isExportStateSpace()) {
                exportStateSpace();
            }

            if (outputSettings.isExportCustomHcs()) {
                exportCustomHcs();
            }

            if (outputSettings.isExportContractsForReuse()) {
                exportContractsForReuse();
            }
            
            if( outputSettings.isExportContractsForInspection() ){
            	exportContractsForInspection();
            }

        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }

    }
    
	private void exportContractsForReuse() throws IOException {

        String directory = outputSettings.getDirectoryForReuseContracts();
        FileUtils.createDirectories(directory);
        for (String signature : outputSettings.getContractForReuseRequests().keySet()) {

            String filename = outputSettings.getContractForReuseRequests().get(signature);
            FileWriter writer = new FileWriter(directory + File.separator + filename);

            Collection<Contract> contracts;
			try {
				contracts = scene().getMethodIfPresent(signature).getContractsForExport();
				ContractToInputFormatExporter exporter = new ContractToInputFormatExporter(writer);
	            exporter.export(signature, contracts);
	            
			} catch (ElementNotPresentException e) {
				logger.info("The contract for " + signature + " is not present.");
			}

            
            writer.close();
        }
        logger.info("Exported contracts for reuse to '"
                + directory
                + "'"
        );
    }
	
    private void exportContractsForInspection() throws IOException {
    		
        logger.info("Exporting contracts for inspection ...");

        String location = outputSettings.getLocationForContractsForInspection();

        // Copy necessary libraries
        InputStream zis = getClass().getClassLoader().getResourceAsStream("contractViewer" +
                ".zip");

        File targetDirectory = new File(location + File.separator);
        ZipUtils.unzip(zis, targetDirectory);
        
        Map<String,Collection<Contract>> contracts = new HashMap<>();
        for( Method method : scene().getRegisteredMethods() ){
        	contracts.put(method.getName(), method.getContractsForExport());
        }

        // Generate JSON files
        JsonContractExporter exporter = new JsonContractExporter();
        exporter.export(location + File.separator + "contractData", contracts);

        logger.info("done. Contracts exported to '" + location + "'");
    }


    private void exportCustomHcs() throws IOException {

        String location = outputSettings.getLocationForCustomHcs();

        // Copy necessary libraries
        InputStream zis = getClass().getClassLoader().getResourceAsStream("customHcViewer" +
                ".zip");

        File targetDirectory = new File(location + File.separator);
        ZipUtils.unzip(zis, targetDirectory);

        // Generate JSON files for prebooked HCs and their summary
        CustomHcListExporter exporter = new JsonCustomHcListExporter();
        exporter.export(location + File.separator + "customHcsData", outputSettings.getCustomHcSet());

        logger.info("Custom HCs exported to '"
                + location
        );
    }

    private void exportStateSpace() throws IOException {

        logger.info("Exporting state space...");
        String location = outputSettings.getLocationForStateSpace();

        exportStateSpace(
                location + File.separator + "data",
                stateSpace,
                program
        );

        Set<ProgramState> states = stateSpace.getStates();
        for (ProgramState state : states) {
            int i = state.getStateSpaceId();
            exportHeapConfiguration(
                    location + File.separator + "data",
                    "hc_" + i + ".json",
                    state.getHeap()
            );
        }

        InputStream zis = getClass().getClassLoader().getResourceAsStream("viewer.zip");

        File targetDirectory = new File(location + File.separator);
        ZipUtils.unzip(zis, targetDirectory);

        logger.info("done. State space exported to '"
                + location
                + "'"
        );
    }

    private void exportGrammar() throws IOException {

        logger.info("Exporting grammar...");

        String location = outputSettings.getLocationForGrammar();

        // Copy necessary libraries
        InputStream zis = getClass().getClassLoader().getResourceAsStream("grammarViewer" +
                ".zip");

        File targetDirectory = new File(location + File.separator);
        ZipUtils.unzip(zis, targetDirectory);

        // Generate JSON files
        GrammarExporter exporter = new JsonGrammarExporter();
        exporter.export(location + File.separator + "grammarData",
                getPhase(GrammarTransformer.class).getGrammar());

        logger.info("done. Grammar exported to '" + location + "'");
    }

    private void exportHeapConfiguration(String directory, String filename, HeapConfiguration hc)
            throws IOException {

        FileUtils.createDirectories(directory);
        FileWriter writer = new FileWriter(directory + File.separator + filename);
        HeapConfigurationExporter exporter = new JsonHeapConfigurationExporter(writer);
        exporter.export(hc);
        writer.close();
    }

    private void exportStateSpace(String directory, StateSpace stateSpace, Program program)
            throws IOException {

        FileUtils.createDirectories(directory);
        Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(directory + File.separator + "statespace.json"))
        );
        StateSpaceExporter exporter = new JsonStateSpaceExporter(writer);
        exporter.export(stateSpace, program);
        writer.close();
    }

    @Override
    public void logSummary() {

        if (!outputSettings.isNoExport() && outputSettings.isExportStateSpace()) {
            String location = outputSettings.getLocationForStateSpace();
            logHighlight("State space has been exported to:");
            logSum(location);
        }
    }

    @Override
    public boolean isVerificationPhase() {

        return false;
    }
}
