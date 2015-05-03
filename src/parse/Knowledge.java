package parse;

import basic.ArrayUtil;
import basic.StringUtil;
import clausal_discovery.core.LogicBase;
import clausal_discovery.core.PredicateDefinition;
import clausal_discovery.instance.Instance;
import idp.IdpProgramPrinter;
import logic.bias.Type;
import logic.example.Example;
import logic.expression.formula.Clause;
import logic.expression.formula.Formula;
import logic.expression.formula.Predicate;
import logic.expression.term.Variable;
import logic.theory.Vocabulary;
import util.Numbers;
import vector.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* The knowledge class implements logic base, containing all the information from a logic file
 *
 * @author Samuel Kolb
*/
public class Knowledge implements LogicBase {

	private final Vocabulary vocabulary;

	@Override
	public Vocabulary getVocabulary() {
		return vocabulary;
	}

	private final Vector<Example> examples;

	@Override
	public Vector<Example> getExamples() {
		return examples;
	}

	private final Vector<PredicateDefinition> searchPredicates;

	@Override
	public Vector<PredicateDefinition> getSearchPredicates() {
		return searchPredicates;
	}

	/**
	 * Creates a new knowledge instances with a vocabulary, examples and a list of search predicates
	 * @param vocabulary        The vocabulary
	 * @param examples         	The list of examples
	 * @param searchPredicates  The list of search predicates
	 */
	public Knowledge(Vocabulary vocabulary, Vector<Example> examples, Vector<PredicateDefinition> searchPredicates) {
		this.vocabulary = vocabulary;
		this.examples = examples;
		this.searchPredicates = searchPredicates;
	}

	@Override
	public String toString() {
		return new IdpProgramPrinter().printVocabulary(getVocabulary(), "Vocabulary")
				+ StringUtil.join("\n", examples.getArray());
	}

	@Override
	public List<Formula> getSymmetryFormulas() {
		List<Formula> formulas = new ArrayList<>();
		for(PredicateDefinition definition : getSearchPredicates().filter(PredicateDefinition::isSymmetric)) {
			Predicate predicate = definition.getPredicate();
			Vector<Integer> variableIndices = ArrayUtil.wrap(Numbers.range(predicate.getArity() - 1));
			Map<Integer, Variable> variableMap = new HashMap<>();
			for(Integer index : variableIndices) {
				Type type = predicate.getTypes().get(index);
				variableMap.put(index, new Variable(type.getName() + index, type));
			}
			Instance body = new Instance(definition, variableIndices);
			List<Numbers.Permutation> permutations = Numbers.getPermutations(predicate.getArity());
			for(int i = 1; i < permutations.size(); i++) {
				Instance head = new Instance(definition, new Vector<>(permutations.get(i).getIntegerArray()));
				formulas.add(Clause.horn(head.makeAtom(variableMap), body.makeAtom(variableMap)));
			}
		}
		return formulas;
	}

	@Override
	public List<LogicBase> split() {
		List<LogicBase> logicBases = new ArrayList<>();
		for(Example example : getExamples())
			logicBases.add(new Knowledge(getVocabulary(), new Vector<Example>(example), getSearchPredicates()));
		return logicBases;
	}
}