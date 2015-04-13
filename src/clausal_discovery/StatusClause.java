package clausal_discovery;

import clausal_discovery.instance.Instance;
import clausal_discovery.instance.InstanceComparator;
import clausal_discovery.instance.InstanceList;
import clausal_discovery.instance.PositionedInstance;
import log.Log;
import vector.Vector;
import vector.WriteOnceVector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a selection of indices that represent instances in the instances body and head
 * The status clause helps with the efficient traversal of the clausal space
 * // TODO Improvement by arranging instance sets in a graph to determine possible moves
 * @author Samuel Kolb
 */
public class StatusClause {

	// IVAR rank - The rank is the amount of variables already introduced

	private final int rank;

	public int getRank() {
		return rank;
	}

	// IVAR instances - The instances in this clause

	private final Vector<PositionedInstance> instances;

	public Vector<PositionedInstance> getInstances() {
		return instances;
	}

	public int getLength() {
		return getInstances().size();
	}

	// IVAR environment - The typing environment

	private final Environment environment;

	public Environment getEnvironment() {
		return environment;
	}

	/**
	 * Creates a new status clause
	 */
	public StatusClause() {
		this.rank = 0;
		this.instances = new Vector<>();
		this.environment = new Environment();
	}

	private StatusClause(int rank, Vector<PositionedInstance> instances, Environment environment) {
		this.rank = rank;
		this.instances = instances;
		this.environment = environment;
	}


	/**
	 * Returns whether this status clause is currently in the body
	 * @return	True iff the status clause is currently in the body
	 */
	public boolean inBody() {
		return getInstances().isEmpty() || getInstances().getLast().isInBody();
	}

	/**
	 * Returns the index of the last instance
	 * @return The index of the last instance or -1 if none such instance exists
	 */
	public int getIndex() {
		return getInstances().isEmpty() ? -1 : getInstances().getLast().getIndex();
	}

	/**
	 * Returns whether this clause contains the given index-boolean pair
	 * @param instance	The instance representation
	 * @return	True iff this instance has been added to this status clause already
	 */
	public boolean contains(PositionedInstance instance) {
		return getInstances().contains(instance);
	}

	/**
	 * Returns whether the given instance can be added
	 * @param instance	A positioned instance
	 * @return	True iff the given instance is 1) consistent with typing, 2) connected 3) introduces variables in
	 * 			order and only it is in the body, and 4) an instance added to the head does not appear in the body
	 * 		TODO correct documentation
	 */
	public boolean canProcess(PositionedInstance instance) {
		if(inBody() == instance.isInBody() && instance.getIndex() <= getIndex())
			return false;
		if(!inBody() && instance.isInBody())
			return false;
		if(!getEnvironment().isValidInstance(instance.getInstance()))
			return false;
		if(!inBody() && contains(instance.clone(false)))
			return false;
		Vector<Integer> indices = instance.getInstance().getVariableIndices();
		return (getRank() == 0 || isConnected(indices)) && introducesVariablesInOrder(instance);
	}

	private boolean isConnected(Vector<Integer> indices) {
		int max = getRank() - 1;
		for(int i = 0; i < indices.size(); i++)
			if(indices.get(i) <= max)
				return true;
		return false;
	}

	private boolean introducesVariablesInOrder(PositionedInstance instance) {
		int max = getRank() - 1;
		Vector<Integer> indices = instance.getInstance().getVariableIndices();
		for(int i = 0; i < indices.size(); i++)
			if(instance.isInBody() && indices.get(i) == max + 1)
				max = indices.get(i);
			else if(indices.get(i) > max)
				return false;
		return true;
	}

	protected boolean isRepresentativeWith(StatusClause clause, PositionedInstance instance) {
		Log.LOG.printLine("INFO ");
		for(int i = 0; i < getInstances().size(); i++)
			if(!isRepresentativeWith(clause, i, instance))
				return false;
		return true;
	}

