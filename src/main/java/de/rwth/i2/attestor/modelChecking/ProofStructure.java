package de.rwth.i2.attestor.modelChecking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.rwth.i2.attestor.export.ProofStructureExporter;
import de.rwth.i2.attestor.export.ProofStructureHtmlExporter;
import de.rwth.i2.attestor.LTLFormula;
import de.rwth.i2.attestor.generated.node.ANextLtlform;
import de.rwth.i2.attestor.generated.node.AReleaseLtlform;
import de.rwth.i2.attestor.generated.node.Node;
import de.rwth.i2.attestor.generated.node.Start;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSpace;
import de.rwth.i2.attestor.stateSpaceGeneration.StateSuccessor;

/**
 * The proof structure is generated by the tableau method for model checking 
 * (see Another Look at Model Checking, Clarke, Grumberg, Hamaguchi, 1994).
 * 
 * @author christina
 *
 */

public class ProofStructure {
	
	private static final Logger logger = LogManager.getLogger( "proofStructure.java" );
	
	HashSet<Assertion> vertices;
	HashMap<Assertion, HashSet<SuccState>> edges;
	boolean successful = true;
	
	
	/**
	 * This class models the edges of the proof structure. I.e. it holds a successor assertion
	 * together with the edge label, that carries the type of applied tableau rule.
	 * 
	 * @author christina
	 *
	 */
	class SuccState {
		Assertion assertion;
		Node type;
		
		// TODO: check if we need succstates instead of assertions only
		private SuccState(Assertion assertion, Node type){
			this.assertion = assertion;
			this.type = type;
		}
		
	}
	
	
	public ProofStructure() {
		this.vertices = new HashSet<Assertion>();
		this.edges = new HashMap<Assertion, HashSet<SuccState>>();
	}
	
	/**
	 * This method builds the proof structure according to the tableau method (as depicted in 
	 * Jonathan's Diss. It sets the successful variable to false, if a failing leaf or cycle is 
	 * detected.
	 * 
	 * @param statespace, the (labelled) state space we want to check the formula for
	 * @param formula, the ltl formula to check
	 */
	public void build(StateSpace statespace, LTLFormula formula){
		
		logger.trace("Building proof structure for formula " + formula.toString());
		
		// The queue holding the vertices that have still to be processed
		LinkedList<Assertion> vertexQueue = new LinkedList<Assertion>();
		
		// Initialise the switch
		TableauRulesSwitch rulesSwitch = new TableauRulesSwitch();
		
		for(ProgramState initial : statespace.getInitialStates()){
			Assertion initialAssertion = new Assertion(initial, formula);
			this.vertices.add(initialAssertion);
			vertexQueue.add(initialAssertion);
		}
		
		// Process vertices until no unprocessed ones remain
		while(!vertexQueue.isEmpty()){
			
			Assertion currentVertex = vertexQueue.poll();
			
			// Do a tableau step
			if(!currentVertex.getFormulae().isEmpty()){
				Node currentSubformula = currentVertex.getFirstFormula();
				
				if(isNextForm(currentSubformula)){
					// Apply next tableau rule to all remaining formula in the current vertice's formula set
					// Note that due to the insertion order we know that all contained formulae are next formulae
					
					// First collect the successor formula of each contained next formula
					HashSet<Node> nextSuccessors = new HashSet<Node>();
					for(Node nextFormula : currentVertex.getFormulae()){
						nextFormula.apply(rulesSwitch);
						
						assert(rulesSwitch.getOut(nextFormula) instanceof Node);
						Node successorNode = (Node) rulesSwitch.getOut(nextFormula);
						nextSuccessors.add(successorNode);
					}
					
					HashSet<SuccState> newEdges = new HashSet<SuccState>();
					// Generate an assertion for each successor state of the current state in the state space 
					// with formula set equal to the next successor formulae generated before
					ProgramState currentState = currentVertex.getProgramState();
					for(StateSuccessor s : statespace.getSuccessors().get(currentState)){
						ProgramState succState = s.getTarget();
						Assertion newAssertion = new Assertion(succState);
						
						for(Node succFormula : nextSuccessors){
							newAssertion.addFormula(succFormula);
						}
						
						// Check if we have already seen an equal assertion before
						boolean formulaePresent = true;
						HashSet<Assertion> presentAssertions = getVerticesForState(newAssertion.getProgramState());
						// Note that formulaePresent is finally true, iff we found an equal assertion 
						if(presentAssertions.isEmpty()){
							formulaePresent = false;
						} else {
							for(Assertion presentAssertion : presentAssertions){
								// Initialise for current iteration
								formulaePresent = true;
								if(newAssertion.getFormulae().size() == presentAssertion.getFormulae().size()){
									for(Node ASTnode : newAssertion.getFormulae()){
										if(!presentAssertion.getFormulae().contains(ASTnode)){
											formulaePresent = false;
											break;
										}
									}
								} else {
									formulaePresent = false;
								}
								if(formulaePresent){
									newAssertion = presentAssertion;
									break;
								}
							}
						}
						
						newEdges.add(new SuccState(newAssertion, currentSubformula));
						this.vertices.add(newAssertion);
						// Process the assertion further only in case it is not one, that was already processed
						if(!formulaePresent){
							vertexQueue.add(newAssertion);
						} else {
							// we detected a cycle, check if it is an unharmful one (containing a release
							// operator)
							boolean containsReleaseOp = false;
							for(Node current : newAssertion.getFormulae()){
								if(current instanceof AReleaseLtlform){
									containsReleaseOp = true;
								}
							}
							if(!containsReleaseOp){
								this.successful = false;
								// Optimisation: abort proof structure generation, as we already know that it is not successful!

							}
						}
					}
					this.addEdges(currentVertex, newEdges);

				} else{
					rulesSwitch.setIn(currentSubformula, currentVertex);
					currentSubformula.apply(rulesSwitch);
					
					// Retrieve the generated assertions
					HashSet<Assertion> successors = (HashSet<Assertion>) rulesSwitch.getOut(currentSubformula);
					// This means that the current vertex is not (yet) successful
					if(successors != null){
						HashSet<SuccState> successorStates = new HashSet<SuccState>();
						for(Assertion assertion : successors){
							successorStates.add(new SuccState(assertion, currentSubformula));
						}
						
						vertices.addAll(successors);
						vertexQueue.addAll(successors);
						
						this.addEdges(currentVertex, successorStates);

						
					}

					
				}
			} else {
				this.successful = false;
				// Do nothing or:
				// Optimisation: abort proof structure generation, as we already know that it is not successful!
			}
			
			
		}
	}
	
