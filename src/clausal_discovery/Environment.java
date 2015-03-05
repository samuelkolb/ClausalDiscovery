package clausal_discovery;

import clausal_discovery.instance.Instance;
import logic.bias.Type;
import logic.expression.formula.Predicate;
import vector.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by samuelkolb on 26/02/15.
 */
public class Environment {

	private final Map<Integer, Type> variableTypes;

	public Environment() {
		this.variableTypes = new HashMap<>();
	}

	private Environment(Map<Integer, Type> variableTypes) {
		this.variableTypes = variableTypes;
	}

	public boolean isValidInstance(Predicate predicate, Vector<Integer> indices) {
		Map<Integer, Type> variables = new HashMap<>(variableTypes);
		for(int i = 0; i < predicate.getArity(); i++) {
			Integer integer = indices.get(i);
			Type type = predicate.getTypes().get(i);
			if(!variables.containsKey(integer) || variables.get(integer).isSuperTypeOf(type))
				variables.put(integer, type);
			else if(!type.isSuperTypeOf(variables.get(integer)))
				return false;
		}
		return true;
	}

	public Environment addInstance(Instance instance) {
		return addInstance(instance.getPredicate(), instance.getVariableIndices());
	}

	public Environment addInstance(Predicate predicate, Vector<Integer> indices) {
		Map<Integer, Type> variables = new HashMap<>(variableTypes);
		for(int i = 0; i < predicate.getArity(); i++) {
			Integer integer = indices.get(i);
			Type type = predicate.getTypes().get(i);
			if (!variables.containsKey(integer) || variables.get(integer).isSuperTypeOf(type))
				variables.put(integer, type);
			else if (!type.isSuperTypeOf(variables.get(integer)))
				throw new IllegalStateException();
		}
		return new Environment(variables);
	}

	public Environment copy() {
		return new Environment(new HashMap<>(variableTypes));
	}
}