	private boolean isRepresentativeWith(StatusClause clause, int index, PositionedInstance instance) {
		List<PositionedInstance> instances = new ArrayList<>();
		for(int i = 0; i < getInstances().size() + 1; i++)
			if(i < index)
				instances.add(getInstances().get(i));
			else if(i == index)
				instances.add(instance);
			else
				instances.add(getInstances().get(i - 1));
		Map<Integer, Integer> mapping = createMapping(instances);
		updateInstances(mapping, instances);
		instances.sort(new InstanceComparator());
		Optional<StatusClause> builtClause = buildClause(instances);
		boolean representative = !builtClause.isPresent() || isRepresentative(clause, builtClause.get());
		Log.LOG.printLine("INFO " + (representative ? "Yes" : "No ") + " " + clause + " compared to " + builtClause + "? ");
		return representative;
	}

	private boolean isRepresentative(StatusClause clause, StatusClause newClause) {
		for(int i = 0; i < clause.getInstances().length; i++)
			if(new InstanceComparator().compare(newClause.getInstances().get(i), clause.getInstances().get(i)) < 0)
				return false;
		return true;
	}

	private Optional<StatusClause> buildClause(List<PositionedInstance> instances) {
		Optional<StatusClause> clause = Optional.of(new StatusClause());
		for(PositionedInstance instance : instances) {
			clause = clause.get().processIfValid(instance);
			if(!clause.isPresent())
				return clause;
		}
		return clause;
	}

	private Map<Integer,Integer> createMapping(List<PositionedInstance> instances) {
		int current = 0;
		Map<Integer, Integer> mapping = new HashMap<>();
		for(PositionedInstance instance : instances)
			for(Integer index : instance.getInstance().getVariableIndices())
				if(!mapping.containsKey(index))
					mapping.put(index, current++);
		return mapping;
	}

	private void updateInstances(Map<Integer, Integer> mapping, List<PositionedInstance> list) {
		for(int i = 0; i < list.size(); i++) {
			PositionedInstance positionedInstance = list.get(i);
			Instance instance = positionedInstance.getInstance();
			InstanceList instanceList = positionedInstance.getInstanceList();
			Instance newInstance = new Instance(instance.getPredicate(), getVariables(mapping, instance));
			list.set(i, instanceList.getInstance(instanceList.getIndex(newInstance), positionedInstance.isInBody()));
		}
	}

	private Vector<Integer> getVariables(Map<Integer, Integer> mapping, Instance instance) {
		Vector<Integer> variableList = new WriteOnceVector<>(new Integer[instance.getPredicate().getArity()]);
		variableList.addAll(instance.getVariableIndices().stream().map(mapping::get).collect(Collectors.toList()));
		return variableList;
	}

	public Optional<StatusClause> processIfRepresentative(PositionedInstance instance) {
		Optional<StatusClause> clause = processIfValid(instance);
		if(clause.isPresent() && isRepresentativeWith(clause.get(), instance))
			return clause;
		return Optional.empty();
	}

	/**
	 * Returns a new status clause where the given instance has been added
	 * @param instance	The instance to add
	 * @return	The new status clause // TODO documentation
	 */
	public Optional<StatusClause> processIfValid(PositionedInstance instance) {
		if(!canProcess(instance))
			return Optional.empty();
		int newRank = Math.max(getRank(), instance.getInstance().getMax() + 1);
		Environment newEnvironment = getEnvironment().addInstance(instance.getInstance());
		return Optional.of(new StatusClause(newRank, getInstances().grow(instance), newEnvironment));
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		StatusClause clause = (StatusClause) o;
		return instances.equals(clause.instances);

	}

	@Override
	public int hashCode() {
		return instances.hashCode();
	}

	@Override
	public String toString() {
		List<Instance> head = getInstances().stream()
				.filter(i -> !i.isInBody())
				.map(PositionedInstance::getInstance)
				.collect(Collectors.toList());
		List<Instance> body = getInstances().stream()
				.filter(PositionedInstance::isInBody)
				.map(PositionedInstance::getInstance)
				.collect(Collectors.toList());
		return  body + " => " + head;
	}
}