	/**
	 * This method collects all vertices, whose program state component is equal to
	 * the input program state.
	 * @param programState, the state that the vertices are checked for
	 * @return a set containing all assertions with program state component 'state', 
	 * if none exist an empty set is returned.
	 */
	private HashSet<Assertion> getVerticesForState(ProgramState programState) {
		HashSet<Assertion> assertionsForState = new HashSet<Assertion>();
		
		for(Assertion current : this.vertices){
			if(current.getProgramState() == programState){
				assertionsForState.add(current);
			}
		}
		return assertionsForState;
	}


	private void addEdges(Assertion currentVertex, HashSet<SuccState> successorStates) {
		if(!edges.containsKey(currentVertex)){
			edges.put(currentVertex, successorStates);
		} else {
			edges.get(currentVertex).addAll(successorStates);
		}
		
	}


	/**
	 * This method checks whether the outermost operator of the formula is a next-operator.
	 * 
	 * @param formula, the formula the outermost operator should be checked for
	 * @return true, in case the outermost operator is a next-operator
	 * 			false, in all other cases
	 */
	private boolean isNextForm(Node formula) {
		if(formula instanceof ANextLtlform){
			return true;
		} else if (formula instanceof Start){
			Start helper = (Start) formula;
			if(helper.getPLtlform() instanceof ANextLtlform){
				return true;
			}
			return false;
		}

		return false;
	}
	
	public boolean isSuccessful(){
		return this.successful;
	}
	
	public HashSet<Assertion> getLeaves(){
		HashSet<Assertion> leaves = new HashSet<Assertion>();
		for(Assertion vertex :vertices){
			if(!edges.containsKey(vertex)){
				leaves.add(vertex);
			}
		}
		return leaves;
	}


	public Integer size() {
		return vertices.size();
	}

	public HashSet<Assertion> getVertices() {

		return this.vertices;
	}
	
	public HashSet<Assertion> getSuccessors(Assertion current){
		HashSet<Assertion> successors = new HashSet<Assertion>();
		for(SuccState successor : this.edges.get(current)){
			successors.add(successor.assertion);
		}
		return successors;
	}

}
