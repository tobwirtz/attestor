package de.rwth.i2.attestor.main.settings;

import java.io.File;

/**
 * All settings related to exporting artifacts.
 *
 * @author Hannah Arndt, Christoph
 */
public class OutputSettings {

    /**
     * True if and only if the generated state space should be exported to a file.
     */
	private boolean exportStateSpace = false;

    /**
     * The path where an exported  state space are stored.
     */
	private String pathForStateSpace;

    /**
     * The directory that is created and contains the exported state space.
     */
	private String folderForStateSpace = "stateSpace";

    /**
     * True if and only if only the terminal states of the generated state space should be exported to a file.
     */
	private boolean exportTerminalStates = false;

    /**
     * The path where the exported terminal states are stored.
     */
	private String pathForTerminalStates;

    /**
     * The directory that is created and contains the exported terminal states.
     */
	private String folderForTerminalStates = "terminalStates";

    /**
     * True if and only if very large states should be exported separately for debugging purposes.
     */
	private boolean exportBigStates = false;

    /**
     * The number of states that is considered "very large".
     */
	private int bigStatesThreshold = 30;

    /**
     * The path where the exported very large states are stored.
     */
	private String pathForBigStates;

    /**
     * The directory that is created and contains exported very large states.
     */
	private String folderForBigStates = "debug";

    /**
     * True if and only if the loaded grammar should be exported.
     */
	private boolean exportGrammar = false;

    /**
     * The path where the exported grammar is stored.
     */
	private String pathForGrammar;

    /**
     * The directory that is created and contains exported grammars.
     */
	private String folderForGrammar;

    /**
     * Sets the default path for all exports.
     * @param path The default path.
     */
	public void setDefaultPath( String path ){
		pathForStateSpace = path;
		pathForBigStates = path;
		pathForTerminalStates = path;
		pathForGrammar = path;
	}

    /**
     * @return True if and only if the generated state space should be exported.
     */
	public boolean isExportStateSpace() {
		return exportStateSpace;
	}

    /**
     * @param exportStateSpace True if and only if the generated state space should be exported.
     */
	public void setExportStateSpace(boolean exportStateSpace) {
		this.exportStateSpace = exportStateSpace;
	}

    /**
     *
     * @param pathForStateSpace The path where exported state spaces are stored.
     */
	public void setPathForStateSpace(String pathForStateSpace) {
		this.pathForStateSpace = pathForStateSpace;
	}

    /**
     * @param folderForStateSpace The directory that is created and contains the exported state space.
     */
	public void setFolderForStateSpace(String folderForStateSpace) {
		this.folderForStateSpace = folderForStateSpace;
	}

    /**
     * @return The directory that is created and contains the exported state space.
     */
	public String getLocationForStateSpace(){
		return pathForStateSpace + File.separator + folderForStateSpace;
	}

    /**
     * @return True if and only terminal states should be exported.
     */
	public boolean isExportTerminalStates() {
		return exportTerminalStates;
	}

    /**
     * @param exportTerminalStates True if and only terminal states should be exported.
     */
	public void setExportTerminalStates(boolean exportTerminalStates) {
		this.exportTerminalStates = exportTerminalStates;
	}

    /**
     *
     * @param pathForTerminalStates The path where exported terminal states are stored.
     */
	public void setPathForTerminalStates(String pathForTerminalStates) {
		this.pathForTerminalStates = pathForTerminalStates;
	}

    /**
     * @param folderForTerminalStates The directory containing exported terminal states.
     */
	public void setFolderForTerminalStates(String folderForTerminalStates) {
		this.folderForTerminalStates = folderForTerminalStates;
	}

    /**
     * @return The fully qualified location containing exported terminal states.
     */
	public String getLocationForTerminalStates(){
		return pathForTerminalStates + File.separator + folderForTerminalStates;
	}

    /**
     * @return True if and only if very large states should be exported separately.
     */
	public boolean isExportBigStates() {
		return exportBigStates;
	}

    /**
     * @param threshold The number of states that is considered "very large".
     */
	public void exportBigStatesThreshold( int threshold ){
		bigStatesThreshold = threshold;
	}

    /**
     * @return The number of states that is considered "very large".
     */
	public int getBigStatesThreshold(){
		return bigStatesThreshold;
	}

    /**
     * @param exportBigStates True if and only if very large states should be exported separately.
     */
	public void setExportBigStates(boolean exportBigStates) {
		this.exportBigStates = exportBigStates;
	}

    /**
     * @param pathForBigStates The path where exported very large states are stored.
     */
	public void setPathForBigStates(String pathForBigStates) {
		this.pathForBigStates = pathForBigStates;
	}

    /**
     * @param folderForBigStates The directory containing exported very large states.
     */
	public void setFolderForBigStates(String folderForBigStates) {
		this.folderForBigStates = folderForBigStates;
	}

    /**
     * @return The fully qualified path to the directory containing exported very large states.
     */
	public String getLocationForBigStates(){
		return pathForBigStates + File.separator + folderForBigStates;
	}

    /**
     * @return True if and only if loaded grammars should be exported.
     */
	public boolean isExportGrammar() {
		return exportGrammar;
	}

    /**
     * @param exportGrammar True if and only if loaded grammars should be exported.
     */
	public void setExportGrammar(boolean exportGrammar) {
		this.exportGrammar = exportGrammar;
	}

    /**
     * @param pathForGrammar The path where exported grammars are stored.
     */
	public void setPathForGrammar(String pathForGrammar) {
		this.pathForGrammar = pathForGrammar;
	}

    /**
     * @param folderForGrammar The directory containing exported grammars.
     */
	public void setFolderForGrammar(String folderForGrammar) {
		this.folderForGrammar = folderForGrammar;
	}

    /**
     * @return The fully qualified path to the directory containing exported grammars.
     */
	public String getLocationForGrammar(){
		return pathForGrammar + File.separator + folderForGrammar;
	}

    /**
     * Checks whether all necessary paths and names are present for objects that should be exported.
     * @return True if and only if the current settings are consistent in the above sense.
     */
	public boolean isValid(){
		if( exportBigStates ){
			if( bigStatesThreshold < 0 || pathForBigStates == null || folderForBigStates == null ){
				return false;
			}
		}
		if( exportGrammar ){
			if( pathForGrammar == null || folderForGrammar == null ){
				return false;
			}
		}
		if( exportStateSpace ){
			if( pathForStateSpace == null || folderForStateSpace == null ){
				return false;
			}
		}
		if( exportTerminalStates ){
			if( pathForTerminalStates == null || folderForTerminalStates == null ){
				return false;
			}
		}
		return true;
	}
	
}
