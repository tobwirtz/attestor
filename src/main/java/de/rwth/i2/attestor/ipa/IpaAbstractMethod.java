package de.rwth.i2.attestor.ipa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.rwth.i2.attestor.graph.Nonterminal;
import de.rwth.i2.attestor.graph.heap.HeapConfiguration;
import de.rwth.i2.attestor.graph.heap.HeapConfigurationBuilder;
import de.rwth.i2.attestor.graph.heap.internal.IpaContractCollection;
import de.rwth.i2.attestor.main.settings.Settings;
import de.rwth.i2.attestor.semantics.jimpleSemantics.jimple.statements.invoke.AbstractMethod;
import de.rwth.i2.attestor.stateSpaceGeneration.ProgramState;
import de.rwth.i2.attestor.util.Pair;
import gnu.trove.list.array.TIntArrayList;

public class IpaAbstractMethod extends AbstractMethod {

	IpaContractCollection contracts = new IpaContractCollection();

	public IpaAbstractMethod(String displayName, StateSpaceFactory factory) {
		super(displayName, factory);
	}
	
	public void addContracts( IpaPrecondition precondition, List<HeapConfiguration> postconditions ){
		if( ! contracts.hasPrecondition(precondition) ){
			contracts.addPrecondition(precondition);
		}
		List<HeapConfiguration> currentPostconditions = contracts.getPostConditions(precondition);
		currentPostconditions.addAll( postconditions );
	}

	@Override
	public Set<ProgramState> getResult(HeapConfiguration input, int scopeDepth) {

		Set<ProgramState> result = new HashSet<>();
		for( HeapConfiguration postConfig : getResult( input ) ){
			result.add( Settings.getInstance().factory().createProgramState( postConfig, 0 ) );
		}

		return result;
	}

	public List<HeapConfiguration> getResult( HeapConfiguration currentConfig ){

		Pair<HeapConfiguration, Pair<HeapConfiguration,Integer>> splittedConfig = prepareInput( currentConfig );
		HeapConfiguration reachableFragment = splittedConfig.first();
		HeapConfiguration remainingFragment = splittedConfig.second().first(); 
		int placeholderPos = splittedConfig.second().second();

		Entry<IpaPrecondition, List<HeapConfiguration>> contract = contracts.getContract( reachableFragment );
		if( contract != null ){
			IpaPrecondition precondition = contract.getKey();
			List<HeapConfiguration> postconditions = contract.getValue();

			remainingFragment = adaptExternalOrdering( precondition, reachableFragment, 
					remainingFragment, placeholderPos);

			return applyContract(remainingFragment, placeholderPos, postconditions );
		}
		return null;
	}

	/**
	 * @param input
	 * @param methodName
	 * @return <reachableFragment,remainingFragment>
	 */
	protected Pair<HeapConfiguration, Pair<HeapConfiguration,Integer>> prepareInput( HeapConfiguration input ){
		ReachableFragmentComputer helper = new ReachableFragmentComputer( this.toString(), input );
		return helper.prepareInput();
	}

	private List<HeapConfiguration> applyContract( HeapConfiguration replacedFragment,
			int contractPlaceholderEdge,
			List<HeapConfiguration> contracts ){

		List<HeapConfiguration> result = new ArrayList<>();
		for( HeapConfiguration contract : contracts ){
			HeapConfigurationBuilder builder = replacedFragment.builder();
			builder.replaceNonterminalEdge(contractPlaceholderEdge, contract);
			result.add( builder.build() );
		}

		return result;
	}

	protected HeapConfiguration adaptExternalOrdering( IpaPrecondition precondition, 
			HeapConfiguration reachableFragment,
			HeapConfiguration remainingFragment,
			int placeholderPosition )
					throws IllegalArgumentException {

		int[] reordering = precondition.getReordering( reachableFragment );
		TIntArrayList oldTentacles = remainingFragment.attachedNodesOf( placeholderPosition );
		TIntArrayList newTentacles = new TIntArrayList();
		for( int i = 0; i < reordering.length; i++ ){
			newTentacles.add( oldTentacles.get( reordering[i]) );
		} 

		Nonterminal label = remainingFragment.labelOf(placeholderPosition);
		return remainingFragment.builder().removeNonterminalEdge(placeholderPosition)
				.addNonterminalEdge(label, newTentacles ).build();

	}




}